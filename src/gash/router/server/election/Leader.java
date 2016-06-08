package gash.router.server.election;

import java.util.Iterator;

import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import pipe.common.Common.Header;
import pipe.work.Work.AppendMessage;
import pipe.work.Work.VoteMessage;
import pipe.work.Work.WorkMessage;
/**
 
 * @author bg
 * 
 */
public class Leader implements ElectionState {

	private ElectionManager Manager;

	@Override
	public synchronized void process() {
		try {
			EdgeList edges = Manager.getEmon().getOutBoundList();
			EdgeInfo e=null;
			try {
				Iterator<EdgeInfo> edgeInfoList= edges.getEdgeMap().values().iterator();
				while (edgeInfoList.hasNext()) {
					e=edgeInfoList.next();
					if (e.isActive()) {
						AppendMessage.Builder ab = AppendMessage.newBuilder();
						ab.setTerm(Manager.getTerm());
						ab.setLeaderId(Manager.getNodeId());

						Header.Builder hb = Header.newBuilder();
						hb.setNodeId(Manager.getNodeId());
						hb.setDestination(-1);
						hb.setTime(System.currentTimeMillis());

						WorkMessage.Builder wb = WorkMessage.newBuilder();
						wb.setAppendMessage(ab);
						wb.setHeader(hb);
						wb.setSecret(1);

						e.getChannel().writeAndFlush(wb.build());
					}
				}
			} catch (Exception ex) {
				e.setChannel(null);
				e.setActive(false);
			}

			Thread.sleep(500);

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

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
		// TODO Auto-generated method stub

		if (msg.getRequestVote().getTerm() > Manager.getTerm()) {
			Manager.setTerm(Manager.getTerm() + 1);
			Manager.setCurrentState(Manager.Follower);
			Manager.getCurrentState().voteRequested(msg);
		}

		if (msg.getRequestVote().getTerm() < Manager.getTerm())
			replyVote(msg, false);

	}

	@Override
	public synchronized void replyVote(WorkMessage msg, boolean voteGranted) {

		int toNodeId = msg.getHeader().getNodeId();
		int fromNodeId = Manager.getNodeId();
		EdgeInfo ei = Manager.getEmon().getOutBoundList().getEdgeMap().get(toNodeId);

		if (ei.isActive()) {
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
		for (EdgeInfo ei : Manager.getEmon().getOutBoundList().getEdgeMap().values()) {
			if (ei.isActive() && ei.getChannel() != null) {
				AppendMessage.Builder ab = AppendMessage.newBuilder();
				ab.setTerm(Manager.getTerm());
				ab.setLeaderId(Manager.getNodeId());

				Header.Builder hb = Header.newBuilder();
				hb.setDestination(-1);
				hb.setNodeId(Manager.getNodeId());
				hb.setTime(System.currentTimeMillis());

				WorkMessage.Builder wb = WorkMessage.newBuilder();
				wb.setAppendMessage(ab);
				wb.setHeader(hb);
				wb.setSecret(1);
				ei.getChannel().writeAndFlush(wb.build());
			}
		}
	}

	@Override
	public synchronized void getAppendMessage(WorkMessage msg) {
		// TODO Auto-generated method stub
		if (msg.getAppendMessage().getTerm() > Manager.getTerm()) {
			Manager.setCurrentState(Manager.Follower);
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
