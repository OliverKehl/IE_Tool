package com.niuniu;

import java.io.BufferedWriter;
import java.util.ArrayList;
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

import com.niuniu.classifier.TokenTagClassifier;

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
	public ArrayList<Integer> getIndexes() {
		return indexes;
	}

	public void setIndexes(ArrayList<Integer> indexes) {
		this.indexes = indexes;
	}

	public ArrayList<String> getEle_arr() {
		return ele_arr;
	}

	public void setEle_arr(ArrayList<String> ele_arr) {
		this.ele_arr = ele_arr;
	}

	public SolrDocumentList getQuery_results() {
		return query_results;
	}

	public void setQuery_results(SolrDocumentList query_results) {
		this.query_results = query_results;
	}

	public int getDiscount_way() {
		return discount_way;
	}

	public void setDiscount_way(int discount_way) {
		this.discount_way = discount_way;
	}

	public float getDiscount_content() {
		return discount_content;
	}

	public void setDiscount_content(float discount_content) {
		this.discount_content = discount_content;
	}

	public String getOriginal_message() {
		return original_message;
	}

	public void setOriginal_message(String original_message) {
		this.original_message = original_message;
	}

	int vital_info_index;
	public int getVital_info_index() {
		return vital_info_index;
	}

	public void setVital_info_index(int vital_info_index) {
		this.vital_info_index = vital_info_index;
	}

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	int discount_way = 5;
	float discount_content = 0f;
	Set<String> base_colors_set;
	String[] base_colors = { "黑", "白", "红", "灰", "棕", "银", "金", "蓝", "紫", "米" };
	Set<String> result_colors;
	String result_remark;
	int level = 2;
	
	public String getResult_remark() {
		return result_remark;
	}

	public void setResult_remark(String result_remark) {
		this.result_remark = result_remark;
	}

	String resource_type;
	public String getResource_type() {
		return resource_type;
	}

	public void setResource_type(String resource_type) {
		this.resource_type = resource_type;
	}

	public Set<String> getResult_colors() {
		return result_colors;
	}

	public void setResult_colors(Set<String> result_colors) {
		this.result_colors = result_colors;
	}

	String vin;
	String pre_brand_name;

	String cur_brand;
	String cur_model;
	
	String original_message;

	USolr solr;

	int backup_index = 0;
	
	public int getBackup_index() {
		return backup_index;
	}

	public void setBackup_index(int backup_index) {
		this.backup_index = backup_index;
	}

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
		result_remark = "";
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
						if(f_hehe==i_hehe && (s.endsWith("MODEL_STYLE_PRICE") || s.endsWith("MODEL_PRICE")) ){
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
		query_results = filterQueryResult(query_results, 1);
		solr.clear();
		return query_results!=null && query_results.size()>0;
	}

	SolrDocumentList filterQueryResult(SolrDocumentList qrs, int standard){
		float gap = 1999;
		if(standard==1)
			gap = 0;
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
		query_results = filterQueryResult(query_results, 2);
		solr.clear();
		return query_results!=null && query_results.size()>0;
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
	
	public void parseColors(int mode, int phase){
		extractColors(mode, phase);
		reExtractColors();
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

	public boolean isQuantOrBehave(int cur) {
		return Utils.isQuantOrBehaveToken(ele_arr, cur, original_message);
	}
	
	private boolean isYearToken(int cur) {
		if(cur==0){
			String content = ele_arr.get(cur);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			return Utils.isYearToken(content);
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
