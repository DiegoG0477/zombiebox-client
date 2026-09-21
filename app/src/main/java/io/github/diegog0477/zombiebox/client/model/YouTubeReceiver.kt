package io.github.diegog0477.zombiebox.client.model

data class YouTubeCommand(val id:String,val action:String,val itemId:String="",val positionMs:Int=0,val volume:Int=100,val muted:Boolean=false)
data class YouTubeReceiver(val id:String,val state:String,val code:String,val command:YouTubeCommand?=null)
data class ReceiverFeedback(val commandId:String="",val success:Boolean=true,val state:String="STOPPED",val positionMs:Int=0,val durationMs:Int=0,val volume:Int=100,val muted:Boolean=false)
interface YouTubeReceiverRepository {
 fun open():YouTubeReceiver
 fun poll(id:String):YouTubeReceiver
 fun feedback(id:String,value:ReceiverFeedback)
 fun close(id:String)
}
