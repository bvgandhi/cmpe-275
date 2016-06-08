/**
 * 
 */
package gash.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import gash.router.utils.SerializationUtil;
import routing.Pipe.CommandMessage;
import storage.Storage.Action;
import storage.Storage.Response;

/**
 
 * @author Dhaval
 * 
 */
public class Client implements CommListener {

	private static Logger logger = LoggerFactory.getLogger(Client.class);
	private static final int M = 1024 * 1024;

	private String host = "localhost";
	private int port = 4168;
	private MessageClient mc;
	private String filepath;
	private List<Response> responseList;
	private int responseSize;
	private boolean fileOutput = true;

	public Client() throws InterruptedException {
		mc = new MessageClient(host, port);
		responseList = new LinkedList<>();
		mc.addListener(this);
	}

	@Override
	public String getListenerID() {
		return "client";
	}

	@Override
	public void onMessage(CommandMessage msg) {

		if (!fileOutput) {
			logger.info(msg.toString ());
		}

		if (msg.getResponse().getAction() == Action.GET) {

			logger.info(" GOT CHUNKS FROM SERVER");
			if (msg.getResponse().hasMetaData()) {
				responseSize = msg.getResponse().getMetaData().getSeqSize();
			} else {
				ByteString data = msg.getResponse().getData();
				String str = new String(data.toByteArray());
				
				if (!fileOutput) {
					logger.info("Fetched data: " + str);
				}
				System.out.println(msg.getResponse().getKey() + " seqNo: " + msg.getResponse().getSequenceNo() );
				responseList.add(msg.getResponse());

				if (responseList.size() == responseSize) {
					Collections.sort(responseList, (o1, o2) -> o1.getSequenceNo() - o2.getSequenceNo());

					List<ByteString> list = new LinkedList<>();
					for (Response response : responseList) {
						list.add(response.getData());
					}

					if (fileOutput) {
						System.out.println();
						SerializationUtil util = new SerializationUtil();
						util.writeFile(filepath, list);
					}
				}
			}
		}
	}

	public void handleCommand(String[] args) throws FileNotFoundException {
		if (args.length < 1) {
			logger.info("Input Correct number of args!");
			return;
		}

		logger.info(Thread.currentThread() + ": Handling " + args[0] );
		switch (args[0].toUpperCase()) {
		case "GET":

			if (args.length < 3) {
				logger.info(
					"Not enough params.\n->Key\n->Output File Location");
				return;
			}
			fileOutput = true;
			String key = args[1];
			filepath = args[2];
			mc.get(key);
			break;
		case "PUT":
			if (args.length < 3) {
				logger.info(
					"Not enough params.\n->Key\n->Input File Location");
				return;
			}
			key = args[1];
			filepath = args[2];
			SerializationUtil util = new SerializationUtil();
			File tempFile = new File(filepath);
			if (!tempFile.exists()) {
				throw new FileNotFoundException(filepath);
			}
			long fileSize = tempFile.length();
			mc.putMetadata(key, (int) (Math.ceil(1.0 * fileSize / M)),
				fileSize);
			for (int i = 0; i < 1 + (fileSize / M / 10); i++) {
				List<ByteString> dataList = util.readfile(filepath, 0, M, 10);

				int sequenceNo = 1;
				for (ByteString data : dataList) {
					mc.put(key, sequenceNo++, data);
				}
			}
			break;

	
		default:
			logger.info("Operation not supported. Use one of the :");
			logger.info("GET");
			logger.info("PUT");
			break;
		}

		try {
			Thread.sleep(20 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
	}
	
	public void releaseClient() {
		logger.info("Client closing...");
		mc.release();
	}
	

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args)
		throws InterruptedException, FileNotFoundException {
		Client c = new Client();
		c.handleCommand(args);
		c.releaseClient();
	}
}
