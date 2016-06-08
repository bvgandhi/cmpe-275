package gash.handler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import gash.database.DBHandler;
import gash.router.channel.CommandChannel;
import gash.router.container.RoutingConf;
import gash.router.server.election.Candidate;
import gash.router.server.election.Leader;
import gash.router.server.election.ElectionManager;
import gash.router.utils.BroadcastUtil;
import io.netty.channel.Channel;
import pipe.common.Common;
import routing.Pipe.CommandMessage;
import storage.Storage;
import storage.Storage.Action;
import storage.Storage.Metadata;
import storage.Storage.Query;	
/**
 
 * @author bg
 * 
 */
public class CommandWriter implements Runnable{
	private final int id;
	 //msgs to be written to DB
	 private  PriorityBlockingQueue<CommandChannel> incmgCmdQ;
	 private boolean forever=true;
	 private RoutingConf conf;
	 private  DBHandler redisCLient;
	 private final Logger logger = LoggerFactory.getLogger(CommandWriter.class);
	 
	 public CommandWriter(int id, PriorityBlockingQueue<CommandChannel> incmgCmdQ,RoutingConf conf) {
	        this.id = id;
	       this.incmgCmdQ=incmgCmdQ;
	       this.conf=conf;
	       this.redisCLient =new DBHandler(conf.getDb1IP(),conf.getDb1port());
	    }

	    public void run() {

	    //take a conn from DB pool database connection
	    	System.out.println("### run called of executor thread");
	    	
	    	while(true && ! (ElectionManager.getCurrentState() instanceof Candidate) ) {
	    		if (!forever && incmgCmdQ.size() == 0)
					break;
	        	try {
	        		
	        		//only msgs having query reach here
	        		
	        		System.out.println("### Taking from Q ");
	        		CommandChannel cmsg=  incmgCmdQ.take();
					System.out.println("THREAD:"+ this.id + " value taken " +cmsg.getCmsg().getMessage() + cmsg.getCmsg().getHeader().getNodeId());
					
					/* todo
				    1 write to db
	    			2 if leader send to all followers using the channel
	    			
	    			  if follower set the seqid to Raft State 
	    			3 NOT TO DO increment the seqid log static counter in raft state done at enqueing
					 */
					Channel ch= cmsg.getChannel();
					Query query = cmsg.getCmsg().getQuery();
					String key= query.getKey();
					Action actiontobedone= query.getAction();
					Storage.Response.Builder rb = Storage.Response.newBuilder();
					//only GET and PUT supported
					Common.Header.Builder hb = Common.Header.newBuilder();
					hb.setNodeId(ElectionManager.getNodeId());
					hb.setTime(System.currentTimeMillis());
					CommandMessage.Builder cb = CommandMessage.newBuilder();
					
					switch (actiontobedone){
					case GET: 
						rb.setAction(query.getAction());
						Map<Integer, byte[]> dataMap = redisCLient.get(key);
						
						if (dataMap.isEmpty()) {
							logger.info("Key ", key, " not present in the database");
							Common.Failure.Builder fb = Common.Failure.newBuilder();
							fb.setMessage("Key not present");
							fb.setId(-999);

							rb.setSuccess(false);
							rb.setFailure(fb);
							System.out.println("Writing to Client on Failure");
							hb.setTime(System.currentTimeMillis());
							//rb.set
							cb.setHeader(hb);
							cb.setErr(fb);
							ch.writeAndFlush(cb);
							
							// enq to outgng cmdq
						} else {
							hb.setTime(System.currentTimeMillis());
							//we store data as filename seqid1 chunk1,  filename seqid2 chunk2
							logger.info("Retrieved sequenceNumbers", dataMap);
							for (Integer sequenceNo : dataMap.keySet()) {
								rb.setSuccess(true);
								rb.setKey(key);
								rb.setSequenceNo(sequenceNo);
								
								if (sequenceNo == 0) {
									try {
										rb.setMetaData(Metadata
											.parseFrom(dataMap.get(sequenceNo)));
									} catch (InvalidProtocolBufferException e) {
										e.printStackTrace();
									}
								} else {
									rb.setData(
										ByteString.copyFrom(dataMap.get(sequenceNo)));
								}
								System.out.println("Writing to Client on Success of file found");
								cb.setHeader(hb);
								cb.setResponse(rb);
								//System.out.println();
								ch.writeAndFlush(cb);	
								
							}
						}
						break;
					case STORE: 
						
						if( ElectionManager.getCurrentState() instanceof Leader )
						{
							// pass to all can be done in outgngq later 
							BroadcastUtil.cmdBroadcastWriteFlush(cmsg.getCmsg());
							System.out.println("chunk rcvd at leader and send to followers");
							writeChunktoDB(query,key);
							
						}
						else{
							//follower we have to respond to client the ip ,port node of leader.. 
							System.out.println("chunk rcvd");
							writeChunktoDB(query,key);
						}
						
						
						
						break;
					
					default:
						break;
					}
					//write to dB
					
					
				} catch ( Exception  e) {
					System.out.println("Exception in Thread" + id);
					e.printStackTrace();
				}
	      
	        }
	    }
	    
	    private void writeChunktoDB(Query query, String key){
	    if (query.hasKey()) {
	    	
			if (query.hasMetadata()) {
				redisCLient.put(key, 0,
					query.getMetadata().toByteArray());
			} else {
				key = redisCLient.put(query.getKey(),
					query.getSequenceNo(),
					query.getData().toByteArray());
			}
		} else {
			key = redisCLient.store(query.getData().toByteArray());

			Metadata.Builder mb = Metadata.newBuilder();
			mb.setSeqSize(1);
			mb.setTime(System.currentTimeMillis());
			mb.setSize(query.getData().size());
			redisCLient.put(key, 0, mb.build().toByteArray());
		}
	    }
}
