package io.github.diegog0477.zombiebox.client.presentation
import io.github.diegog0477.zombiebox.client.model.*

/** A receiver lease and exactly-once dispatch on this client; acknowledgements retry. */
class YouTubeReceiverViewModel(private val repository:YouTubeReceiverRepository,private val execute:(()->Unit)->Unit,private val deliver:(()->Unit)->Unit) {
 var receiver:YouTubeReceiver?=null;private set
 var observer:(()->Unit)?=null
 var command:((YouTubeCommand)->Unit)?=null
 var failed=false;private set
 private var busy=false
 @Volatile private var closed=false
 @Volatile private var generation=0
 private var dispatched=""
 private var pending:YouTubeCommand?=null
 private var buffered=false
 private var ready:ReceiverFeedback?=null
 private var latest=ReceiverFeedback()
 fun open(){
  if(closed||busy||receiver!=null)return
  busy=true;failed=false;val run=++generation
  execute{
   try{val result=repository.open()
    if(closed||run!=generation){repository.close(result.id);return@execute}
    deliver{if(!closed&&run==generation){busy=false;receiver=result;observer?.invoke()}else execute{try{repository.close(result.id)}catch(_:Exception){}}}
   }catch(_:Exception){deliver{if(run==generation){busy=false;failed=true;observer?.invoke()}}}
  }
 }
 fun tick(){
  val active=receiver?:return
  if(closed||busy)return
  busy=true;val run=generation;val sent=ready;val feedback=sent?:latest.copy(commandId="")
  execute{
   try{
    // A timed-out/preempted command may reject its old acknowledgement. Polling
    // must still recover the current command instead of retrying the stale ID forever.
    val accepted=try{repository.feedback(active.id,feedback);true}catch(_:Exception){false}
    val result=repository.poll(active.id)
    deliver{if(!closed&&run==generation){
     busy=false;failed=false;if(ready===sent&&(accepted||result.command?.id!=sent?.commandId))ready=null;receiver=result;observer?.invoke()
     val incoming=result.command
     if(incoming!=null&&incoming.id!=dispatched){dispatched=incoming.id;pending=incoming;buffered=false;command?.invoke(incoming)}
    }}
   }catch(_:Exception){deliver{if(run==generation){busy=false;failed=true;observer?.invoke()}}}
  }
 }
 fun playerState(state:String,position:Int,duration:Int){
  latest=latest.copy(state=state,positionMs=position.coerceAtLeast(0),durationMs=duration.coerceAtLeast(0))
  val current=pending?:return
  if(state=="BUFFERING")buffered=true
  val success=when(current.action){
   "play" -> buffered&&state=="PLAYING"
   "pause" -> state=="PAUSED"
   "resume" -> state=="PLAYING"
   "stop" -> state=="STOPPED"
   "seek" -> state in listOf("PLAYING","PAUSED") && kotlin.math.abs(position-current.positionMs)<1500
   else -> false
  }
  if(success||state=="FAILED"){ready=latest.copy(commandId=current.id,success=success);pending=null}
 }
 fun volumeApplied(id:String,volume:Int,muted:Boolean,success:Boolean){if(pending?.id!=id)return;if(success)latest=latest.copy(volume=volume,muted=muted);complete(success,id)}
 fun complete(success:Boolean,id:String?=null){val current=pending?:return;if(id!=null&&id!=current.id)return;ready=latest.copy(commandId=current.id,success=success);pending=null}
 fun disable(){
  val id=receiver?.id;generation++;receiver=null;busy=false;pending=null;ready=null;dispatched="";observer?.invoke()
  if(id!=null)execute{try{repository.close(id)}catch(_:Exception){}}
 }
 fun close(){closed=true;disable();observer=null;command=null}
}
