package gash.router.server.election;

import java.text.SimpleDateFormat;
import java.util.Date;

import gash.router.server.edges.EdgeInfo;
import pipe.common.Common.Header;
import pipe.work.Work.VoteMessage;
import pipe.work.Work.WorkMessage;
/**
 
 * @author bg
 * 
 */
public class Follower implements ElectionState {

	private ElectionManager Manager;
	private int votedFor = -1;
	
	@Override
	public synchronized void process() {
		if(Manager.getElectionTimeout()<=0){
			Manager.setCurrentState(Manager.Candidate);			
			return;
		}
		long dt = Manager.getElectionTimeout()- (System.currentTimeMillis() - Manager.getTimerStart() );
		Manager.setElectionTimeout(dt);
	}

	@Override
	public synchronized void setManager(ElectionManager Mgr) {
		this.Manager = Mgr;
	}

	@Override
	public synchronized ElectionManager getManager() {
		return Manager;
	}

	@Override
	public synchronized void receivedVote(WorkMessage msg) {
		// Do Nothing
		return;
		
	}

	@Override
	public synchronized void voteRequested(WorkMessage msg) {
		if(msg.getRequestVote().getTerm() > Manager.getTerm()){
			votedFor = -1;
			Manager.setTerm(Manager.getTerm()+1);			
			if(votedFor == -1){				
				votedFor = msg.getHeader().getNodeId();
				Manager.randomizeElectionTimeout();
				System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS").format(new Date())+ " : You" +Manager.getNodeId() +"voted  for "+votedFor+" in term "+Manager.getTerm()+". Timeout is : "+Manager.getElectionTimeout());
				
				replyVote(msg, true);				
			}else{
				replyVote(msg, false);
			}
		}	
		
		
	}

	@Override
	public synchronized void replyVote(WorkMessage msg, boolean voteGranted) {
		int toNodeId = msg.getHeader().getNodeId();
		int fromNodeId = Manager.getRoutingConf().getNodeId();
		EdgeInfo ei = Manager.getEmon().getOutBoundList().getEdgeMap().get(toNodeId);
		
		if(ei.isActive()){
			VoteMessage.Builder vb = VoteMessage.newBuilder();
			vb.setTerm(Manager.getTerm());
			vb.setVoteGranted(voteGranted);
			
			Header.Builder hb = Header.newBuilder();
			hb.setNodeId(fromNodeId);
			hb.setDestination(-1);
			hb.setTime(System.currentTimeMillis());
			
			WorkMessage.Builder wb = WorkMessage.newBuilder();
			wb.setVoteMessage(vb);
			wb.setHeader(hb);
			wb.setSecret(1);
			
			ei.getChannel().writeAndFlush(wb.build());
		}
		
	}

	@Override
	public synchronized void sendAppendMessage() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public synchronized void getAppendMessage(WorkMessage msg) {
		// TODO Auto-generated method stub
		if(msg.getAppendMessage().getTerm()>=Manager.getTerm()){
			votedFor = -1;

			Manager.randomizeElectionTimeout();
			Manager.randomizeElectionTimeout();
			if(msg.getAppendMessage().getLeaderId()!=Manager.getLeaderId())
				System.out.println("Leader heartbeat received. Leader is : "+msg.getAppendMessage().getLeaderId()+" Timeout left is : "+Manager.getElectionTimeout());
			
			Manager.setTerm((int) msg.getAppendMessage().getTerm());
			
			Manager.setLeaderId((int) msg.getAppendMessage().getLeaderId());
			Manager.setLastKnownBeat(System.currentTimeMillis());
			
		}
	}

	@Override
	public synchronized void sendAppendReply(WorkMessage msg, boolean successStatus) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public synchronized void getAppendReply(WorkMessage msg) {
		// TODO Auto-generated method stub
		
	}
	
}
