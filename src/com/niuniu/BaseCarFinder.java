package com.niuniu;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.niuniu.cache.CacheManager;
import com.niuniu.classifier.PriceValidationClassifier;
import com.niuniu.classifier.TokenTagClassifier;
import com.niuniu.config.NiuniuBatchConfig;

public class BaseCarFinder {

	public final static Logger log = LoggerFactory.getLogger(BaseCarFinder.class);

	public ArrayList<String> models;
	public ArrayList<String> prices;
	public ArrayList<String> brands;
	public ArrayList<String> styles;
	public ArrayList<String> years;
	public ArrayList<String> colors;
	ArrayList<Integer> indexes;
	ArrayList<String> ele_arr;
	SolrDocumentList query_results;
	int vital_info_index;
	int discount_way = 5;
	float discount_content = 0f;
	Set<String> base_colors_set;
	String[] base_colors = { "黑", "白", "红", "灰", "棕", "银", "金", "蓝", "紫", "米" };
	Set<String> result_colors;
	String pre_brand_name;

	String cur_brand;
	String cur_model;
	
	String original_message;

	USolr solr;

	int backup_index = 0;

	String[] suffix_quants = { "台", "轮", "度", "速", "天", "分钟", "小时", "秒", "辆", "年", "月", "寸", "月底", "号", "项", "匹", "缸", "气囊"};
	String[] prefix_behave = { "送" };

	Set<String> suffix_quants_set;
	Set<String> prefix_behave_set;
	
	boolean colorBeforePrice = false;

	Set<String> specialDigitalToken = null;
	String[] specialDigits = {"1500","1794","2500","2700","3000","3004","3500","4000","4500","4600","5000","5700","5802","6004","6104","7004","7504"};
	
	// 指导价
	// 考虑色全的情况

	private void init(){
		models = new ArrayList<String>();
		prices = new ArrayList<String>();
		brands = new ArrayList<String>();
		styles = new ArrayList<String>();
		colors = new ArrayList<String>();
		years = new ArrayList<String>();
		indexes = new ArrayList<Integer>();
		ele_arr = new ArrayList<String>();
		base_colors_set = new HashSet<String>();
		for (String s : base_colors) {
			base_colors_set.add(s);
		}
		result_colors = new HashSet<String>();

		suffix_quants_set = new HashSet<String>();
		prefix_behave_set = new HashSet<String>();

		for (String s : suffix_quants)
			suffix_quants_set.add(s);

		for (String s : prefix_behave)
			prefix_behave_set.add(s);
		
		specialDigitalToken = new HashSet<String>();
		for(int i=0;i<specialDigits.length;i++)
			specialDigitalToken.add(specialDigits[i]);
	}
	
	public BaseCarFinder() {
		init();
		solr = new USolr("http://121.40.204.159:8080/solr/");
	}

	public BaseCarFinder(USolr solr) {
		init();
		this.solr = solr;
	}

	public BaseCarFinder(USolr solr, String pre_brand_name) {
		init();
		this.solr = solr;
		this.pre_brand_name = pre_brand_name;
	}

	/*
	 * 从search库里获取多个候选base_car_id
	 */
	private void fillBaseCarIds(Map<Integer, Float> result, SolrDocumentList docs) {
		float threshold = docs.getMaxScore();
		SolrDocumentList sdl = new SolrDocumentList();
		for (SolrDocument doc : docs) {// 遍历结果集
			long base_car_id = (long) doc.get("id");
			float score = (float) doc.get("score");
			if(score>=threshold){
				result.put((int) base_car_id, score);
				sdl.add(doc);
			}
		}
		docs.retainAll(sdl);
		docs.setNumFound(sdl.size());
	}

	/*
	 * 根据分数等信息，评估候选base_car_id
	 */
	public boolean assessBaseCarCandidates(Map<Integer, Float> result) {
		Iterator<Entry<Integer, Float>> entries = result.entrySet().iterator();
		float max_score = 0;
		while (entries.hasNext()) {
			Entry<Integer, Float> entry = (Entry<Integer, Float>) entries.next();
			float value = entry.getValue();
			if ((1.2 * value) < max_score) {
				break;
			}
		}
		return true;
	}

	public boolean isStandardPrice(String str) {
		String tmp = str.substring(str.lastIndexOf("|") + 1, str.indexOf("#"));
		float f = NumberUtils.toFloat(tmp, 0);
		if (f < 100 && f > 10)
			return true;
		if (f > 10000)
			return true;
		if (f > 1000) {
			if (f == 2008 || f == 3008 || f == 4008)
				return false;
			return true;
		}
		if(f>100 && f%10!=0 )
			return true;
		return false;
	}

	/*
	 * -1 means price 1 means model
	 */
	private String judgeRealTag(String token) {
		String real_tag = null;
		if (!brands.isEmpty()) {
			real_tag = TokenTagClassifier.predict(token, brands.get(0));
		} else {
			real_tag = TokenTagClassifier.predict(token, pre_brand_name);
		}
		return real_tag;
	}
	
	private String judgeRealTagByLocalContext(String token, String local_context) {
		if(local_context==null || local_context.isEmpty())
			return null;
		if(!local_context.contains("MODEL") && !local_context.contains("BRAND")){
			return null;
		}
		String real_tag = null;
		String sub_key = local_context.substring(local_context.lastIndexOf("|") + 1, local_context.indexOf("#"));
		real_tag = TokenTagClassifier.predict(token, sub_key);
		return real_tag;
	}
	
	private boolean priceSuffix(int cur){
		if((cur+1)>=ele_arr.size())
			return false;
		String content = ele_arr.get(cur + 1);
		content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
		if ("折".equals(content) || "万".equals(content) || "点".equals(content) || "w".equals(content))
			return true;
		return false;
	}
	
	private boolean isFrontVinWithSpace(int idx, String message, int standard){
		if(standard==1)
			return false;
		while(idx>0){
			if(message.charAt(idx-1)==' ')
				idx--;
			else if(message.charAt(idx-1)=='#')
				return true;
			else 
				break;
		}
		return false;
	}
	
	private boolean isBehindVinWithSpace(int idx, String message, int standard){
		if(standard==1)
			return false;
		while(idx<message.length()){
			if(message.charAt(idx)==' ')
				idx++;
			else if(message.charAt(idx)=='#')
				return true;
			else 
				break;
		}
		return false;
	}
	
