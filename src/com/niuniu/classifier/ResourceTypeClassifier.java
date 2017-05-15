package com.niuniu.classifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niuniu.Utils;
import com.niuniu.config.NiuniuBatchConfig;

/*
 * 区分某个平行进口车是期货还是现车
 */
public class ResourceTypeClassifier {
	public final static Logger log = LoggerFactory.getLogger(ResourceTypeClassifier.class);
	private ArrayList<Pattern> patterns;
	
	private static final ResourceTypeClassifier singleton;
	
	static{
		singleton = new ResourceTypeClassifier();
	}
	
	private ResourceTypeClassifier(){
		InputStream is = null;
        BufferedReader reader = null; 
        patterns = new ArrayList<Pattern>();
        
		try{
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getResourceTypeModel());
			if(is == null){
				log.error("[batch_processor]\t" + NiuniuBatchConfig.getResourceTypeModel() + "\t车源类型正则文件不存在");
				return;
	        }
			
			reader = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if(line.isEmpty())
					continue;
				Pattern pattern = Pattern.compile(line);
				patterns.add(pattern);
			}
			log.info("[batch_processor]\t" + NiuniuBatchConfig.getResourceTypeModel() + "\tt车源类型正则表达式初始化完成");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static String predict(String clue){
		if(clue.contains("现车")){
			return "现车";
		}
		
		for(Pattern p: singleton.patterns){
			Matcher m = p.matcher(clue);
			if(m.find()){
				return "期货";
			}
		}
		return null;
	}
	
	public static void main(String[] args){
		System.out.println(ResourceTypeClassifier.predict("5.20到港"));
		System.out.println(ResourceTypeClassifier.predict("5月底合同"));
		System.out.println(ResourceTypeClassifier.predict("期货"));
	}
}
