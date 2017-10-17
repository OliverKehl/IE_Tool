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
 *  批量发布资源规格判定
 *  分类：
 *  		0：国产或中规		1：平行进口
 *  		   有指导价		   无指导价
 *  	
 */
public class MessageStandardClassifier {
	public final static Logger log = LoggerFactory.getLogger(ResourceTypeClassifier.class);
	private ArrayList<Pattern> patterns;
	
	private static final MessageStandardClassifier singleton;
	
	static{
		singleton = new MessageStandardClassifier();
	}
	
	private MessageStandardClassifier(){
		InputStream is = null;
        BufferedReader reader = null; 
        patterns = new ArrayList<Pattern>();
        
		try{
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getStandardModel());
			if(is == null){
				log.error("[batch_processor]\t {} \t车源类型正则文件不存在", NiuniuBatchConfig.getStandardModel());
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
			log.info("[batch_processor]\t {} \t规格正则表达式初始化完成", NiuniuBatchConfig.getResourceTypeModel());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static int predict(String clue){
		for(Pattern p: singleton.patterns){
			Matcher m = p.matcher(clue);
			if(m.find()){
				return 1;
			}
		}
		return 0;
	}
	
	public static void main(String[] args){
		System.out.println(MessageStandardClassifier.predict("蓝鸟1139 白 加4000"));
	}
}
