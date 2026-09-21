package io.github.diegog0477.zombiebox.client
import org.junit.Assert.*
import org.junit.Test
import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.client.presentation.YouTubeReceiverViewModel
class YouTubeReceiverViewModelTest {
 private class Repository:YouTubeReceiverRepository {
  var value=YouTubeReceiver("receiver","READY","123456")
  val feedback=ArrayList<ReceiverFeedback>();var closed=false;var rejectFeedback=false
  override fun open()=value
  override fun poll(id:String)=value
  override fun feedback(id:String,value:ReceiverFeedback){if(rejectFeedback)throw IllegalStateException("stale");feedback.add(value)}
  override fun close(id:String){closed=true}
 }
 @Test fun playAcknowledgesOnlyAfterNewPlayerBuffersAndPlays(){
  val repo=Repository();val model=YouTubeReceiverViewModel(repo,{it()},{it()});var calls=0;model.command={calls++}
  model.open();repo.value=repo.value.copy(command=YouTubeCommand("one","play","youtube-fixture"));model.tick();model.tick();assertEquals(1,calls)
  model.playerState("PLAYING",1,1000);model.tick();assertTrue(repo.feedback.last().commandId.isEmpty())
  model.playerState("BUFFERING",0,0);model.playerState("PLAYING",1000,10000);model.tick();assertEquals("one",repo.feedback.last().commandId);assertTrue(repo.feedback.last().success)
  model.disable();assertTrue(repo.closed)
 }
 @Test fun previousVolumeCompletionCannotAcknowledgeNewPlay(){
  val repo=Repository();val model=YouTubeReceiverViewModel(repo,{it()},{it()});model.command={};model.open()
  repo.value=repo.value.copy(command=YouTubeCommand("old","volume"));model.tick()
  repo.value=repo.value.copy(command=YouTubeCommand("new","play"));model.tick()
  model.volumeApplied("old",50,false,true);model.tick();assertTrue(repo.feedback.last().commandId.isEmpty())
 }
 @Test fun closingDuringOpenCleansUpLateLease(){
  val repo=Repository();val work=ArrayList<()->Unit>();val model=YouTubeReceiverViewModel(repo,{work.add(it)},{it()})
  model.open();model.close();work.removeAt(0)();assertTrue(repo.closed);assertNull(model.receiver)
 }
 @Test fun staleAcknowledgementDoesNotPreventPollingANewerCommand(){
  val repo=Repository();val model=YouTubeReceiverViewModel(repo,{it()},{it()});val calls=ArrayList<String>()
  model.command={calls.add(it.id)};model.open()
  repo.value=repo.value.copy(command=YouTubeCommand("old","stop"));model.tick();model.complete(true)
  repo.rejectFeedback=true;repo.value=repo.value.copy(command=YouTubeCommand("new","stop"));model.tick()
  assertEquals(listOf("old","new"),calls)
  repo.rejectFeedback=false;model.tick();assertTrue(repo.feedback.last().commandId.isEmpty())
 }
}
