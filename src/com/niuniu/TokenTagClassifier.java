package com.niuniu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


/*
 *  根据品牌信息决定该token应该属于什么tag
 */
public class TokenTagClassifier {
	
	private Map<String, String> taggingMap = null;
	
	//需要换成其他方式的路径
	public static final String PATH = "/Users/kehl/Documents/workspace/MessageProcessor/src/com/niuniu/resource/indicator/tags.m";
	
	private static final TokenTagClassifier singleton;
	
	static{
		singleton = new TokenTagClassifier();
	}
	
	private TokenTagClassifier() {
		/*
		 * 初始化预测模型
		 */
		
		taggingMap = new HashMap<String, String>();
		InputStream is = null;
		File file = new File(PATH);
        BufferedReader reader = null; 
		try{
			is = TokenTagClassifier.class.getClassLoader().getResourceAsStream("com/niuniu/tags.m");
			if(is == null){
	        	throw new RuntimeException("标签模型不存在");
	        }
			reader = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			//reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				String[] arrs = line.split("\t");
				if(arrs.length!=2)
					continue;
				taggingMap.put(arrs[0].trim().toLowerCase(), arrs[1].trim());
			}
			System.out.println("token打标初始化完成");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static String predict(String token, String brand_name){
		String combination = brand_name + "#" + token;
		combination = combination.toLowerCase();
		if(singleton.taggingMap.containsKey(combination)){
			return singleton.taggingMap.get(combination);
		}
		return null;
	}
}
