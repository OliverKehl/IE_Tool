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
public class ParallelResourceVinClassifier {
	public final static Logger log = LoggerFactory.getLogger(ParallelResourceVinClassifier.class);
	private ArrayList<Pattern> patterns;
	private Pattern digitPattern;
	private Pattern digitWithPoundPattern;
	private static final ParallelResourceVinClassifier singleton;
	
	static{
		singleton = new ParallelResourceVinClassifier();
	}
	
	private ParallelResourceVinClassifier(){
		InputStream is = null;
        BufferedReader reader = null; 
        patterns = new ArrayList<Pattern>();
        digitWithPoundPattern = Pattern.compile("#?\\s*\\d{4}\\s*#?");
        digitPattern = Pattern.compile("\\d{4}");
		try{
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getParallelVinModel());
			if(is == null){
				log.error("[batch_processor]\t {} \t车源车架号正则文件不存在", NiuniuBatchConfig.getParallelPriceModel());
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
			log.info("[batch_processor]\t {} \t车源车架号正则表达式初始化完成", NiuniuBatchConfig.getParallelPriceModel());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static String predict(StringBuilder clue){
		for(Pattern p: singleton.patterns){
			Matcher m = p.matcher(clue);
			if(m.find()){
				int start = m.start();
				int end = m.end();
				String tmp = clue.substring(start, end);
				Matcher m2 = singleton.digitWithPoundPattern.matcher(tmp);
				if(m2.find()){
					clue.replace(start + m2.start(), start + m2.end(), " ");
					m2 = singleton.digitPattern.matcher(tmp);
					if(m2.find())
						return tmp.substring(m2.start(), m2.end());
				}else{
					m2 = singleton.digitPattern.matcher(tmp);
					if(m2.find()){
						clue.replace(start + m2.start(), start + m2.end(), " ");
						return tmp.substring(m2.start(), m2.end());
					}
				}
			}
		}
		return null;
	}
	
	public static void main(String[] args){
		System.out.println(ParallelResourceVinClassifier.predict(new StringBuilder("17款揽运HSE版汽油 白黑3台 7830# 8513# 7855# 滑动天窗19轮 真皮方向盘 16项座椅电动调节 后视镜自动防眩目 前挡风加热 前雾灯 LED氙灯带大灯清洗 车道偏离警示 电尾 倒影 倒车助手 前后侧身隔热防噪音玻璃 现车90万")));
	}
}
