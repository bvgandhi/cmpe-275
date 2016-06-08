package gash.router.channel;

import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;
/**
 
 * @author bg
 * 
 */
public class CommandChannel implements Comparable<Integer> {
	private  CommandMessage cmsg;
	private Channel channel;
	private Integer sequenceid; 
	public CommandMessage getCmsg() {
		return cmsg;
	}

	public void setCmsg(CommandMessage cmsg) {
		this.cmsg = cmsg;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	
	
	public CommandChannel(CommandMessage cmsg,Channel ch, Integer seqid){
		this.cmsg= cmsg;
		channel=ch;
		this.sequenceid=seqid;
	}

	@Override
	public int compareTo(Integer anotherInteger) {
		// TODO Auto-generated method stub
		//i want in order of lowest to first 
		return -1*this.sequenceid.compareTo(anotherInteger);
	}
	
	
	
}
