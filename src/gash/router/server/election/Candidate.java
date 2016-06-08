package gash.router.server.election;

import java.text.SimpleDateFormat;
import java.util.Date;

import gash.router.server.edges.EdgeInfo;
import pipe.common.Common.Header;
import pipe.work.Work.RequestVote;
import pipe.work.Work.VoteMessage;
import pipe.work.Work.WorkMessage;
/**
 
 * @author bg
 * 
 */
public class Candidate implements ElectionState {
	private ElectionManager Manager;
	private int voteCount = 0;
	private int votedFor = -1;
	private int totalActive = 1;
	
	@Override
	public synchronized void process() {
		
		long dt = Manager.getElectionTimeout()- (System.currentTimeMillis() - Manager.getTimerStart() );
		Manager.setElectionTimeout(dt);
		if(Manager.getElectionTimeout()<=0){
			System.out.println("Node : "+Manager.getNodeId()+" timed out");
			requestVote();
			Manager.randomizeElectionTimeout();
			return;
		}		
	}
	
	public synchronized void requestVote(){	
		
		System.out.println("Timed out + "+ Manager.getElectionTimeout());
		Manager.setTerm(Manager.getTerm()+1);
		totalActive = 1;
		voteCount = 0;
		votedFor = Manager.getNodeId(); //Vote for self
		voteCount++;
		 //get new timeout for re-election in case of split vote
		Manager.randomizeElectionTimeout();
		
		//First get total active nodes. Used for counting majority vote (majority vote -> received votes > total / 2)
		
		
		for(EdgeInfo ei : Manager.getEmon().getOutBoundList().getEdgeMap().values()){
			if(ei.isActive() && ei.getChannel()!=null){
				
				RequestVote.Builder rb = RequestVote.newBuilder();
				rb.setTerm(Manager.getTerm());
				rb.setCandidateId((long)Manager.getNodeId());
				
				Header.Builder hb = Header.newBuilder();
				hb.setNodeId(Manager.getRoutingConf().getNodeId());
				hb.setDestination(-1);
				hb.setTime(System.currentTimeMillis());
				
				WorkMessage.Builder wb = WorkMessage.newBuilder();
				wb.setRequestVote(rb);
				wb.setHeader(hb);
				wb.setSecret(1);
				
				ei.getChannel().writeAndFlush(wb.build());
			}
		}
		
		return;
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
		System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"Vote Received from : "+msg.getHeader().getNodeId() + "is : "+msg.getVoteMessage().getVoteGranted());
		if(msg.getVoteMessage().getVoteGranted()){			
			voteCount++;
			totalActive = 1;
			for(EdgeInfo ei : Manager.getEmon().getOutBoundList().getEdgeMap().values()){
				if(ei.isActive() && ei.getChannel()!=null)
				{
					totalActive++;
				}
			}
			
				
			if(voteCount > (totalActive/2) ){
				Manager.randomizeElectionTimeout();
				System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) +"Leader Elected. Node Id : " + Manager.getNodeId()+". Out of total nodes: "+totalActive);				
				Manager.setCurrentState(Manager.Leader);				
				votedFor = -1;
				voteCount = 0;
			}
		}
		return;
	}

	@Override
	public synchronized void voteRequested(WorkMessage msg) {
		
			replyVote(msg, false);
	}

	@Override
	public synchronized void replyVote(WorkMessage msg, boolean voteGranted) {
		int toNodeId = msg.getHeader().getNodeId();
		int fromNodeId = Manager.getNodeId();
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
			this.votedFor = -1;
			Manager.setTerm((int) msg.getAppendMessage().getTerm());
			Manager.setCurrentState(Manager.Follower);
			Manager.randomizeElectionTimeout();
			System.out.println("Leader Elected. Leader is : "+msg.getAppendMessage().getLeaderId());
			Manager.getCurrentState().getAppendMessage(msg);
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
