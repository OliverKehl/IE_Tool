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
import com.niuniu.classifier.TokenTagClassifier;
import com.niuniu.config.NiuniuBatchConfig;

public class BaseCarFinder {

	public final static Logger log = LoggerFactory.getLogger(BaseCarFinder.class);

	public ArrayList<String> models;
	public ArrayList<String> prices;
	public ArrayList<String> brands;
	public ArrayList<String> styles;
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

	String[] suffix_quants = { "台", "轮", "度", "速", "天", "分钟", "小时", "秒", "辆", "年", "月", "寸", "月底", "号", "项"};
	String[] prefix_behave = { "送" };

	Set<String> suffix_quants_set;
	Set<String> prefix_behave_set;
	
	boolean colorBeforePrice = false;

	Set<String> specialDigitalToken = null;
	String[] specialDigits = {"1500","1794","2500","2700","3000","3004","3500","4000","4500","4600","5000","5700","5802","6004","6104","7004","7504"};
	
	// 指导价
	// 考虑色全的情况

	public BaseCarFinder() {
		models = new ArrayList<String>();
		prices = new ArrayList<String>();
		brands = new ArrayList<String>();
		styles = new ArrayList<String>();
		colors = new ArrayList<String>();
		indexes = new ArrayList<Integer>();
		ele_arr = new ArrayList<String>();
		base_colors_set = new HashSet<String>();
		for (String s : base_colors) {
			base_colors_set.add(s);
		}
		result_colors = new HashSet<String>();
		solr = new USolr("http://121.40.204.159:8080/solr/");

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

	public BaseCarFinder(USolr solr) {
		models = new ArrayList<String>();
		prices = new ArrayList<String>();
		brands = new ArrayList<String>();
		styles = new ArrayList<String>();
		colors = new ArrayList<String>();
		indexes = new ArrayList<Integer>();
		ele_arr = new ArrayList<String>();
		base_colors_set = new HashSet<String>();
		for (String s : base_colors) {
			base_colors_set.add(s);
		}
		result_colors = new HashSet<String>();
		this.solr = solr;

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

	public BaseCarFinder(USolr solr, String pre_brand_name) {
		models = new ArrayList<String>();
		prices = new ArrayList<String>();
		brands = new ArrayList<String>();
		styles = new ArrayList<String>();
		colors = new ArrayList<String>();
		indexes = new ArrayList<Integer>();
		ele_arr = new ArrayList<String>();
		base_colors_set = new HashSet<String>();
		for (String s : base_colors) {
			base_colors_set.add(s);
		}
		result_colors = new HashSet<String>();
		this.solr = solr;
		this.pre_brand_name = pre_brand_name;

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

	/*
	 * 从search库里获取多个候选base_car_id
	 */
	private void fillBaseCarIds(Map<Integer, Float> result, SolrDocumentList docs) {
		for (SolrDocument doc : docs) {// 遍历结果集
			long base_car_id = (long) doc.get("id");
			float score = (float) doc.get("score");
			result.put((int) base_car_id, score);
		}
	}

	/*
	 * 根据分数等信息，评估候选base_car_id
	 */
	public boolean assessBaseCarCandidates(Map<Integer, Float> result) {
		Iterator<Entry<Integer, Float>> entries = result.entrySet().iterator();
		float max_score = 0;
		while (entries.hasNext()) {
			Entry<Integer, Float> entry = (Entry<Integer, Float>) entries.next();
			// int key = entry.getKey();
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
		for (; i < tokens.size(); i++) {
			String s = tokens.get(i);
			int start = NumberUtils.toInt(s.substring(0, s.indexOf('-')));
			if (start > 0 && isFrontVinWithSpace(start, message, standard)) {
				return i;
			}
			backup_index = Math.max(start,
					backup_index);
			if (s.endsWith("#OTHERS") || s.endsWith("#COLOR") || s.endsWith("AREA")) {
				
				backup_index = Math.max(NumberUtils.createInteger(s.substring(0, s.indexOf('-'))),
						backup_index);
				
				if(standard!=2 && s.endsWith("COLOR") && !price_status){
					colorBeforePrice = true;
					continue;
				}
				if (s.contains("指导价")) {
					tokens.remove(i);
					continue;
				}
				if(standard==2 && s.endsWith("#COLOR"))
					continue;
				return i;
				// stop = NumberUtils.createInteger(s.substring(0,
				// s.indexOf("-")));
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
				String real_tag = judgeRealTag(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
				if (real_tag != null) {
					if (real_tag.equals("MODEL")) {
						models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						continue;
					} else if (real_tag.equals("STYLE")) {
						styles.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
						continue;
					}
				}else{
					/* 
					 * 在不确定一个term是model还是style时，把它优先归为model
					 * 因为这里的归类只是作为辅助功能，并不会对搜索的精度造成影响  
					 */
					models.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
		            continue;
				}
			} else if (s.endsWith("#STYLE_PRICE") || s.endsWith("#FPRICE") || s.endsWith("MODEL_PRICE")
					|| s.endsWith("MODEL_STYLE_PRICE")) {
				//TODO
				if(isQuantOrBehave(i) || priceSuffix(i)){
					return i;
				}
				if (price_status){
					String content = s.substring(s.lastIndexOf("|") + 1, s.indexOf("#"));
					int head = NumberUtils.toInt(s.substring(0, s.indexOf("-")));
					int tail = NumberUtils.toInt(s.substring(s.indexOf("-") + 1, s.indexOf("|")));
					if((content.startsWith("0")&& content.length()==4) || isFrontVinWithSpace(head, message, standard) || isBehindVinWithSpace(tail, message, standard))
						return i;
					return Math.min(i + 1, tokens.size());
				}
				
				//扫到指导价
				if(colorBeforePrice && standard!=2){
					prices.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
					price_status = price_status | isStandardPrice(s);
					return Math.min(i + 1, tokens.size());
				}
				
				String real_tag = judgeRealTag(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
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
					if(standard==1 || hehe.length()<4 || (standard==2 && specialDigitalToken.contains(hehe))){
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
		if (pre_info != null)
			sub_query = pre_info + " " + sub_query;
		query_results = Utils.select(sub_query, solr);
		//if (query_results == null || (sub_query.length()<3 && brands.isEmpty() && models.isEmpty() && styles.isEmpty())) {
		if (query_results == null) {
			// 1. 空行
			// 2. 把颜色放指导价前面了
			// TODO
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
		Map<Integer, Float> base_car_info = new HashMap<Integer, Float>();
		if (solr == null)
			return false;
		String sub_query = parseMessage(solr, message, standard);
		if (pre_info != null)
			sub_query = pre_info + " " + sub_query;
		 
		query_results = Utils.select(sub_query, solr, standard);
		if (query_results == null) {
			// 1. 空行
			// 2. 把颜色放指导价前面了
			// TODO
			return false;
		}
		//平行进口车使用的是search_level=low，所以需要进行截断,分数定在2000以内，即1999，为了规避浮点数带来的干扰
		if(standard==2){
			query_results = filterQueryResult(query_results);
		}
		fillBaseCarIds(base_car_info, this.query_results);
		solr.clear();
		return base_car_info.size() > 0;
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

	/*
	 * 从简处理，如果colors.size()=2就是外+内，如果=1，细分一下看看是不是能区分成 后续还可以这么做： 一行里面可能会写 黑黑 棕黑
	 * 如果拆分成两个词后两个词相同，那么就是外+内 金黑 黑黑 => 黑#COLOR 黑#COLOR 棕黑 => 棕黑#COLOR 金黑 =>
	 * 金黑#COLOR 那么就可以结合上下文的颜色情况来确定这里的棕黑和金黑是不是也要拆分
	 * 
	 */
	public void generateColors(int mode) {
		int idx = 0;
		int start_index = 0;
		if(!colorBeforePrice){
			if(mode!=-1){
				start_index = vital_info_index;
			}
			for (idx = start_index; idx < ele_arr.size(); idx++) {
				String s = ele_arr.get(idx);
				if (s.endsWith("#COLOR")) {
					indexes.add(idx);
					colors.add(s.substring(s.lastIndexOf("|") + 1, s.indexOf("#")));
				} else if (mode==1 && (s.endsWith("STYLE") || s.endsWith("PRICE"))) {
					idx--;
					break;
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
				backup_index = Math.max(original_message.length(), backup_index);
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
			if (isAdjacentColor(indexes.get(0), indexes.get(1)) && isValidInnerColor(inner)) {
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
			// TODO 这里定好一个基调，要不就是全都是外观，要不就是全都是外观+内饰
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
							if (isAdjacentColor(indexes.get(i), indexes.get(i + 1)) && isValidInnerColor(inner)) {
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
		outer_flag = true;
		for(int i=0;i<colors.size()-1;i++){
			String cur = colors.get(i);
			String next = colors.get(i + 1);
			if(cur.length()==1 && next.length()==1 && isAdjacentColor(indexes.get(i), indexes.get(i+1)))
				continue;
			else
				outer_flag = false;
		}
		if(outer_flag)//全是外饰
			return 0;
		
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

	// TODO
	// 判断单个颜色的token是不是包含外观和内饰
	private int validDualColors(String s) {
		if (s.length() != 2)
			return -1;
		String c1 = s.substring(0, 1);
		String c2 = s.substring(1, 2);
		if (!isValidInnerColor(c2) || c2.equals("色")) {
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
	 * 有的人的车直接写价格，而不是下点或者下万，如果找到某个地方是万，但是数字前面没有明确的下，优惠等关键词，就judge
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
		// 下xx万
		if (bias2 < bias3) {
			discount_way = 2;
			discount_content = coupon;
		}
		// 直接报价
		if (bias3 < bias2) {
			discount_way = 4;
			discount_content = coupon;
		}
	}

	private void judgeMarketingPriceWithDiscount(float coupon){
		Object guiding_price = query_results.get(0).get("guiding_price_s");
		if (guiding_price == null) {
			discount_way = 4;
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
	
	private void judgeMarketingPrice(float coupon) {
		Object guiding_price = query_results.get(0).get("guiding_price_s");
		if (guiding_price == null) {
			discount_way = 4;
			discount_content = coupon;
			return;
		}
		int id = NumberUtils.createInteger(query_results.get(0).get("id").toString());
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price1 = Utils.round(price * (100 - coupon) / 100f, 2);
		float bias1 = calcPriceBias(id, price1);
		float price2 = price - coupon;
		float bias2 = calcPriceBias(id, price2);
		float price3 = coupon;
		float bias3 = calcPriceBias(id, price3);
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

		return false;
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
			if (ele.endsWith("PRICE")) {
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
		try {
			for (int i = vital_info_index; i < ele_arr.size(); i++) {
				String element = ele_arr.get(i);
				String fc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
				float cf = NumberUtils.toFloat(fc, 0.0f);
				if (element.endsWith("PRICE") || (cf != 0.0f && cf < 10000000)) {
					if (isQuantOrBehave(i))
						break;
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
							return true;
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
								} else {
									// 不确定是下xx点还是下xx万，使用行情价判定
									judgeMarketingPriceWithDiscount(f);
								}
							} else {
								// 不确定是下xx点还是下xx万，使用行情价判定
								judgeMarketingPrice(f);
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
									judgeMarketingPrice(f);
								}
							} else {
								judgeMarketingPrice(f);
							}
						}
					}
				}
				if (discount_way != 5 || discount_content != 0) {
					backup_index = Math.max(NumberUtils.createInteger(
							element.substring(element.indexOf("-") + 1, element.indexOf("|"))), backup_index);
					break;
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
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
					brand_name, car_model_name, mode, VIN, year, style_name, standard_name, resource_type, guiding_price);
			if (carResourceGroup == null)
				carResourceGroup = new CarResourceGroup();
			carResourceGroup.getResult().add(cr);
			if (!disableCache && NiuniuBatchConfig.getEnableCache())
				CacheManager.set(generateKey(user_id, reserve_s), JSON.toJSONString(cr).toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
				/*
				if (remark.charAt(1) == ' ') {
					return remark.substring(2);
				}
				*/
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
