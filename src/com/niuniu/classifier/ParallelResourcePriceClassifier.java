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

public class ParallelResourcePriceClassifier {
	public final static Logger log = LoggerFactory.getLogger(ParallelResourcePriceClassifier.class);
	private ArrayList<Pattern> patterns;
	private Pattern price_pattern;
	private static final ParallelResourcePriceClassifier singleton;
	
	static{
		singleton = new ParallelResourcePriceClassifier();
	}
	
	private ParallelResourcePriceClassifier(){
		InputStream is = null;
        BufferedReader reader = null; 
        patterns = new ArrayList<Pattern>();
        price_pattern = Pattern.compile("\\d{2,3}(\\.\\d{1,2})?");
		try{
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getParallelPriceModel());
			if(is == null){
				log.error("[batch_processor]\t {} \t平行进口车价格正则文件不存在", NiuniuBatchConfig.getParallelPriceModel());
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
			log.info("[batch_processor]\t {} \t平行进口车价格正则表达式初始化完成", NiuniuBatchConfig.getParallelPriceModel());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static String predict(String clue){
		for(Pattern p: singleton.patterns){
			Matcher m = p.matcher(clue);
			while(m.find()){
				int start = m.start();
				int end = m.end();
				if(clue.charAt(start)<'0' || clue.charAt(start)>'9')
					start++;
				if(clue.charAt(start)=='0')
					return null;
				String tmp = clue.substring(start, end);
				if(tmp.startsWith("825") || tmp.startsWith("380"))
					continue;
				Matcher m2 = singleton.price_pattern.matcher(tmp);
				if(m2.find())
					return tmp.substring(m2.start(), m2.end());
				
			}
		}
		return null;
	}
	public static void main(String[] args){
		System.out.println(ParallelResourcePriceClassifier.predict("价格：141.88"));
	}
}