	private int parse(ArrayList<String> tokens, String message, int standard) {
		boolean price_status = false;
		int i=0;
		boolean stop_color_area_status = false;
		for (; i < tokens.size(); i++) {
			String s = tokens.get(i);
			int start = NumberUtils.toInt(s.substring(0, s.indexOf('-')));
			if (start > 0 && isFrontVinWithSpace(start, message, standard)) {
				return i;
			}
			backup_index = Math.max(start,
					backup_index);
			
			if(stop_color_area_status && !s.endsWith("MODEL") && !s.endsWith("BRAND")){
				return i;
			}
			if(s.endsWith("#STOP")){
				if(s.contains("报价")){
					if(standard==2)
						return i;
				}
				if(s.contains("特价")){
					if(i<tokens.size()-1){
						String next = tokens.get(i+1);
						String val = next.substring(next.lastIndexOf("|") + 1, next.indexOf("#"));
						float f = NumberUtils.toFloat(val,0.0f);
						if(f!=0.0){
							return i;
						}
					}else{
					}
				}
				continue;
			}
			if (s.endsWith("#OTHERS") || s.endsWith("#COLOR") || s.endsWith("AREA")) {
				if(i==0)
					return i;
				backup_index = Math.max(NumberUtils.createInteger(s.substring(0, s.indexOf('-'))),
						backup_index);
				
				if(standard!=2 && s.endsWith("COLOR") && !price_status){
					colorBeforePrice = true;
					continue;
				}
				if (s.contains("指导价")) {
					//tokens.remove(i);
					continue;
				}
				if(!s.endsWith("#COLOR")){
					return i;
				}
				if(!stop_color_area_status){
					stop_color_area_status = true;
					continue;
				}else{
					return i-1;
				}
			} else if (s.endsWith("#BRAND")) {
				brands.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
			} else if (s.endsWith("#MODEL")) {
				models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
			} else if (s.endsWith("#STYLE")) {
				String tmp = s.substring(s.lastIndexOf("|") + 1, s.indexOf("#"));
				float tmp_f = NumberUtils.toFloat(tmp, 0f);
				if(tmp_f>0){
					int j = i+1;
					if(j<tokens.size()){
						String s2 = tokens.get(j);
						String tmp2 = s2.substring(s2.lastIndexOf("|") + 1, s2.indexOf("#"));
						if(tmp2.equals("w") || tmp2.equals("折") || tmp2.equals("万") || tmp2.equals("点"))
							return i;
					}
				}
				styles.add(tmp);
			} else if (s.endsWith("MODEL_STYLE")) {
				String real_tag = null;
				String content = s.substring(s.lastIndexOf("|") + 1, s.indexOf("#"));
				real_tag = judgeRealTag(content);
				if(real_tag==null){
					for(int j=i-1;j>=0;j--){
						real_tag = judgeRealTagByLocalContext(content, tokens.get(j));
						if(real_tag!=null)
							break;
					}
				}
				if (real_tag != null) {
					if (real_tag.equals("MODEL")) {
						models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						continue;
					} else if (real_tag.equals("STYLE")) {
						styles.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						continue;
					}
				}else{
					// 在不确定一个term是model还是style时，把它优先归为model
					// 因为这里的归类只是作为辅助功能，并不会对搜索的精度造成影响  
					models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
		            continue;
				}
			} else if (s.endsWith("#STYLE_PRICE") || s.endsWith("#FPRICE") || s.endsWith("MODEL_PRICE")
					|| s.endsWith("MODEL_STYLE_PRICE")) {
				String content = s.substring(s.lastIndexOf("|") + 1, s.indexOf("#"));
				if(i>0 && ele_arr.get(i-1).contains("指导价")){
					return Math.min(i + 1, tokens.size());
				}
				
				if(isQuantOrBehave(i) || priceSuffix(i) || (price_status && content.endsWith("000") && !content.equals("4000"))){
					return i;
				}
				
				if(isYearToken(i)){
					styles.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
					continue;
				}
				
				if (price_status){
					int head = NumberUtils.toInt(s.substring(0, s.indexOf("-")));
					int tail = NumberUtils.toInt(s.substring(s.indexOf("-") + 1, s.indexOf("|")));
					if((content.startsWith("0")&& content.length()==4) || isFrontVinWithSpace(head, message, standard) || isBehindVinWithSpace(tail, message, standard))
						return i;
					prices.add(content);
					return Math.min(i + 1, tokens.size());
				}
				int price_int = NumberUtils.toInt(content,0);
				if(price_int!=0 && price_int<100000 && (price_int%1000==0 || price_int%10000==0) && price_int!=3000 && price_int!=4000)
					return i;
				//扫到指导价
				if(colorBeforePrice && standard!=2){
					prices.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
					price_status = price_status | isStandardPrice(s);
					prices.add(content);
					return Math.min(i + 1, tokens.size());
				}
				String real_tag = null;
				real_tag = judgeRealTag(content);
				if(real_tag==null){
					for(int j=i-1;j>=0;j--){
						real_tag = judgeRealTagByLocalContext(content, tokens.get(j));
						if(real_tag!=null)
							break;
					}
				}
				if (real_tag != null) {
					if (real_tag.equals("MODEL")) {
						models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						continue;
					} else if (real_tag.equals("STYLE")) {
						styles.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						continue;
					}
				} else {
					String hehe = s.substring(s.lastIndexOf("|") + 1, s.indexOf("#"));
					int head = NumberUtils.toInt(s.substring(0, s.indexOf("-")));
					int tail = NumberUtils.toInt(s.substring(s.indexOf("-") + 1, s.indexOf("|")));
					if(standard==2 && ((content.startsWith("0")&& content.length()==4) || isFrontVinWithSpace(head, message, standard) || isBehindVinWithSpace(tail, message, standard)))
						return i;
					if(hehe.indexOf('.')>0 && hehe.length() - hehe.indexOf('.')>3){
						return i;
					}
					float f_hehe = NumberUtils.toFloat(hehe);
					int i_hehe = (int)f_hehe;
					/*
					 * 2700
					 * 4008
					 * 308
					 */
					if(i_hehe<10 || (i_hehe%100==0 && i_hehe>300 && i_hehe<10000) || (i_hehe%10==0 &&i_hehe>300 && i_hehe!=380 && i_hehe<10000)){
						if(f_hehe==i_hehe){
							models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						}else{
							prices.add(hehe);
							price_status = price_status | isStandardPrice(s);
						}
						continue;
					}
					boolean flag = false;
					if(i>0){
						String t = tokens.get(i-1);
						if(t.endsWith("COLOR") || t.endsWith("OTHERS"))
							flag = true;
					}
					if(standard==1 || (standard==1 && !flag && hehe.length()<4) || (standard==2 && specialDigitalToken.contains(hehe))){
						prices.add(hehe);
						price_status = price_status | isStandardPrice(s);
					}else{
						return i;
					}
				}
			}
		}
		if(i==tokens.size()){
			String str = tokens.get(i-1);
			backup_index = Math.max(NumberUtils.createInteger(str.substring(str.indexOf('-')+1, str.indexOf('|'))),
					backup_index);
		}
			
		return tokens.size();
	}

	private String parseMessage(USolr solr, String message, int standard) {
		ele_arr = Utils.tokenize(message, solr, "filter_word");
		if (ele_arr == null)
			return null;
		vital_info_index = parse(ele_arr, message, standard);
		int stop = 0;
		if (vital_info_index == ele_arr.size()) {
			String tmp = ele_arr.get(vital_info_index-1);
			stop = NumberUtils.toInt(tmp.substring(tmp.indexOf("-") + 1, tmp.indexOf("|")));
		} else {
			String tmp = ele_arr.get(vital_info_index);
			stop = NumberUtils.createInteger(tmp.substring(0, tmp.indexOf("-")));
		}
		String sub_query = message.substring(0, stop).trim();
		if(standard==2 && vital_info_index==1){
			String str = ele_arr.get(vital_info_index-1);
			String content = str.substring(str.lastIndexOf("|") + 1, str.lastIndexOf("#"));
			if(str.endsWith("STYLE") && content.length()<5)
				sub_query = "";
		}
		return sub_query;
	}
	
