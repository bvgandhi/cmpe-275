package gash.router.channel;

import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;
	
/**
 
 * @author bg
 * 
 */	
public class WorkChannel {
		
	private WorkMessage workMessage;
	private Channel channel;
	
	public WorkMessage getWorkMessage() {
		return workMessage;
	}

	public void setWorkMessage(WorkMessage workMessage) {
		this.workMessage = workMessage;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}


	
	public WorkChannel(WorkMessage wmsg,Channel ch){
		workMessage= wmsg;
		channel=ch;
	}
}


