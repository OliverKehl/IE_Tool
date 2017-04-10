package com.niuniu.cache;

import redis.clients.jedis.Jedis;

public class JedisUtilTest {
	public static void main(String[] args){
		Jedis jedis = JedisUtil.getInstance().getJedis("121.40.204.159", 6379);
		jedis.select(6);
		
		System.out.println(jedis.exists("kang"));
		System.out.println(jedis.get("kang"));
	}
}