	public boolean generateBaseCarId(String message, String pre_info) {
		if (message.isEmpty())
			return false;
		this.original_message = message;
		message = Utils.preProcess(message);
		Map<Integer, Float> base_car_info = new HashMap<Integer, Float>();
		if (solr == null)
			return false;
		String sub_query = parseMessage(solr, message, 1);
		fillYearToken();
		if (pre_info != null)
			sub_query = pre_info + " " + sub_query;
		query_results = Utils.select(sub_query, solr, years);
		//if (query_results == null || (sub_query.length()<3 && brands.isEmpty() && models.isEmpty() && styles.isEmpty())) {
		if (query_results == null) {
			return false;
		}
		fillBaseCarIds(base_car_info, this.query_results);
		solr.clear();
		return base_car_info.size() > 0;
	}

	SolrDocumentList filterQueryResult(SolrDocumentList qrs){
		float gap = 1999;
		float max_score = qrs.getMaxScore();
		float entry_score = max_score - gap;
		SolrDocumentList ans = new SolrDocumentList();
		for(SolrDocument doc:qrs){
			float f = (float)doc.getFieldValue("score");
			if(f<entry_score)
				break;
			ans.add(doc);
			if(ans.size()>=20)
				break;
		}
		ans.setNumFound(ans.size());
		ans.setMaxScore(max_score);
		ans.setStart(0);
		return ans;
	}
	
	public boolean generateBaseCarId(String message, String pre_info, int standard) {
		if (message.isEmpty())
			return false;
		this.original_message = message;
		message = Utils.preProcess(message);
		if (solr == null)
			return false;
		String sub_query = parseMessage(solr, message, standard);
		fillYearToken();
		if(sub_query.length()<2)
			return false;
		if (pre_info != null)
			sub_query = pre_info + " " + sub_query;
		 
		query_results = Utils.select(sub_query, solr, years, standard);
		if (query_results == null) {
			return false;
		}
		//平行进口车使用的是search_level=low，所以需要进行截断,分数定在2000以内，即1999，为了规避浮点数带来的干扰
		if(standard==2){
			query_results = filterQueryResult(query_results);
		}
		solr.clear();
		return query_results!=null && query_results.size()>0;
	}

	/*
	 * 到solr的搜索结果中去匹配标准的颜色 source=0 外饰 source=1 内饰
	 */
	private String matchStandardColor(String color, int source) {
		String[] standard_colors = null;
		String tmp = null;
		if (source == 0) {
			tmp = query_results.get(0).get("outer_color").toString();
		} else {
			tmp = query_results.get(0).get("inner_color").toString();
		}
		standard_colors = tmp.split(",");
		int ans = 1000;
		if(color.endsWith("色") && color.length()>1){
			color = color.substring(0, color.length()-1);
		}
		ArrayList<Integer> candidates = new ArrayList<Integer>();
		for (int i = 0; i < standard_colors.length; i++) {
			int distance = Utils.getEditDistance(color, standard_colors[i]);
			if (distance < Math.max(color.length(), standard_colors[i].length())) {
				// 有一定的相关性
				if (ans == distance)
					candidates.add(i);
				else if (ans > distance) {
					ans = distance;
					candidates.clear();
					candidates.add(i);
				}
			}
		}
		return candidates.size() == 1 ? standard_colors[candidates.get(0)] : null;
	}

	private String unExpectedOuterColor(String color) {
		String[] standard_colors = null;
		String tmp = null;
		tmp = query_results.get(0).get("outer_color").toString();
		standard_colors = tmp.split(",");
		for (int i = 0; i < standard_colors.length; i++) {
			int distance = Utils.getEditDistance(color, standard_colors[i]);
			if (distance <= 1)
				return standard_colors[i];
		}
		return null;
	}

	private void extractColors(int mode, int phase){
		int idx = 0;
		int start_index = 0;
		for(int i=0;i<ele_arr.size() && i<15;i++){
			String s = ele_arr.get(i);
			String content = s.substring(s.lastIndexOf('|') + 1, s.indexOf('#'));
			if(content.length()>10)
				return;
			if(ele_arr.get(i).endsWith("#COLOR")){
				start_index = i;
				break;
			}
		}
		
		
		if(!colorBeforePrice){
			//if(mode!=-1){
				//start_index = vital_info_index;
			//}
			for (idx = start_index; idx < ele_arr.size() && idx<(start_index + 15); idx++) {
				String s = ele_arr.get(idx);
				String content = s.substring(s.lastIndexOf('|') + 1, s.indexOf('#'));
				if (s.endsWith("#COLOR")) {
					indexes.add(idx);
					colors.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
				} else if (mode==1 && (s.endsWith("STYLE") || s.endsWith("PRICE") || content.length()>10) && phase==1) {//手机号
					idx--;
					break;
				}else if (content.length()>10){
					idx--;
					break;
				}else if(mode==1 && (s.endsWith("STYLE") || s.endsWith("PRICE") ) && phase==2){
					continue;
				}else if(mode==-1){
					if(colors.size()!=0){
						idx--;
						break;
					}
				}
			}
			if (idx < ele_arr.size() && !colors.isEmpty()) {
				String temp = ele_arr.get(idx);
				backup_index = Math.max(NumberUtils.createInteger(temp.substring(temp.indexOf("-") + 1, temp.indexOf("|"))),
						backup_index);
			} else if(!colors.isEmpty()){
				String temp = ele_arr.get(idx-1);
				backup_index = Math.max(NumberUtils.createInteger(temp.substring(temp.indexOf("-") + 1, temp.indexOf("|"))),
						backup_index);
				//backup_index = Math.max(original_message.length(), backup_index);
			}
		}else{
			for (idx = 0; idx < vital_info_index; idx++) {
				String s = ele_arr.get(idx);
				if (s.endsWith("#COLOR")) {
					indexes.add(idx);
					colors.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
				} else if (!colors.isEmpty() && (s.endsWith("STYLE") || s.endsWith("PRICE"))) {
					idx--;
					break;
				}
			}
			if (idx < ele_arr.size()) {
				String temp = ele_arr.get(idx);
				backup_index = Math.max(NumberUtils.createInteger(temp.substring(temp.indexOf("-") + 1, temp.indexOf("|"))),
						backup_index);
			} else {
				backup_index = Math.max(original_message.length(), backup_index);
			}
		}
	}
	
	private void reExtractColors(){
		if(colors.size()==0)
			return;
		ArrayList<String>  ans_color = new ArrayList<String>();
		ArrayList<Integer> ans_index = new ArrayList<Integer>();
		for(int i=0;i<colors.size();i++){
			String c = colors.get(i);
			if(c.length()!=2){
				ans_color.add(c);
				ans_index.add(indexes.get(i));
			}else{
				int status = validDualColors(c);
				if (status <= 0) {
					if (status == 0) {
						String interpolation = unExpectedOuterColor(c);
						if (interpolation != null) {
							status = -1;
						}
					}
					if(status==-1){
						ans_color.add(c);
						ans_index.add(indexes.get(i));
					}
				}else{
					// 拆分颜色token
					// 长度肯定是2
					ans_color.add(c.substring(0,1));
					ans_index.add(indexes.get(i));
					ans_color.add(c.substring(1,2));
					ans_index.add(indexes.get(i));
				}
			}
		}
		colors = ans_color;
		indexes = ans_index;
	}
	
