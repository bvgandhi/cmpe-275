/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server.edges;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.CommandHandler;
import gash.router.server.CommandInit;
import gash.router.server.ServerState;
import gash.router.server.WorkHandler;
import gash.router.server.WorkInit;
import gash.router.server.election.ElectionManager;
import gash.router.utils.BroadcastUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Header;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import routing.Pipe.CommandMessage;

public class EdgeMonitor implements EdgeListener, Runnable {

	protected static Logger logger = LoggerFactory.getLogger("edge monitor");

	private EdgeList outboundEdges;
	private EdgeList inboundEdges;
	private EdgeList outboundCmdEdges;
	private long dt = 2000;
	private ServerState state;
	private boolean forever = true;
	ChannelFuture channelFuture;
	

	public EdgeMonitor(ServerState state) {
		if (state == null)
			throw new RuntimeException("state is null");

		this.outboundEdges = new EdgeList();
		this.outboundCmdEdges=new EdgeList();
		this.inboundEdges = new EdgeList();
		this.state = state;
		this.state.setEmon(this);

		if (state.getConf().getRouting() != null) {
			for (RoutingEntry e : state.getConf().getRouting()) {
				outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
				outboundCmdEdges.addNode(e.getId(), e.getHost(), e.getPort()+1);
			}
			System.out.println("####outboundCmdEdges Size"+ outboundCmdEdges.getEdgeMap().size());
		}

		// cannot go below 2 sec
		if (state.getConf().getHeartbeatDt() > this.dt)
			this.dt = state.getConf().getHeartbeatDt();
	}
	
	public EdgeList getOutBoundList(){
		return outboundEdges;
	}
	
	public EdgeList getInBountList(){
		return inboundEdges;
	}

	public void createInboundIfNew(int ref, String host, int port) {
		inboundEdges.createIfNew(ref, host, port);
	}

	private WorkMessage createHB(EdgeInfo ei) {
		WorkState.Builder sb = WorkState.newBuilder();
		sb.setEnqueued(-1);
		sb.setProcessed(-1);

		Heartbeat.Builder bb = Heartbeat.newBuilder();
		bb.setState(sb);

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setBeat(bb);
		wb.setSecret(1);


		return wb.build();
	}

	public void shutdown() {
		forever = false;
	}
	
	// creates channel between servers to send HB 
	public synchronized void createWorkChannel(EdgeInfo ei) {
		try {
			EventLoopGroup group = new NioEventLoopGroup();
			Bootstrap b = new Bootstrap();
			b.handler(new WorkHandler(state));

			b.group(group).channel(NioSocketChannel.class).handler(new WorkInit(state, false));
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.\
			System.out.println("Connecting to Work Host "+ei.getHost()+" Work Port "+ei.getPort());
			ChannelFuture cf = b.connect(ei.getHost(), ei.getPort()).syncUninterruptibly();
			BroadcastUtil.addWorkChannel(ei.getRef(), cf.channel()); 
			ei.setChannel(cf.channel());
			ei.setActive(true);
			cf.channel().closeFuture();
		}
		catch (Throwable ex) {
			System.out.println("Unable to create channel!");
			ei.setActive(false);
			ei.setChannel(null);
			//ex.printStackTrace();
		}

		
	}
	public synchronized void createCmdChannel(EdgeInfo ei) {
		try {
			EventLoopGroup group = new NioEventLoopGroup();
			Bootstrap b = new Bootstrap();
			b.handler(new CommandHandler(state.getConf()));

			b.group(group).channel(NioSocketChannel.class).handler(new CommandInit(state.getConf(), false));
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			System.out.println("Connecting to Command Host "+ei.getHost()+" Command Port "+ei.getPort());
			ChannelFuture cf = b.connect(ei.getHost(), ei.getPort()).syncUninterruptibly();
			BroadcastUtil.addCmdChannel(ei.getRef(), cf.channel());
			ei.setChannel(cf.channel());
			ei.setActive(true);
			cf.channel().closeFuture();
		}
		catch (Throwable ex) {
			System.out.println("Unable to createCmdChannel channel! for port"+ ei.getPort());
			ei.setActive(false);
			ei.setChannel(null);
			//ex.printStackTrace();
		}

		
	}

	@Override
	public void run() {
		while (forever) {
			try {
				for (EdgeInfo ei : this.outboundEdges.map.values()) {
								
					if(ei.getChannel()==null){
						
						this.createWorkChannel(ei);
						//System.out.println("Channel not found for node id: " + ei.getRef() + ". Creating channel..." + ei.getChannel());
					}else if (ei.isActive() && ei.getChannel() != null) {
						//Heartbeat is created and appended in WorkMesaage to pass to servers
						WorkMessage wm = createHB(ei);
						ei.getChannel().writeAndFlush(wm);
						
					} else {
						// TODO create a client to the node
						logger.info("trying to connect to node: " + ei.getRef());
						logger.info("Connected to node: " + ei.getRef() +" : " +ei.isActive());
					}
				}
				
				for (EdgeInfo ei : this.outboundCmdEdges.map.values()) {
					
					if(ei.getChannel()==null){
						//###TODO
						this.createCmdChannel(ei);
					}else if (ei.isActive() && ei.getChannel() != null) {
						
						//TODO for cmd create ping msg and send directly for connection maintinag only pure dat msgs will be in outboundQ
						System.out.println("### Sending Test Cmd Ping to " + ei.getRef());
						ei.getChannel().writeAndFlush(cmdPing());
						
						
					} else {
						// TODO create a client to the node
						logger.info("trying to connect to node: " + ei.getRef());
						logger.info("Connected to node: " + ei.getRef() +" : " +ei.isActive());
					}
				}
				Thread.sleep(dt/6);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.info("Unable to connect to a server");
			}
		}
	}
	
	//
	private CommandMessage cmdPing(){
				Header.Builder hb = Header.newBuilder();
				hb.setNodeId(ElectionManager.getNodeId());
				hb.setTime(System.currentTimeMillis());
				hb.setDestination(-1);

				CommandMessage.Builder rb = CommandMessage.newBuilder();
				rb.setHeader(hb);
				rb.setPing(true);
				//rb.setMessage("CmdMsg");
				
				return rb.build();
	}
	
	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// TODO check connection
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}
}
