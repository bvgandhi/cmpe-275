package gash.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.channel.WorkChannel;
import gash.router.queue.WorkQueue;
import pipe.work.Work.WorkMessage;

/**
 
 * @author bg
 * 
 */
public class WorkHandler extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("cmd");
	
	// ip address of the node where the thread is running
	protected String ip;
	
	
	@Override
	public void run(){
		while (true) {
			try {
				// block until a message is enqueued
				WorkChannel msg =WorkQueue.incoming.take();
				
				// msg can be from client asking for leader
				// msg can be Hb
				// msg can be asking for vote
				
					
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.info("Incoming q was interrupted at server "+  ip);
				e.printStackTrace();
			}
			finally{
				
			}
		}
	}
	
}
