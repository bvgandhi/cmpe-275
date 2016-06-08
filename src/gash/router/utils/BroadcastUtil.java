package gash.router.utils;
import java.util.HashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;
	
/**
 
 * @author bg
 * 
 */	
public class BroadcastUtil {
	
	
	/** node ID to channel */
	private static HashMap<Integer, Channel> cmdchannnels = new HashMap<Integer, Channel>();
	private static HashMap<Integer, Channel> workChannels = new HashMap<Integer, Channel>();
	
	public static HashMap<Integer, Channel> getCmdchannnels() {
		return cmdchannnels;
	}
	public static void setCmdchannnels(HashMap<Integer, Channel> cmdchannnels) {
		BroadcastUtil.cmdchannnels = cmdchannnels;
	}
	public static HashMap<Integer, Channel> getWorkChannels() {
		return workChannels;
	}
	public static void setWorkChannels(HashMap<Integer, Channel> workChannels) {
		BroadcastUtil.workChannels = workChannels;
	}

	public synchronized static void addWorkChannel(Integer nodeId, Channel channel) {
		workChannels.put(nodeId, channel);
	}
	public synchronized static void removeWorkChannel(Integer nodeId) {
		workChannels.remove(nodeId) ;
	}
	
	public synchronized static void removeCmdChannel(Integer nodeId) {
		cmdchannnels.remove(nodeId) ;
	}
	public synchronized static void addCmdChannel(Integer nodeId, Channel channel) {
		cmdchannnels.put(nodeId, channel);
	}
	
	public static Channel getWorkChannel_ForNode(Integer nodeId) {
			return workChannels.get(nodeId);
	}
	
	public static Channel getCmdChannel_ForNode(Integer nodeId) {
		return cmdchannnels.get(nodeId);
	}
	
	public synchronized static void cmdBroadcastWriteFlush(CommandMessage cmsg) {
		if (cmsg == null)
			return;

		for (Channel ch : cmdchannnels.values()){
			System.out.println("sending cmd msg ffrom leader to followers broadcast");
			ch.writeAndFlush(cmsg);
		}
	}
	
	public synchronized static void wrkBroadcastWriteFlush(WorkMessage wmsg) {
		if (wmsg == null)
			return;

		for (Channel ch : workChannels.values())
			ch.writeAndFlush(wmsg);
		
	}
	
	public static ChannelFuture sendToNodeWork(Integer nodeId, WorkMessage wmsg) {
		return getWorkChannel_ForNode(nodeId).writeAndFlush(wmsg);
	}
	
	public static ChannelFuture sendToNodeCmd(Integer nodeId,CommandMessage cmsg) {
		return getCmdChannel_ForNode(nodeId).writeAndFlush(cmsg);
	}
	
	//to be used during election times to chk the nuo. of nodes reqd for majority
	public static int getNumWorkConnections() {
		return workChannels.size();
	}
	
	public static int getNumCmdConnections() {
		return cmdchannnels.size();
	}
	
	
	
}
