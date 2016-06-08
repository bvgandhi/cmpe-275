package gash.router.server.election;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import gash.router.container.RoutingConf;
import gash.router.server.ServerState;
import gash.router.server.edges.EdgeMonitor;
import routing.Pipe.CommandMessage;
/**
 
 * @author bg
 * 
 */
public class ElectionManager implements Runnable{
	private ServerState state;
	private static int nodeId=-1;
	private int leaderId=-1;

	private RoutingConf conf;
	private EdgeMonitor emon;
	
	private long timerStart = 0;
	//This servers states
	private static ElectionState CurrentState;
	public ElectionState Leader;
	public ElectionState Candidate;
	public ElectionState Follower;
	
	private int heartBeatBase=3000;
	private long electionTimeout=3000;
	private long lastKnownBeat=0;
	private Random rand;
	private static int term = 0;
	
		private static AtomicInteger sequenceid ;
		
		private static Map<Integer, CommandMessage> serverid_chnksprcsd =  Collections.synchronizedMap(new LinkedHashMap<Integer, CommandMessage>(1000,1.33f,false));
		
		private static ConcurrentHashMap<String,List<Integer>> filesaved;
		
		public static ConcurrentHashMap<String, List<Integer>> getFilesaved() {
			return filesaved;
		}

		public static void setFilesaved(ConcurrentHashMap<String, List<Integer>> filesaved) {
			ElectionManager.filesaved = filesaved;
		}

		public static  AtomicInteger getSequenceid() {
			return sequenceid;
		}

		public static void setSequenceid(AtomicInteger sequenceid) {
			ElectionManager.sequenceid = sequenceid;
		}

		public static Map<Integer, CommandMessage> getChnksprcsd() {
			return serverid_chnksprcsd;
		}

		public static void setChnksprcsd(ConcurrentHashMap<Integer, CommandMessage> chnksprcsd) {
			ElectionManager.serverid_chnksprcsd = chnksprcsd;
		}
			
		public static int increment() {
	        return sequenceid.incrementAndGet();
	    }
		
		/*
		 * this mthd is called for each chunk written to db
		 */
		public static synchronized int putNincrement(CommandMessage cmsg){
			int serverSeqid=sequenceid.incrementAndGet();
			
			serverid_chnksprcsd.put(serverSeqid, cmsg);
			String key= cmsg.getQuery().getKey();
			if (filesaved.get(key)==null){
				
				List<Integer> clist=Collections.synchronizedList(new ArrayList<Integer>());
				clist.add(serverSeqid);
				System.out.println("FileName: "+key +"is saving");
				filesaved.put(key,clist);
			}
			else{
				filesaved.get(key).add(serverSeqid);
			}
			return serverSeqid;
			
		}
	public static synchronized ElectionState getCurrentState(){
		return CurrentState;
	}
	
	public synchronized long getTimerStart(){
		return timerStart;
	}
	
	public synchronized void setTimerStart(long t){
		 timerStart = t; 
	}
	
	public static synchronized void setCurrentState(ElectionState st){
		CurrentState = st;
	}
	
	public synchronized static int getNodeId(){
		return nodeId;
	}
	
	public synchronized int getLeaderId(){
		return leaderId;
	}
	
	public synchronized void setLeaderId(int id){
		leaderId = id;
	}
	
	public synchronized RoutingConf getRoutingConf(){
		return conf;
	}
	
	public synchronized EdgeMonitor getEmon(){
		return emon;
	}
	
	public synchronized int getTerm(){
		return term;
	}
	
	public synchronized void setTerm(int trm){
		term = trm;
	}
	
	public synchronized long getLastKnownBeat(){
		return lastKnownBeat;
	}
	
	public synchronized void setLastKnownBeat(long beatTime){
		lastKnownBeat = beatTime;
	}
	
	public synchronized void setState(ServerState state){
		this.state = state;
	}
	
	public ServerState getState(){
		if(state != null)
			return state;
		else
			return null;
	}
	
	public synchronized void setElectionTimeout(long et){
		electionTimeout = et;
	}
	
	public synchronized void randomizeElectionTimeout(){
		
		
			int temp = rand.nextInt(heartBeatBase);
			temp= temp+heartBeatBase;
			electionTimeout = (long)temp;
		
	}
	
	public synchronized long getElectionTimeout(){
		return electionTimeout;
	}
	
	public synchronized int getHbBase(){
		return heartBeatBase;
	}
	
	public ElectionManager(ServerState state){
		this.state= state;
	}
	
	public void init(){
		rand = new Random();
		Leader = new Leader();
		Leader.setManager(this);
		
		Candidate = new Candidate();
		Candidate.setManager(this);
		
		Follower = new Follower();
		Follower.setManager(this);
		
		this.conf = state.getConf();
		this.emon = state.getEmon();
		
		lastKnownBeat = System.currentTimeMillis();
		heartBeatBase = conf.getHeartbeatDt();
		nodeId = conf.getNodeId();
		
		randomizeElectionTimeout();
		this.electionTimeout = this.electionTimeout+4000;
		sequenceid=new AtomicInteger(0);
		CurrentState = Follower;
	}

	@Override
	public void run() {

		nodeId = conf.getNodeId();
		System.out.println("Node Id is + "+nodeId);
		while(true){
			
						timerStart = System.currentTimeMillis();
						CurrentState.process();
		}
		
	}

	

	
}