	// GREEDY METHOD
	// 先验假设：如果有外+内的颜色组合，那么外饰一定在内饰的前面
	public void newGenerateColors(int mode, int phase){
		extractColors(mode, phase);
		reExtractColors();
		Set<Integer> isUsedSet = new HashSet<Integer>();
		ArrayList<Integer> color_tag = new ArrayList<Integer>();
		if(colors.size()==0)
			return;
		boolean inner_flag = false;//上一个颜色是内饰，则last_inner=true，否则是false
		String last_outer_color = null;
		// greedy
		for(int i=0;i<colors.size();i++){
			String c = colors.get(i);
			if(inner_flag || i==0){
				// String outer_standard = matchStandardColor(c, 0);
				// result_colors.add(fetchValidColor(outer_standard, c) + "#");
				if((!newIsExplicitOuterColor(indexes.get(i), isUsedSet) && newIsExplicitInnerColor(indexes.get(i), isUsedSet)) || c.equals("黄鹤")){
					inner_flag = true;
					color_tag.add(1);
					continue;
				}
				last_outer_color = c;
				inner_flag = false;
				continue;
			}else{
				// 上一个颜色是外饰, 则这个颜色有可能是外饰，有可能是内饰
				int pre = i-1;
				int next = i+1;
				if(c.equals("黄鹤")){
					if(last_outer_color!=null){
						String outer_standard = matchStandardColor(last_outer_color, 0);
						result_colors.add(buildColorString(last_outer_color, c, outer_standard, c));
						last_outer_color = null;
						color_tag.add(0);
					}
					color_tag.add(1);
					inner_flag = true;
					continue;
				}
				if(last_outer_color!=null && c.equals(last_outer_color)){
					String outer_standard = matchStandardColor(last_outer_color, 0);
					String inner_standard = matchStandardColor(c, 1);
					result_colors.add(buildColorString(last_outer_color, c, outer_standard, inner_standard));
					last_outer_color = null;
					inner_flag = true;
					color_tag.add(0);
					color_tag.add(1);
					continue;
				}
				if(isAdjacentColor(indexes.get(pre), indexes.get(i)) || indexes.get(pre)==indexes.get(i)){//
					if(i>=2 && (isAdjacentColor(indexes.get(i-2), indexes.get(i-1)) || indexes.get(i-2)==indexes.get(i-1)) && color_tag.get(i-2)==0){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}else if(next>=colors.size() || (!isAdjacentColor(indexes.get(i), indexes.get(next)) && indexes.get(i)!=indexes.get(next) )){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							String inner_standard = matchStandardColor(c, 1);
							result_colors.add(buildColorString(last_outer_color, c, outer_standard, inner_standard));
							color_tag.add(0);
							last_outer_color = null;
						}
						inner_flag = true;
						color_tag.add(1);
					}else if(next>=indexes.size() || isAdjacentColor(indexes.get(i), indexes.get(next)) || indexes.get(i)==indexes.get(next)){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}
				}else{
					if(newIsExplicitOuterColor(indexes.get(i), isUsedSet)){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}else if(newIsExplicitInnerColor(indexes.get(i), isUsedSet)){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							String inner_standard = matchStandardColor(c, 1);
							result_colors.add(buildColorString(last_outer_color, c, outer_standard, inner_standard));
							last_outer_color = null;
							color_tag.add(0);
						}
						inner_flag = true;
						color_tag.add(1);
					}else{
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}
				}
			}
		}
		if(last_outer_color!=null){
			String outer_standard = matchStandardColor(last_outer_color, 0);
			result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
			color_tag.add(0);
		}
	}
	
	// OLDER METHOD
	public void generateColors(int mode, int phase) {
		extractColors(mode, phase);
		if(colors.size()==0)
			return;
		int mod = 0;// 默认是外和内分开
		if (colors.size() == 1) {// 黑白对应外+内，而米黄对应的只是外饰，摩卡对应的也是外饰
			String color = colors.get(0);
			if (color.length() != 2) {
				// 外饰
				String outer_standard = matchStandardColor(color, 0);
				result_colors.add(fetchValidColor(outer_standard, color) + "#");
			} else {
				// 只有外饰
				int status = validDualColors(color);
				if (status > -1) {
					if (status == 0) {
						String interpolation = unExpectedOuterColor(color);
						if (interpolation != null) {
							result_colors.add(interpolation + "#");
							return;
						}
					}
					String outer = color.substring(0, 1);
					String inner = color.substring(1, 2);
					String outer_standard = matchStandardColor(outer, 0);
					String inner_standard = matchStandardColor(inner, 1);
					result_colors.add(buildColorString(outer, inner, outer_standard, inner_standard));
				} else {
					String final_color = matchStandardColor(color, 0);
					if (final_color == null)
						result_colors.add(color + "#");
					else
						result_colors.add(final_color + "#");
				}
			}
		} else if (colors.size() == 2) {
			String outer = colors.get(0);
			String inner = colors.get(1);
			if ((isAdjacentColor(indexes.get(0), indexes.get(1)) && isValidInnerColor(inner)) || inner.equals("黄鹤")) {
				String outer_standard = matchStandardColor(outer, 0);
				String inner_standard = matchStandardColor(inner, 1);
				result_colors.add(buildColorString(outer, inner, outer_standard, inner_standard));
			} else {
				// 第二个颜色并不是内饰
				if(isExplicitOuterColor(indexes.get(0)) && isExplicitInnerColor(indexes.get(1))){
					String outer_standard = matchStandardColor(outer, 0);
					String inner_standard = matchStandardColor(inner, 1);
					result_colors.add(buildColorString(outer, inner, outer_standard, inner_standard));
				}else if (isExplicitOuterColor(indexes.get(1)) && isExplicitInnerColor(indexes.get(0))){
					String outer_standard = matchStandardColor(outer, 1);
					String inner_standard = matchStandardColor(inner, 0);
					result_colors.add(buildColorString(inner, outer, inner_standard, outer_standard));
				}else{
					String outer_standard = matchStandardColor(outer, 0);
					result_colors.add(fetchValidColor(outer_standard, outer) + "#");
					outer_standard = matchStandardColor(inner, 0);
					result_colors.add(fetchValidColor(outer_standard, inner) + "#");
				}
			}
		} else {
			mod = calcColorMode();
			if (mod == 0) {// 全是外色
				for (String outer : colors) {
					String outer_standard = matchStandardColor(outer, 0);
					result_colors.add(fetchValidColor(outer_standard, outer) + "#");
				}
			} else {
				for (int i = 0; i < colors.size();) {
					if (validDualColors(colors.get(i)) == 1) {
						String color = colors.get(i);
						String outer = color.substring(0, 1);
						String inner = color.substring(1, 2);
						String outer_standard = matchStandardColor(outer, 0);
						String inner_standard = matchStandardColor(inner, 1);
						result_colors.add(buildColorString(outer, inner, outer_standard, inner_standard));
						i += 1;
					} else {
						if (i + 1 == colors.size()) {
							String color = colors.get(i);
							String outer = color.substring(0, 1);
							String outer_standard = matchStandardColor(outer, 0);
							result_colors.add(fetchValidColor(outer_standard, outer) + "#");
							i++;
						} else {
							String outer = colors.get(i);
							String inner = colors.get(i + 1);
							if ((isAdjacentColor(indexes.get(i), indexes.get(i + 1)) && (isValidInnerColor(inner) || mode==-1)) || (inner.equals("黄鹤"))) {
								String outer_standard = matchStandardColor(outer, 0);
								String inner_standard = matchStandardColor(inner, 1);
								result_colors.add(buildColorString(outer, inner, outer_standard, inner_standard));
								i += 2;
							} else {
								String outer_standard = matchStandardColor(outer, 0);
								result_colors.add(fetchValidColor(outer_standard, outer) + "#");
								i += 1;
							}
						}
					}
				}
			}
		}
	}

	/*
	 * 在有多个颜色标签的情况下，我们默认颜色是内外色的情况是一致的 即，内容里不会有 外，内外 交错出现的情况 只会全是外色或者全是内外色
	 * 如果是0则全是外色 如果是1则都是外+内
	 */
	public int calcColorMode() {
		if (colors.size() != indexes.size())
			return -1;
		
		boolean outer_flag = true;
		for(int i=0;i<colors.size();i++){
			if(colors.get(i).length()!=1){
				outer_flag = false;
				break;
			}
		}
		if(outer_flag && colors.size()%2==1)
			return 0;
		outer_flag = false;
		for(int i=0;i<colors.size()-1;i++){
			String cur = colors.get(i);
			String next = colors.get(i + 1);
			if(cur.length()==1 && next.length()==1 && isAdjacentColor(indexes.get(i), indexes.get(i+1))){
				i+=1;
				continue;
			}else if(isAdjacentColor(indexes.get(i), indexes.get(i+1)) || next.equals("黄鹤")){
				i+=1;
				continue;
			}else
				outer_flag = true;
		}
		if(!outer_flag)//内+外
			return 1;
		
		// 内外都有
		for (int i = 0; i < indexes.size(); i++) {
			String c = colors.get(i);
			if (validDualColors(c) == 1) {
				return 1;
			}
		}

		for (int i = 0; i < indexes.size() - 1; i += 2) {
			String outer = colors.get(i);
			String inner = colors.get(i + 1);
			if (outer.length() > 2 && inner.length() > 2)
				return 0;
			if (outer.length() == 1 && inner.length() == 1 && isAdjacentColor(indexes.get(i), indexes.get(i + 1))
					&& isValidInnerColor(inner)) {
				return 1;// 内外都有
			}
		}

		return 0;// 都是外色
	}

	public boolean isExplicitOuterColor(int idx) {
		if (idx == 0)
			return false;
		String c = ele_arr.get(idx - 1);
		String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
		if (content.equals("外") || content.equals("外色") || content.equals("外饰")) {
			return true;
		}

		if (idx + 1 == ele_arr.size()) {
			return false;
		}
		c = ele_arr.get(idx + 1);
		content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
		if (content.equals("外") || content.equals("外色") || content.equals("外饰") || content.equals("车")) {
			return true;
		}
		return false;
	}
	
	public boolean newIsExplicitOuterColor(int idx, Set<Integer> usedSet) {
		if (idx == 0)
			return false;
		if(!usedSet.contains(idx-1)){
			String c = ele_arr.get(idx - 1);
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("外") || content.equals("外色") || content.equals("外饰")) {
				usedSet.add(idx - 1);
				return true;
			}
		}

		if (idx + 1 == ele_arr.size()) {
			return false;
		}
		if(!usedSet.contains(idx+1)){
			String c = ele_arr.get(idx + 1);
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("外") || content.equals("外色") || content.equals("外饰") || content.equals("车")) {
				usedSet.add(idx + 1);
				return true;
			}
		}
		return false;
	}

	public boolean newIsExplicitInnerColor(int idx, Set<Integer> usedSet) {
		if (idx + 1 == ele_arr.size()) {
			return false;
		}
		String c = ele_arr.get(idx + 1);
		if(!usedSet.contains(idx+1)){
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("内") || content.equals("内色") || content.equals("内饰")) {
				usedSet.add(idx+1);
				return true;
			}
		}

		if (idx == 0)
			return false;
		if(!usedSet.contains(idx-1)){
			c = ele_arr.get(idx - 1);
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("内") || content.equals("内色") || content.equals("内饰")) {
				usedSet.add(idx-1);
				return true;
			}
		}
		return false;
	}
	
	public boolean isExplicitInnerColor(int idx) {
		if (idx + 1 == ele_arr.size()) {
			return false;
		}
		String c = ele_arr.get(idx + 1);
		String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
		if (content.equals("内") || content.equals("内色") || content.equals("内饰")) {
			return true;
		}

		if (idx == 0)
			return false;
		c = ele_arr.get(idx - 1);
		content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
		if (content.equals("内") || content.equals("内色") || content.equals("内饰")) {
			return true;
		}
		return false;
	}

	public boolean isAdjacentColor(int idx1, int idx2) {
		String t1 = ele_arr.get(idx1);
		String t2 = ele_arr.get(idx2);

		t1 = t1.substring(t1.indexOf("-") + 1, t1.indexOf("|"));
		t2 = t2.substring(0, t2.indexOf("-"));
		if (t1.equals(t2))
			return true;

		int it1 = NumberUtils.toInt(t1);
		int it2 = NumberUtils.toInt(t2);
		if (it1 + 1 == it2) {
			char c = this.original_message.charAt(it1);
			if (c == '/') {
				return true;
			}
		}
		return false;
	}

	public String buildColorString(String outer, String inner, String outer_standard, String inner_standard) {
		return fetchValidColor(outer_standard, outer) + "#" + fetchValidColor(inner_standard, inner);
	}

	private String fetchValidColor(String standard_color, String color) {
		return standard_color == null ? color : standard_color;
	}

	// 判断单个颜色的token是不是包含外观和内饰
	private int validDualColors(String s) {
		if (s.length() != 2)
			return -1;
		String c1 = s.substring(0, 1);
		String c2 = s.substring(1, 2);
		if (c2.equals("色")) {
			// 后面的颜色并不是
			return -1;
		}
		if (c1.equals(c2))
			return 1;
		if(!base_colors_set.contains(c1))
			return -1;
		return base_colors_set.contains(c1) && base_colors_set.contains(c2) ? 1 : 0;
	}

	private boolean isValidInnerColor(String inner_color) {
		String[] standard_colors = null;
		String tmp = null;
		tmp = query_results.get(0).get("inner_color").toString();
		standard_colors = tmp.split(",");
		for (String s : standard_colors) {
			int distance = Utils.getEditDistance(inner_color, s);
			if (distance < Math.max(inner_color.length(), s.length())) {
				return true;
			}
		}
		return false;
	}

	private int getDiscountWay(int index) {
		String way = ele_arr.get(index);
		String content = way.substring(way.lastIndexOf("|") + 1, way.lastIndexOf("#"));
		if (content.equals("优惠") || content.equals("下") || content.equals("降")) {
			return -1;
		} else if (content.equals("加")) {
			return 1;
		}
		return 0;
	}

	private float calcPriceBias(int base_car_id, float real_price) {
		if (Utils.PRICE_GUIDE_25.containsKey(base_car_id)) {
			float price_25 = Utils.PRICE_GUIDE_25.get(base_car_id);
			float price_50 = Utils.PRICE_GUIDE_50.get(base_car_id);
			float price_75 = Utils.PRICE_GUIDE_75.get(base_car_id);
			return Math.min(calcSinglePriceBias(real_price, price_25),
					Math.min(calcSinglePriceBias(real_price, price_50), calcSinglePriceBias(real_price, price_75)));
		} else {
			float guiding_price = NumberUtils.createFloat(query_results.get(0).get("guiding_price_s").toString());
			return calcSinglePriceBias(real_price, guiding_price);
		}
	}

	private float calcSinglePriceBias(float expected_price, float guiding_price) {
		if (guiding_price == 0f) {
			return 1.0f;
		} else {
			return Utils.round(Math.abs(expected_price - guiding_price) / guiding_price, 2);
		}
	}

	/*
	 * 有的人的车直接写价格，而不是下点或者下万，如果找到某个地方是万，但是数字前面没有明确的下，优惠,加价等关键词，就judge
	 */
	private void judgeMarketingPriceByW(float coupon) {
		Object guiding_price = query_results.get(0).get("guiding_price_s");
		if (guiding_price == null) {
			discount_way = 4;
			discount_content = coupon;
			return;
		}
		int id = NumberUtils.createInteger(query_results.get(0).get("id").toString());
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price2 = price - coupon;
		float bias2 = calcPriceBias(id, price2);
		float price3 = coupon;
		float bias3 = calcPriceBias(id, price3);
		float price4 = price + coupon;
		float bias4 = calcPriceBias(id, price4);
		
		float mini = Math.min(Math.min(bias2, bias3), bias4);
		if(bias2==mini){
			discount_way = 2;
			discount_content = coupon;
		}else if (bias3 == mini) {
			discount_way = 4;
			discount_content = coupon;
		}else{
			discount_way = 3;
			discount_content = coupon;
		}
	}

	/*
	 * params sdc: string_discount_content  
	 */
	private void judgeMarketingPriceWithDiscount(float coupon, String sdc){
		Object guiding_price = query_results.get(0).get("guiding_price_s");
		if (guiding_price == null) {
			discount_way = 4;
			discount_content = coupon;
			return;
		}
		
		boolean eliminate_1 = false;
		if(sdc.contains(".")){
			String sub = sdc.substring(sdc.indexOf(".")+1);
			while(sub.endsWith("0")){
				sub = sub.substring(0,sub.length()-1);
			}
			if(!sub.isEmpty() && !sub.equals("5")){
				eliminate_1 = true;
			}
		}
		
		if(eliminate_1){
			discount_way = 2;
			discount_content = coupon;
			return;
		}
		int id = NumberUtils.createInteger(query_results.get(0).get("id").toString());
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price1 = Utils.round(price * (100 - coupon) / 100f, 2);
		float bias1 = calcPriceBias(id, price1);
		float price2 = price - coupon;
		float bias2 = calcPriceBias(id, price2);
		if(bias1<bias2){
			discount_way = 1;
			discount_content = coupon;
		}else{
			discount_way = 2;
			discount_content = coupon;
		}
	}
	
	/*
	 * params sdc: string_discount_content
	 */
	private void judgeMarketingPrice(float coupon, String sdc) {
		Object guiding_price = query_results.get(0).get("guiding_price_s");
		if (guiding_price == null) {
			discount_way = 4;
			discount_content = coupon;
			return;
		}
		
		boolean eliminate_1 = false;
		if(sdc.contains(".")){
			String sub = sdc.substring(sdc.indexOf(".")+1);
			while(sub.endsWith("0")){
				sub = sub.substring(0,sub.length()-1);
			}
			if(!sub.isEmpty() && !sub.equals("5")){
				eliminate_1 = true;
			}
		}
		
		int id = NumberUtils.createInteger(query_results.get(0).get("id").toString());
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price1 = Utils.round(price * (100 - coupon) / 100f, 2);
		float bias1 = calcPriceBias(id, price1);
		float price2 = price - coupon;
		float bias2 = calcPriceBias(id, price2);
		float price3 = coupon;
		float bias3 = calcPriceBias(id, price3);
		if(eliminate_1){
			if(bias2<=bias3){
				discount_way = 2;
				discount_content = coupon;
			}else{
				discount_way = 4;
				discount_content = coupon;
			}
		}else{
			// 下xx点
			if (bias1 < bias2 && bias1 < bias3) {
				discount_way = 1;
				discount_content = coupon;
			}
			// 下xx万
			if (bias2 < bias1 && bias2 < bias3) {
				discount_way = 2;
				discount_content = coupon;
			}
			// 直接报价
			if (bias3 < bias1 && bias3 < bias2) {
				discount_way = 4;
				discount_content = coupon;
			}
		}
	}
	
	private boolean isQuantOrBehave(int cur) {
		if (cur > 0) {
			String content = ele_arr.get(cur - 1);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			if (prefix_behave_set.contains(content))
				return true;
		}

		if (cur + 1 < ele_arr.size()) {
			String content = ele_arr.get(cur + 1);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			if (suffix_quants_set.contains(content))
				return true;
		}
		String element = ele_arr.get(cur);
		String head_str = element.substring(0,element.indexOf("-"));
		int head = NumberUtils.toInt(head_str);
		
		if(head>0 && cur>0){
			char c = original_message.charAt(head-1);
			String element2 = ele_arr.get(cur-1);
			if(element2.endsWith("COLOR") && (c=='*' || c=='x' || c=='X'))
				return true;
		}

		return false;
	}
	
	private boolean isYearToken(int cur) {
		if(cur==0){
			String content = ele_arr.get(cur);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			content = content.replaceAll("^(20)?\\d{2}(\\D|$)+", "");
			if(content.isEmpty())
				return true;
		}
		return false;
	}
	
	private void fillYearToken(){
		if(years==null || years.isEmpty()){
			for(int idx=0;idx<ele_arr.size();idx++){
				String content = ele_arr.get(idx);
				content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
				if(content.equals("新款") || content.equals("老款")){
					years.add(content);
					break;
				}
				content = content.replaceAll("^(20)?\\d{2}款", "");
				if(content.isEmpty()){
					String s = ele_arr.get(idx);
					years.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
					break;
				}
			}
		}
	}

	/*
	 * 提取平行进口车的成交价
	 */
	public boolean generarteParellelPrice() {
		discount_way = 5;
		discount_content = 0f;
		for (int i = vital_info_index; i < ele_arr.size(); i++) {
			String element = ele_arr.get(i);

			if (i - vital_info_index >= 15)// 已经在扫配置信息了，停止，不指望从配置信息里获取价格，价格索性就电议，然后填到备注里
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
				setParallelPrice(p);
				return true;
			}
			
			if (i > 0) {
				String tmp = ele_arr.get(i - 1);
				String kfc = tmp.substring(tmp.lastIndexOf("|") + 1, tmp.indexOf("#"));
				if ("特价".equals(kfc) || "现价".equals(kfc)) {
					setParallelPrice(p);
					return true;
				}
			}

			String head_str = element.substring(element.indexOf("-") + 1, element.indexOf("|"));
			int head = NumberUtils.toInt(head_str);
			if ((head >= 1 && this.original_message.charAt(head - 1) == '价')) {
				setParallelPrice(p);
				return true;
			}

			if ((i + 1) < ele_arr.size()) {
				String tmp = ele_arr.get(i + 1);
				String kfc = tmp.substring(tmp.lastIndexOf("|") + 1, tmp.indexOf("#"));
				if ("万".equals(kfc) || "w".equals(kfc)) {
					setParallelPrice(p);
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
					setParallelPrice(p);
					return true;
				}
			}
			if((i+1)==ele_arr.size() && p>15){
				setParallelPrice(p);
				return true;
			}
			String tail_str = element.substring(element.indexOf("-") + 1, element.indexOf("|"));
			int tail = NumberUtils.toInt(tail_str);
			if (tail < this.original_message.length()
					&& (this.original_message.charAt(tail) == '万' || this.original_message.charAt(tail) == 'w')) {
				setParallelPrice(p);
				return true;
			}
		}
		if(ele_arr.size()>=15){
			String token = ele_arr.get(ele_arr.size()-1);
			if(token.endsWith("PRICE")){
				String content = token.substring(token.lastIndexOf("|") + 1, token.indexOf("#"));
				float f = NumberUtils.toFloat(content, 0f);
				if(f!=0 && f<1000 && !content.matches("[0-9]{4,6}$")){
					setParallelPrice(f);
					return true;
				}
			}else{
				String tmp = token.substring(token.lastIndexOf("|") + 1, token.indexOf("#"));
				if(tmp.equals("万") || tmp.equals("w")){
					if(ele_arr.size()-2>=10){
						String token2 = ele_arr.get(ele_arr.size()-2);
						float f2 = NumberUtils.toFloat(token2.substring(token2.lastIndexOf("|") + 1, token2.indexOf("#")), 0f);
						if(f2!=0){
							setParallelPrice(f2);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void setParallelPrice(float f){
		if(f<20)
			return;
		discount_way = 4;
		discount_content = f;
	}
	/*
	 * 提取车架号
	 */
	public String extractVIN() {
		for (int i = vital_info_index; i < ele_arr.size(); i++) {
			String ele = ele_arr.get(i);
			String pre_ele = null;
			if (i > 0) {
				pre_ele = ele_arr.get(i - 1);
				if (pre_ele.contains("车架号")) {
					String content = ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#"));
					if (content.matches("[0-9]{4,6}$")) {
						String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
						int thehe = NumberUtils.toInt(hehe);
						backup_index = Math.max(backup_index, thehe);
						return content;
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
						backup_index = Math.max(backup_index, thehe);
						return content;
					}
					while(head>=1 && this.original_message.charAt(head - 1) == ' ')
						head--;
					if ((head >= 1 && this.original_message.charAt(head - 1) == '#')) {
						String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
						int thehe = NumberUtils.toInt(hehe);
						backup_index = Math.max(backup_index, thehe);
						return content;
					}
					
					while(tail< this.original_message.length() && this.original_message.charAt(tail)==' ')
						tail++;
					if ((tail < this.original_message.length() && this.original_message.charAt(tail) == '#')) {
						backup_index = Math.max(backup_index, tail + 1);
						return content;
					}
					
					String hehe = ele.substring(ele.indexOf("-") + 1, ele.indexOf("|"));
					int thehe = NumberUtils.toInt(hehe);
					backup_index = Math.max(backup_index, thehe);
					return content;
					
				}
			}
		}
		return null;
	}

	public boolean generateRealPrice() {
		ArrayList<Integer> status_arr = new ArrayList<>();
		ArrayList<Integer> way_arr = new ArrayList<>();
		ArrayList<Float> content_arr = new ArrayList<>();
		try {
			for (int i = vital_info_index; i < ele_arr.size(); i++) {
				String element = ele_arr.get(i);
				String fc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
				float cf = NumberUtils.toFloat(fc, 0.0f);
				if (element.endsWith("PRICE") || (cf != 0.0f && cf < 10000000)) {
					if (isQuantOrBehave(i))
						continue;
					float f = NumberUtils
							.createFloat(element.substring(element.lastIndexOf("|") + 1, element.indexOf("#")));
					int way = 0;
					if (i > 0) {
						way = getDiscountWay(i - 1);
					}
					// 找到实际价格
					if (way != 0) {
						if (f > 500) {// 例如下25000
							
							discount_way = way <= 0 ? 2 : 3;
							discount_content = f / 10000f;
							backup_index = Math.max(
									NumberUtils.createInteger(
											element.substring(element.indexOf("-") + 1, element.indexOf("|"))),
									backup_index);
							if (discount_way != 5 || discount_content != 0) {
								int status = validatePrice();
								if(status==1){
									backup_index = Math.max(NumberUtils.createInteger(
											element.substring(element.indexOf("-") + 1, element.indexOf("|"))), backup_index);
									return true;
								}else{
									status_arr.add(status);
									way_arr.add(discount_way);
									content_arr.add(discount_content);
								}
								discount_way = 5;
								discount_content = 0;
							}
							//return true;
						} else {// 万、点
							if (way == 1) {
								discount_way = 3;
								discount_content = f;// 加XX万
							} else if (ele_arr.size() - 1 > i) {
								String content = ele_arr.get(i + 1);
								content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
								if (content.equals("点")) {
									discount_way = 1;
									discount_content = f;
								} else if (content.equals("w") || content.equals("万")) {
									discount_way = 2;
									discount_content = f;
								} else if (content.equals("折") && (f > 0 && f < 100)) {
									discount_way = 1;
									if (f < 10)
										discount_content = 100 - f * 10;
									else
										discount_content = 100 - f;
								}else {
									String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
									// 不确定是下xx点还是下xx万，使用行情价判定
									judgeMarketingPriceWithDiscount(f, sdc);
								}
							} else {
								String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
								// 不确定是下xx点还是下xx万，使用行情价判定
								if(way==0)
									judgeMarketingPrice(f,sdc);
								else
									judgeMarketingPriceWithDiscount(f,sdc);
							}
						}
					} else {
						if (f > 500) {
							if(ele_arr.size() - 1 > i){
								String content = ele_arr.get(i + 1);
								content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
								if (content.equals("w") || content.equals("万")) {
									discount_way = 4;
									discount_content = f;
								}else{
									f = f / 10000;
									judgeMarketingPriceByW(f);
								}
							}else{
								f = f / 10000;
								judgeMarketingPriceByW(f);
							}
						} else {
							if (ele_arr.size() - 1 > i) {
								String content = ele_arr.get(i + 1);
								content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
								if (content.equals("点")) {
									discount_way = 1;
									discount_content = f;
								} else if (content.equals("w") || content.equals("万")) {
									//discount_way = 2;
									judgeMarketingPriceByW(f);
									discount_content = f;
								} else if (content.equals("折") && (f > 0 && f < 100)) {
									discount_way = 1;
									if (f < 10)
										discount_content = 100 - f * 10;
									else
										discount_content = 100 - f;
								} else {
									// 不确定是下xx点还是下xx万，使用行情价判定
									String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
									judgeMarketingPrice(f, sdc);
								}
							} else {
								String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
								judgeMarketingPrice(f, sdc);
							}
						}
					}
				}
				if (discount_way != 5 || discount_content != 0) {
					int status = validatePrice();
					if(status==1){
						backup_index = Math.max(NumberUtils.createInteger(
								element.substring(element.indexOf("-") + 1, element.indexOf("|"))), backup_index);
						return true;
					}else{
						status_arr.add(status);
						way_arr.add(discount_way);
						content_arr.add(discount_content);
					}
					discount_way = 5;
		            discount_content = 0;
				}
			}
			
			// 从所有可能的价格里挑一个？？？还是不挑？？
			// 如果分数低于5001，表明这条资源的置信度不算很高，那么如果价格也没有，就索性不添加？还是说加上价格，让鹰眼把它过滤掉?
			// 考虑到，鹰眼可能无法过滤掉，那么就滚蛋？还是电议？
			// 挑偏差最小的
			float max_score = query_results.getMaxScore();
			
			// 没有价格，资源分数也低，那就不发, 否则，电议
			if(way_arr.isEmpty())
	            return max_score>5000?true:false;
			
			discount_way = way_arr.get(0);
			discount_content = content_arr.get(0);
			int flag = status_arr.get(0);
			
			// 选了个合适的价格，是不是最接近不好说，因为这里没有一个数据记录价格的偏差
			for(int i=1;i<way_arr.size();i++){
				if(status_arr.get(i)>flag){
					discount_way = way_arr.get(i);
					discount_content = content_arr.get(i);
					flag = status_arr.get(i);
				}
			}
			
			if(max_score<5010){
				return false;
			}else{
				if(flag==-1){
					discount_way = 5;
					discount_content = 0;
				}
				return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public int validatePrice(){
		String base_car_id = query_results.get(0).get("id").toString();
        float guiding_price = NumberUtils.toFloat(query_results.get(0).get("guiding_price_s").toString());
        float real_price = 0f;
        
        switch (discount_way) {
		case 1://下点
			real_price = Utils.round(guiding_price * (100 - discount_content) / 100f, 2);
			break;
		case 2://下万
			real_price = guiding_price - discount_content;
			break;
		case 3://加万
			real_price = guiding_price + discount_content;
			break;
		case 4://直接报价
			real_price = discount_content;
			break;
		default:
			break;
		}
        return PriceValidationClassifier.predict(base_car_id, real_price, guiding_price);
	}

	public String concatWithSpace(String s) {
		for (int i = s.length(); i < 50; i++) {
			s = s + " ";
		}
		return s;
	}

	public void addToResponse(ArrayList<String> res_base_car_ids, ArrayList<String> res_colors,
			ArrayList<String> res_discount_way, ArrayList<String> res_discount_content, ArrayList<String> res_remark) {
		if (query_results.size() == 0) {
			try {
				res_base_car_ids.add("");
				res_colors.add("");
				res_discount_way.add("");
				res_discount_content.add("");
				res_remark.add("");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		String brand_name = query_results.get(0).get("brand_name").toString();
		this.cur_brand = brand_name;
		String car_model_name = query_results.get(0).get("car_model_name").toString();
		this.cur_model = car_model_name;
		String base_car_id = query_results.get(0).get("id").toString();

		try {
			res_base_car_ids.add(base_car_id);
			res_colors.add(result_colors.toString());
			res_discount_way.add(Integer.toString(discount_way));
			res_discount_content.add(Float.toString(discount_content));
			res_remark.add(
					Utils.removeRemarkIllegalHeader(postProcessRemark(this.original_message.substring(backup_index))));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 生成redis中的key
	 */
	private String generateKey(String user_id, String mes) {
		return user_id + "_" + mes;
	}

	public void addToResponseWithCache(String user_id, String reserve_s, ArrayList<String> res_base_car_ids,
			ArrayList<String> res_colors, ArrayList<String> res_discount_way, ArrayList<String> res_discount_content,
			ArrayList<String> res_remark, CarResourceGroup carResourceGroup, int mode, String VIN, String resource_type,
			boolean disableCache) {
		if (query_results.size() == 0) {
			try {
				res_base_car_ids.add("");
				res_colors.add("");
				res_discount_way.add("");
				res_discount_content.add("");
				res_remark.add("");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		String brand_name = query_results.get(0).get("brand_name").toString();
		this.cur_brand = brand_name;
		String car_model_name = query_results.get(0).get("car_model_name").toString();
		this.cur_model = car_model_name;
		String base_car_id = query_results.get(0).get("id").toString();
		int year = NumberUtils.toInt(query_results.get(0).get("year").toString());
		String style_name = query_results.get(0).get("base_car_style").toString();
		String standard_name = query_results.get(0).get("standard_name").toString();
		String guiding_price = query_results.get(0).get("guiding_price_s").toString();
		
		try {
			CarResource cr = new CarResource(base_car_id, result_colors.toString(), Integer.toString(discount_way),
					Float.toString(discount_content),
					Utils.removeRemarkIllegalHeader(postProcessRemark(this.original_message.substring(backup_index))),
					brand_name, car_model_name, mode, VIN, year, style_name, standard_name, resource_type, guiding_price, query_results);
			if (carResourceGroup == null)
				carResourceGroup = new CarResourceGroup();
			carResourceGroup.getResult().add(cr);
			if (!disableCache && NiuniuBatchConfig.getEnableCache())
				CacheManager.set(generateKey(user_id, reserve_s), JSON.toJSONString(cr).toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 后处理备注信息
	 */
	private String postProcessRemark(String remark) {
		if (remark == null)
			return "";
		remark = remark.trim();
		if (remark.length() == 0)
			return "";

		char c = remark.charAt(0);
		if (c == ',' || c == ')' || c == ']' || c == '}')
			return remark.substring(1).trim();

		if (remark.startsWith("万") || remark.startsWith("点") || remark.startsWith("元") || remark.startsWith("w")
				|| remark.startsWith("折")) {
			if (remark.length() == 1) {
				return "";
			} else {
				remark = remark.substring(1).trim();
			}
		}
		return remark.trim();
	}

	public void printParsingResult(BufferedWriter writer) {
		if (query_results.size() == 0) {
			try {
				if (writer != null) {
					writer.newLine();
					writer.write(concatWithSpace(original_message));
					writer.flush();
				} else {
					System.out.println(concatWithSpace(original_message));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		String brand_name = query_results.get(0).get("brand_name").toString();
		this.cur_brand = brand_name;
		String car_model_name = query_results.get(0).get("car_model_name").toString();
		this.cur_model = car_model_name;
		String base_car_style = query_results.get(0).get("base_car_style").toString();
		String guiding_price = query_results.get(0).get("guiding_price_s").toString();
		String base_car_id = query_results.get(0).get("id").toString();
		String result = base_car_id + "\t" + brand_name + "\t" + car_model_name + "\t" + base_car_style + "\t"
				+ guiding_price + "\t" + result_colors.toString() + "\t" + discount_way + "\t" + discount_content + "\t"
				+ Utils.removeRemarkIllegalHeader(postProcessRemark(this.original_message.substring(backup_index)));
		try {
			if (writer != null) {
				writer.newLine();
				writer.write(concatWithSpace(original_message) + "\t\t" + result);
				writer.flush();
			} else {
				log.info("[batch_processor]\t {} \t\t {}",concatWithSpace(original_message), result);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 是头上的[新朗逸] 还是正儿八经的征文
	 */
	public boolean isHeader() {
		if(original_message.contains("系列"))
			return true;
		if (prices.size() == 0 && original_message.trim().length() < 8)
			return true;
		if (prices.size() == 1 && original_message.trim().length() < 8
				&& ( (prices.get(0).equals("301") || prices.get(0).equals("308") || prices.get(0).equals("408")
						|| prices.get(0).equals("2008") || prices.get(0).equals("3008") || prices.get(0).equals("4008")
						|| prices.get(0).equals("5008") || prices.get(0).equals("508") ) && brands.isEmpty() ) ){
			models.add(prices.get(0));
			prices.clear();
			return true;
		}
		return false;
	}

	/*
	 * 更多汽车资源请咨询长沙易驰 只要你户价格能解决的问题不是问题✔省内接车
	 * 铃木大放价，不放过一个客户，拿下为主、来车放。留钱就出了，看着留看着给，不在挣钱，在出车啊！[胜利][胜利][胜利][胜利][胜利]
	 */
	public boolean isInvalidMessage() {
		// model, brand, price有2个以上是空的
		if ((models.isEmpty() && brands.isEmpty()) || (models.isEmpty() && prices.isEmpty())
				|| (prices.isEmpty() && brands.isEmpty()))
			return true;
		return false;
	}
}
