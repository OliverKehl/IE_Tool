package com.niuniu.extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niuniu.BaseCarFinder;
import com.niuniu.CarResource;
import com.niuniu.Utils;
import com.niuniu.config.NiuniuBatchConfig;

public class ResourceVinExtractor {
	
	public final static Logger log = LoggerFactory.getLogger(ResourceVinExtractor.class);
	
	private ArrayList<Pattern> patterns;
	private Pattern digitPattern;
	private Pattern digitWithPoundPattern;
	private static final ResourceVinExtractor singleton;
	
	static{
		singleton = new ResourceVinExtractor();
	}
	
	private ResourceVinExtractor(){
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
	
	public static void extract(BaseCarFinder baseCarFinder) {
		ArrayList<String> ele_arr = baseCarFinder.getEle_arr();
		int backup_index = baseCarFinder.getBackup_index();
		String original_message = baseCarFinder.getOriginal_message();
		for (int i = baseCarFinder.getVital_info_index(); i < ele_arr.size(); i++) {
			String ele = ele_arr.get(i);
			String pre_ele = null;
			if (i > 0) {
				pre_ele = ele_arr.get(i - 1);
				if (pre_ele.contains("车架号")) {
					String content = ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#"));
					if (content.matches("[0-9]{4,6}$")) {
						String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
						int thehe = NumberUtils.toInt(hehe);
						baseCarFinder.setBackup_index(Math.max(backup_index, thehe));
						baseCarFinder.setVin(content);
						return;
					}
				}
			}
			if (ele.endsWith("PRICE") || ele.endsWith("OTHERS")) {
				String head_str = ele.substring(0, ele.indexOf("-"));
				String tail_str = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
				String content = ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#"));
				int head = NumberUtils.toInt(head_str);
				int tail = NumberUtils.toInt(tail_str);
				if (content.matches("[0-9]{4,6}$")) {
					if (content.startsWith("0")) {// 0开头肯定是车架号
						String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
						int thehe = NumberUtils.toInt(hehe);
						baseCarFinder.setBackup_index(Math.max(backup_index, thehe));
						baseCarFinder.setVin(content);
						return;
					}
					while(head>=1 && original_message.charAt(head - 1) == ' ')
						head--;
					if ((head >= 1 && original_message.charAt(head - 1) == '#')) {
						String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
						int thehe = NumberUtils.toInt(hehe);
						baseCarFinder.setBackup_index(Math.max(backup_index, thehe));
						baseCarFinder.setVin(content);
						return;
					}
					
					while(tail< original_message.length() && original_message.charAt(tail)==' ')
						tail++;
					if ((tail < original_message.length() && original_message.charAt(tail) == '#')) {
						baseCarFinder.setBackup_index(Math.max(backup_index, tail + 1));
						baseCarFinder.setVin(content);
						return;
					}
					
					String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
					int thehe = NumberUtils.toInt(hehe);
					baseCarFinder.setBackup_index(Math.max(backup_index, thehe));
					baseCarFinder.setVin(content);
					return;
					
				}
			}
		}
	}
	
	public static void reExtract(StringBuilder clue, CarResource cr){
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
					if(m2.find()){
						cr.setVin(tmp.substring(m2.start(), m2.end()));
						return;
					}
				}else{
					m2 = singleton.digitPattern.matcher(tmp);
					if(m2.find()){
						clue.replace(start + m2.start(), start + m2.end(), " ");
						cr.setVin(tmp.substring(m2.start(), m2.end()));
						return;
					}
				}
			}
		}
	}
	
	public static void main(String[] args){
		CarResource cr = new CarResource();
		ResourceVinExtractor.reExtract(new StringBuilder("17款揽运HSE版汽油 白黑3台 7830# 8513# 7855# 滑动天窗19轮 真皮方向盘 16项座椅电动调节 后视镜自动防眩目 前挡风加热 前雾灯 LED氙灯带大灯清洗 车道偏离警示 电尾 倒影 倒车助手 前后侧身隔热防噪音玻璃 现车90万"), cr);
		System.out.println(cr.getVin());
	}
	
}
