package io.github.diegog0477.zombiebox.client.presentation

import io.github.diegog0477.zombiebox.client.model.*

/** One foreground poll at a time; network errors preserve the last confirmed session. */
class ReceiverViewModel(private val repository:ReceiverRepository,private val execute:(()->Unit)->Unit,private val deliver:(()->Unit)->Unit) {
    var observer:((ReceiverPlan?)->Unit)?=null
    private var generation=0
    private var loading=false
    private var closed=false
    private var dismissed=""
    var activeSession="";private set
    private var interrupted:PlaybackContext?=null
    fun transition(plan:ReceiverPlan?,current:PlaybackContext):ReceiverChange? {
        if(plan==null) {
            if(activeSession.isEmpty())return null
            val previous=interrupted;activeSession="";interrupted=null
            return ReceiverChange.Restore(previous)
        }
        if(plan.sessionId==activeSession || plan.sessionId==dismissed)return null
        if(activeSession.isEmpty())interrupted=current
        activeSession=plan.sessionId
        return ReceiverChange.Begin(plan)
    }
    fun dismiss(sessionId:String){dismissed=sessionId;activeSession="";interrupted=null;generation++;loading=false}
    fun refresh() {
        if(closed || loading)return
        loading=true;val request=++generation;val ignored=dismissed
        execute {
            try { var plan=repository.active();if(plan?.sessionId==ignored){repository.stop(ignored);plan=null};val result=plan;deliver { if(!closed && request==generation){loading=false;observer?.invoke(result)} } }
            catch(_:Exception){deliver { if(request==generation)loading=false } }
        }
    }
    fun reset(){generation++;loading=false;activeSession="";interrupted=null;dismissed=""}
    fun close(){closed=true;reset();observer=null}
}
