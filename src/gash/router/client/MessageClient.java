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
package gash.router.client;

import com.google.protobuf.ByteString;

import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;
import storage.Storage.Action;
import storage.Storage.Metadata;
import storage.Storage.Query;

/**
 * front-end (proxy) to our service - functional-based
 * 
 * @author gash
 * 
 */
public class MessageClient {
	// track requests
	private long curID = 0;

	public MessageClient(String host, int port) {
		init(host, port);
	}

	private void init(String host, int port) {
		CommConnection.initConnection(host, port);
	}

	public void addListener(CommListener listener) {
		CommConnection.getInstance().addListener(listener);
	}

	public void ping() {
		// construct the message to send
		Header.Builder hb = buildHeader();
		
		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setPing(true);
		
		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(rb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void store(ByteString data) {
		Header.Builder hb = buildHeader();
		;

		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);

		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.STORE);
		qb.setData(data);
		qb.setSequenceNo(1);
	
		cb.setQuery(qb);

		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void get(String key) {
		Header.Builder hb = buildHeader();
	
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);
	
		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.GET);
		qb.setKey(key);
	
		cb.setQuery(qb);
	
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void put(String key, int sequenceNo, ByteString data) {
		Header.Builder hb = buildHeader();
	
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);
	
		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.STORE);
		qb.setKey(key);
		qb.setSequenceNo(sequenceNo);
		qb.setData(data);
		cb.setQuery(qb);
	
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void putMetadata(String key, int seqSize, long fileLength) {
		Header.Builder hb = buildHeader();
	
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);
	
		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.STORE);
		qb.setKey(key);
		qb.setSequenceNo(0);
		
		Metadata.Builder mb = Metadata.newBuilder();
		mb.setSeqSize(seqSize);
		mb.setSize(fileLength);
		mb.setTime(System.currentTimeMillis());
		
		qb.setMetadata(mb);
		cb.setQuery(qb);
	
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	private Header.Builder buildHeader() {
		Header.Builder hb = Header.newBuilder();
		hb.setDestination(-1);
		hb.setMaxHops(5);
		hb.setTime(System.currentTimeMillis());
		hb.setNodeId(-1);
		return hb;
	}

	public void release() {
		CommConnection.getInstance().release();
	}

	/**
	 * Since the service/server is asychronous we need a unique ID to associate
	 * our requests with the server's reply
	 * 
	 * @return
	 */
	private synchronized long nextId() {
		return ++curID;
	}
}
