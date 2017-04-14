package com.niuniu.cache;

import java.util.HashMap;
import java.util.Map;

import com.niuniu.config.NiuniuBatchConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
 
public class JedisUtil
{
 
    private JedisUtil(){}
 
    private static class RedisUtilHolder{
        private static final JedisUtil instance = new JedisUtil();
    }
 
    public static JedisUtil getInstance(){
        return RedisUtilHolder.instance;
    }
 
    private static Map<String,JedisPool> maps = new HashMap<String,JedisPool>();
 
    private static JedisPool getPool(String ip, int port){
        String key = ip+":"+port;
        JedisPool pool = null;
        if(!maps.containsKey(key))
        {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(RedisConfig.MAX_ACTIVE);
            config.setMaxIdle(RedisConfig.MAX_IDLE);
            config.setMaxWaitMillis(RedisConfig.MAX_WAIT);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);
 
            if(!NiuniuBatchConfig.getRedisPassword().isEmpty())
            	pool = new JedisPool(config,ip,port,RedisConfig.TIMEOUT, NiuniuBatchConfig.getRedisPassword());
            else
            	pool = new JedisPool(config,ip,port,RedisConfig.TIMEOUT);
            maps.put(key, pool);
        }
        else
        {
            pool = maps.get(key);
        }
        return pool;        
    }
 
    public Jedis getJedis(String ip, int port)
    {
        Jedis jedis = null;
        int count = 0;
        do
        {
            try
            {
                jedis = getPool(ip,port).getResource();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                getPool(ip,port).returnBrokenResource(jedis);
            }
        }
        while(jedis == null && count<RedisConfig.RETRY_NUM);
        return jedis;
    }
 
    public void closeJedis(Jedis jedis, String ip, int port){
        if(jedis != null)
        {
            getPool(ip,port).returnResource(jedis);
        }
    }
}
