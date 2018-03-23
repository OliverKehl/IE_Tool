package com.niuniu.extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niuniu.BaseCarFinder;
import com.niuniu.CarResource;
import com.niuniu.Utils;
import com.niuniu.config.NiuniuBatchConfig;

public class ResourceTypeExtractor {
	public final static Logger log = LoggerFactory.getLogger(ResourceTypeExtractor.class);
	private ArrayList<Pattern> patterns;
	
	private static final ResourceTypeExtractor singleton;
	
	static{
		singleton = new ResourceTypeExtractor();
	}
	
	private ResourceTypeExtractor(){
		InputStream is = null;
        BufferedReader reader = null; 
        patterns = new ArrayList<Pattern>();
        
		try{
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getResourceTypeModel());
			if(is == null){
				log.error("[batch_processor]\t {} \t车源类型正则文件不存在", NiuniuBatchConfig.getResourceTypeModel());
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
			log.info("[batch_processor]\t {} \t车源类型正则表达式初始化完成", NiuniuBatchConfig.getResourceTypeModel());
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void extract(BaseCarFinder baseCarFinder, String clue){
		if(clue.contains("现车")){
			baseCarFinder.setResource_type("现车");
		}
		
		for(Pattern p: singleton.patterns){
			Matcher m = p.matcher(clue);
			if(m.find()){
				baseCarFinder.setResource_type("期货");
			}
		}
	}
	
	public static void reExtract(CarResource cr, String clue){
		if(clue.contains("现车")){
			cr.setResource_type("现车");
		}
		
		for(Pattern p: singleton.patterns){
			Matcher m = p.matcher(clue);
			if(m.find()){
				cr.setResource_type("期货");
			}
		}
	}
	
	public static void main(String[] args){
		CarResource cr = new CarResource();
		ResourceTypeExtractor.reExtract(cr, "5.20到港");
		System.out.println(cr.getResource_type());
		ResourceTypeExtractor.reExtract(cr, "5月底合同");
		System.out.println(cr.getResource_type());
		ResourceTypeExtractor.reExtract(cr, "期货");
		System.out.println(cr.getResource_type());
	}
}
