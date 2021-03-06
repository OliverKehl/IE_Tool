package com.niuniu.cache;

import com.niuniu.config.NiuniuBatchConfig;

import redis.clients.jedis.Jedis;

public class CacheManager {
	
	private static String url;
	private static int port;
	private static int db;
	private static int ttl;
	
	static{
		url = NiuniuBatchConfig.getRedisHost();
		port = NiuniuBatchConfig.getRedisPort();
		db = NiuniuBatchConfig.getRedisIndex();
		ttl = NiuniuBatchConfig.getExpiredSeconds();
	}
	
	public static Jedis before(){
		Jedis jedis = JedisUtil.getInstance().getJedis(url, port);
		jedis.select(db);
		return jedis;
	}
	
	public static void after(Jedis jedis){
		JedisUtil.getInstance().closeJedis(jedis, url, port);
	}
	
	public static void set(String key, String value){
		Jedis jedis = CacheManager.before();
		jedis.setex(key, ttl, value);
		CacheManager.after(jedis);
	}
	
	public static void setex(String key, int seconds, String value){
		Jedis jedis = CacheManager.before();
		jedis.setex(key, seconds, value);
		CacheManager.after(jedis);
	}
	
	public static String get(String key){
		Jedis jedis = CacheManager.before();
		String res = null;
		if(jedis.exists(key))
			res = jedis.get(key);
		CacheManager.after(jedis);
		return res;
	}
	
	public static void clearDB(){
		Jedis jedis = CacheManager.before();
		jedis.flushDB();
		CacheManager.after(jedis);
	}
	
}
