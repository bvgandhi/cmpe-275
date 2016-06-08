package gash.router.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.handler.CommandWriter;
import gash.handler.WorkHandler;
import gash.router.channel.CommandChannel;
import gash.router.container.RoutingConf;
import gash.router.server.ServerState;
import gash.router.server.election.ElectionManager;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

/**
 
 * @author bg
 * 
 */
public class CommandQueue {
protected static Logger logger = LoggerFactory.getLogger("CommandQueue");
	
	public static PriorityBlockingQueue<CommandChannel> incmgCmdQ = new PriorityBlockingQueue<CommandChannel>();

	public static PriorityBlockingQueue<CommandChannel> outgCmdQ = new PriorityBlockingQueue<CommandChannel>();
	
	private RoutingConf conf;
	private final int inmcgCnsrs=5;
	 ExecutorService service= null;
	
	public CommandQueue(RoutingConf conf){
		this.conf=conf;
	}
	public void init(){
		 service = Executors.newFixedThreadPool(inmcgCnsrs);
		 System.out.println("Init of exects called");
	    for (int j = 1; j < inmcgCnsrs+1; j++) {
	    	System.out.println("submiitng to exec");
	        service.submit(new CommandWriter(j, incmgCmdQ, conf));
	    }
	}
	
	public static void enqueueRequest(CommandMessage cmsg, Channel ch) {
		CommandChannel cmchnl;
		System.out.println("Cmd Msg Enqueued");
		if  (cmsg.hasMessage())
			 cmchnl= new CommandChannel(cmsg,ch,ElectionManager.putNincrement(cmsg));
		else
			 cmchnl= new CommandChannel(cmsg,ch,0);
		
		incmgCmdQ.put(cmchnl);
	}
}
