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
import com.niuniu.USolr;
import com.niuniu.Utils;
import com.niuniu.config.NiuniuBatchConfig;

public class ParallelResourcePriceExtractor {
	
	public final static Logger log = LoggerFactory.getLogger(ParallelResourcePriceExtractor.class);
	private ArrayList<Pattern> patterns;
	private Pattern price_pattern;
	private static final ParallelResourcePriceExtractor singleton;
	
	static{
		singleton = new ParallelResourcePriceExtractor();
	}
	
	private ParallelResourcePriceExtractor(){
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
	
	public static boolean extract(BaseCarFinder baseCarFinder) {
		baseCarFinder.setDiscount_way(5);
		baseCarFinder.setDiscount_content(0f);
		ArrayList<String> ele_arr = baseCarFinder.getEle_arr();
		for (int i = baseCarFinder.getVital_info_index(); i < ele_arr.size(); i++) {
			String element = ele_arr.get(i);

			if (i - baseCarFinder.getVital_info_index() >= 15)// 已经在扫配置信息了，停止，不指望从配置信息里获取价格，价格索性就电议，然后填到备注里
				break;

			if (!element.endsWith("PRICE")) {
				continue;
			}
			String fc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
			if(fc.startsWith("0"))//车架号
				continue;
			float p = NumberUtils.toFloat(fc);
			if (p < 20 || p >= 500 || fc.matches("[0-9]{4,6}$"))// 平行进口车的价格不会落在这个区间外
				continue;
			
			if(fc.endsWith(".00") || fc.endsWith(".0")){
				singleton.setParallelPrice(p, baseCarFinder);
				return true;
			}
			
			if (i > 0) {
				String tmp = ele_arr.get(i - 1);
				String kfc = tmp.substring(tmp.lastIndexOf("|") + 1, tmp.indexOf("#"));
				if ("特价".equals(kfc) || "现价".equals(kfc)) {
					singleton.setParallelPrice(p, baseCarFinder);
					return true;
				}
			}

			String head_str = element.substring(element.indexOf("-") + 1, element.indexOf("|"));
			int head = NumberUtils.toInt(head_str);
			if ((head >= 1 && baseCarFinder.getOriginal_message().charAt(head - 1) == '价')) {
				singleton.setParallelPrice(p, baseCarFinder);
				return true;
			}

			if ((i + 1) < ele_arr.size()) {
				String tmp = ele_arr.get(i + 1);
				String kfc = tmp.substring(tmp.lastIndexOf("|") + 1, tmp.indexOf("#"));
				if ("万".equals(kfc) || "w".equals(kfc)) {
					singleton.setParallelPrice(p, baseCarFinder);
					return true;
				}
				boolean flag = true;
				for(int j=i+1;j<ele_arr.size();j++){
					if(!ele_arr.get(j).endsWith("#STOP")){
						flag = false;
						break;
					}
				}
				if(flag){
					singleton.setParallelPrice(p, baseCarFinder);
					return true;
				}
			}
			if((i+1)==ele_arr.size() && p>15){
				singleton.setParallelPrice(p, baseCarFinder);
				return true;
			}
			String tail_str = element.substring(element.indexOf("-") + 1, element.indexOf("|"));
			int tail = NumberUtils.toInt(tail_str);
			if (tail < baseCarFinder.getOriginal_message().length()
					&& (baseCarFinder.getOriginal_message().charAt(tail) == '万' || baseCarFinder.getOriginal_message().charAt(tail) == 'w')) {
				singleton.setParallelPrice(p, baseCarFinder);
				return true;
			}
		}
		if(ele_arr.size()>=15){
			String token = ele_arr.get(ele_arr.size()-1);
			if(token.endsWith("PRICE")){
				String content = token.substring(token.lastIndexOf("|") + 1, token.indexOf("#"));
				float f = NumberUtils.toFloat(content, 0f);
				if(f!=0 && f<1000 && !content.matches("[0-9]{4,6}$")){
					singleton.setParallelPrice(f, baseCarFinder);
					return true;
				}
			}else{
				String tmp = token.substring(token.lastIndexOf("|") + 1, token.indexOf("#"));
				if(tmp.equals("万") || tmp.equals("w")){
					if(ele_arr.size()-2>=10){
						String token2 = ele_arr.get(ele_arr.size()-2);
						float f2 = NumberUtils.toFloat(token2.substring(token2.lastIndexOf("|") + 1, token2.indexOf("#")), 0f);
						if(f2!=0){
							singleton.setParallelPrice(f2, baseCarFinder);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private String localExtract(String clue){
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
	
	public static void reExtract(CarResource cr, String info){
		String price = singleton.localExtract(info);
		//从price中提取数字部分
		float p = 0.0f;
		if( price != null){
			if(price.matches("\\d{1,3}(\\.\\d)?(w|W|万)$")){
				price = price.substring(0,  price.length()-1);
			}
		}else{
			ArrayList<String> tokens = Utils.tokenize(info, new USolr(NiuniuBatchConfig.getSolrHost()), "filter_word");
			int i = tokens.size() - 1;
			while(i>=0){
				if(tokens.get(i).endsWith("STOP"))
					i--;
				else
					break;
			}
			if(i>=0){
				String content = tokens.get(i);
				price = content.substring(content.lastIndexOf("|") + 1, content.lastIndexOf("#"));
			}
		}
		p = NumberUtils.toFloat(price);
		if(p>0 && p<500 && p!=380 && p!=825){
			cr.setDiscount_way("4");
			cr.setDiscount_content(Float.toString(p));
		}
	}
	
	private void setParallelPrice(float f, BaseCarFinder baseCarFinder){
		if(f<20)
			return;
		baseCarFinder.setDiscount_way(4);
		baseCarFinder.setDiscount_content(f);
	}
}
