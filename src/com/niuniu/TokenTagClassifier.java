package com.niuniu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niuniu.config.NiuniuBatchConfig;


/*
 *  根据品牌信息决定该token应该属于什么tag
 */
public class TokenTagClassifier {
	
	private Map<String, String> taggingMap = null;
	public final static Logger log = LoggerFactory.getLogger(TokenTagClassifier.class);
	//需要换成其他方式的路径
	
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
        BufferedReader reader = null; 
		try{
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getTokenTagModel());
			if(is == null){
				log.error(NiuniuBatchConfig.getTokenTagModel() + "\t标签模型不存在");
				return;
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
			log.info(NiuniuBatchConfig.getTokenTagModel() + "\ttoken打标初始化完成");
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
