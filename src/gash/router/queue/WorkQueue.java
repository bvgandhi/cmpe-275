package gash.router.queue;

import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.handler.WorkHandler;
import gash.router.channel.WorkChannel;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

/**
 * 
 * @author BG
 *	There will be 1 WorkQueue instance on each server participating in leader election
 *
 *singleton impln with multithreading need to be chkd..as of now not impltd singleton
 */
public class WorkQueue {
	protected static Logger logger = LoggerFactory.getLogger("cmd");
	
	public static LinkedBlockingDeque<WorkChannel> incoming = new LinkedBlockingDeque<WorkChannel>();
	public static LinkedBlockingDeque<WorkChannel> outgoing = new LinkedBlockingDeque<WorkChannel>();
	
	WorkHandler workHandler=new WorkHandler();
	
		
	public void init(){
		workHandler.start();
	}
	
	public static void enqueueRequest(WorkMessage wmsg, Channel ch) {
		try {
			WorkChannel wmchnl= new WorkChannel(wmsg,ch);
			incoming.put(wmchnl);	
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}
	
}
