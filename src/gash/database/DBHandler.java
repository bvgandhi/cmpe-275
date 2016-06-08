package gash.database;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 
 * @author bg
 * 
 */
 
public class DBHandler {
	Jedis redis;
	static JedisPool jedisPool;

	public synchronized Jedis getJedisConnection(String ip, int port) throws JedisConnectionException {
		if (jedisPool == null) {
			JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
			jedisPoolConfig.setMaxTotal(10);
			jedisPoolConfig.setMaxIdle(5);
			jedisPoolConfig.setMinIdle(1);
			jedisPool = new JedisPool(jedisPoolConfig, ip,port);
		}
		return jedisPool.getResource();
	}

	
	public DBHandler(String ip, int port) {
		redis = getJedisConnection(ip,port);
	}

	public byte[] serialize(Object obj) throws IOException {
		try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
			try (ObjectOutputStream o = new ObjectOutputStream(b)) {
				o.writeObject(obj);
			}
			return b.toByteArray();
		}
	}

	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
			try (ObjectInputStream o = new ObjectInputStream(b)) {
				return o.readObject();
			}
		}
	}

	public boolean hasKey(String key) {
		boolean returnVal = false;
		try {
			if (redis.exists(serialize(key))) {
				returnVal = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnVal;
	}

	public String put(String key, int sequenceId, byte[] value) {
		// TODO Auto-generated method stub
		if (key == null || value == null) {
			return null;
		}
		try {
			redis.hset(serialize(key), serialize(sequenceId), value);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return key;
	}

	
	public String stringPut(String key, int sequenceId, byte[] value) {
		// TODO Auto-generated method stub
		if (key == null || value == null) {
			return null;
		}
		redis.hset(key+"_test",new Integer(sequenceId).toString(),Arrays.toString(value));
		return key;
	}
	
	public Map<Integer, byte[]> get(String key) {
		Map<Integer, byte[]> sequenceIdValue = new HashMap<Integer, byte[]>();
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				Set<byte[]> byteSequencIds = redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					sequenceIdValue.put((Integer) deserialize(bs), redis.hget(serializedKey, bs));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return sequenceIdValue;
	}

	public List<Integer> getSequenceIds(String key) {
		List<Integer> listSequenceIds = new ArrayList<Integer>();
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				Set<byte[]> byteSequencIds = redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					listSequenceIds.add((Integer) deserialize(bs));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return listSequenceIds;
	}

	public boolean update(String key, int sequenceId, byte[] value) {
		boolean isUpdated = false;
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				byte[] serializedSequenceId = serialize(sequenceId);
				if (redis.hexists(serializedKey, serializedSequenceId)) {
					redis.hset(serializedKey, serializedSequenceId, value);
					isUpdated = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isUpdated;
	}

	public String store(byte[] value) {
		if (value == null) {
			return null;
		}
		String uuid = UUID.randomUUID().toString();
		String key = put(uuid, 1, value);
		return key;
	}

	public Map<Integer, byte[]> remove(String key) {
		// TODO Auto-generated method stub
		Map<Integer, byte[]> removedMap = new HashMap<Integer, byte[]>();
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				Set<byte[]> byteSequencIds = redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					removedMap.put((Integer) deserialize(bs), redis.hget(serializedKey, bs));
					redis.hdel(serializedKey, bs);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return removedMap;
	}

	protected void finalize() throws Throwable {

		if (redis != null) {
			redis.close();
		}
		super.finalize();
	}
}
