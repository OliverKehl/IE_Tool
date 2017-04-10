package com.niuniu;

import java.util.HashMap;
import java.util.Map;

public class MessageCaches {
	public static Map<String, CarResource> messageCache = null;
	public static Map<String, String> brandCache = null;
	public static Map<String, String> modelCache = null;
	
	static{
		messageCache = new HashMap<String, CarResource>();
		brandCache = new HashMap<String, String>();
		modelCache = new HashMap<String, String>();
	}
	
	public static synchronized boolean updateCache(String key, CarResource carResource, String brand_name, String car_model_name){
		MessageCaches.messageCache.put(key, carResource);
		MessageCaches.brandCache.put(key, brand_name);
		MessageCaches.modelCache.put(key, car_model_name);
		return true;
	}
	
}
