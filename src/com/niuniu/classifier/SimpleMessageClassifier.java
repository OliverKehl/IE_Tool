package com.niuniu.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;

import com.niuniu.USolr;
import com.niuniu.Utils;

public class SimpleMessageClassifier {
	USolr solr;
	// 这里需要baseCarFinder对整个被tokenize后的字符串进行一次深度的解析
	// TODO

	String message;
	ArrayList<String> tokens;

	ArrayList<String> brands;
	ArrayList<String> models;
	ArrayList<String> standards;
	ArrayList<String> latent_prices;
	ArrayList<String> real_prices;
	ArrayList<String> unknown_prices;
	ArrayList<String> fake_prices;
	String[] suffix_quants = { "台", "轮", "度", "速", "天", "分钟", "小时", "秒", "辆", "年", "月" };
	String[] prefix_behave = { "送" };

	Set<String> suffix_quants_set;
	Set<String> prefix_behave_set;

	String[] parallel_tokens = {"公羊", "公羊1500", "2700","3000","4000","4500","4600","5700","1794","塞纳","坦途","gle43",};
	Set<String> parallelToken;
	
	public SimpleMessageClassifier() {
	}

	public SimpleMessageClassifier(String message, USolr solr) {
		brands = new ArrayList<String>();
		models = new ArrayList<String>();
		standards = new ArrayList<String>();
		latent_prices = new ArrayList<String>();
		real_prices = new ArrayList<String>();
		fake_prices = new ArrayList<String>();
		unknown_prices = new ArrayList<String>();

		suffix_quants_set = new HashSet<String>();
		prefix_behave_set = new HashSet<String>();

		for (String s : suffix_quants)
			suffix_quants_set.add(s);

		for (String s : prefix_behave)
			prefix_behave_set.add(s);

		this.message = message;
		this.solr = solr;
		
		parallelToken = new HashSet<String>();
		for(String s:parallel_tokens){
			parallelToken.add(s);
		}
	}

	public boolean isYear(String s) {
		if (s.equals("15") || s.equals("16") || s.equals("17") || s.equals("2015") || s.equals("2016")
				|| s.equals("2017"))
			return true;
		return false;
	}

	/*
	 * 1. 量词，比如说2台，3天 2. 和价格无关的动词前缀，送1000配置
	 */
	public boolean isQuantOrBehave(int cur) {
		if (cur > 0) {
			String content = tokens.get(cur - 1);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			if (prefix_behave_set.contains(content))
				return true;
		}

		if (cur + 1 < tokens.size()) {
			String content = tokens.get(cur + 1);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			if (suffix_quants_set.contains(content))
				return true;
		}

		return false;
	}

	// 判断它是不是指导价
	public int isRealPrice(int cur) {
		if (cur == 0) {
			String content = tokens.get(cur);
			content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
			if (isYear(content))
				return 2;
			if (content.length() >= 3)
				return 1;
			return 0;
		}
		String pre_ele = tokens.get(cur - 1);
		String pre_val = pre_ele.substring(pre_ele.lastIndexOf("|") + 1, pre_ele.indexOf("#"));
		if (pre_val.equals("下") || pre_val.equals("优惠") || pre_val.equals("加") || pre_val.equals("降")) {
			return -1;
		}
		// 不是指导价
		return 0;
	}

	public boolean isGuidingPrice(float f) {
		int i = (int) f;
		if (i < 1000 && i % 10 != 0)
			return true;
		if (i > 1000 && i % 100 != 0)
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
		} 
		return real_tag;
	}
	
	public void parse() {
		for (int i = 0; i < tokens.size(); i++) {
			String ele = tokens.get(i);
			if (ele.endsWith("PRICE")) {
				if (isYear(ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#")))) {
					fake_prices.add(ele);
					continue;
				}
				
				String real_tag = judgeRealTag(ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#")));
				if (real_tag != null) {
					if (real_tag.equals("MODEL")) {
						models.add(ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#")));
					}
					continue;
				}
				
				if (i == 0) {
					float f = NumberUtils.toFloat(ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#")), 0);
					if (f > 10000) {
						real_prices.add(ele);
						continue;
					}
				}
				
				int idx = NumberUtils.toInt(ele.substring(0, ele.indexOf('-')));
				if(idx>0 && message.charAt(idx-1)=='#')
					continue;
				
				
				int c = isRealPrice(i);
				if (c == 1 && !ele.contains("MODEL")) {
					real_prices.add(ele);
					continue;
				} else if (c == -1) {
					fake_prices.add(ele);
					continue;
				}

				if (isQuantOrBehave(i))
					continue;
			}

			if (ele.endsWith("STANDARD")) {
				standards.add(ele);
			} else if (ele.endsWith("BRAND")) {
				brands.add(ele);
			} else if (ele.endsWith("#MODEL")) {
				models.add(ele);
			} else if (ele.contains("MODEL")) {
				models.add(ele);
				if (ele.endsWith("PRICE")) {
					unknown_prices.add(ele);
				}
			} else if (ele.endsWith("PRICE")) {
				String cf = ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#"));
				float f = NumberUtils.toFloat(cf, 0);

				if (f > 100 && f < 10000 && isGuidingPrice(f)) {
					real_prices.add(ele);
					continue;
				}

				if (f > 0 && f < 400 && cf.indexOf(".") > 0 && (cf.length() - cf.indexOf(".")) > 2) {

					String suffix = null;
					String suf = null;
					if (i < tokens.size() - 1) {
						suffix = tokens.get(i + 1);
						suf = suffix.substring(suffix.lastIndexOf("|") + 1, suffix.indexOf("#"));
						;
					}
					if (suf == null || !(suf.equals("点") || suf.equals("w") || suf.equals("万"))) {
						real_prices.add(ele);
						continue;
					} else if (suf != null && (suf.equals("点") || suf.equals("w") || suf.equals("万"))) {
						// 有后缀信息的一般都不是指导价
						fake_prices.add(ele);
						continue;
					}
				}

				if (f > 500000) {// 一般降价猛的车，直接会报价而不是下500000,所以大于500000的可以认为是
					real_prices.add(ele);
					continue;
				}

				if (i > 0) {
					int c = isRealPrice(i);
					if (c == 1) {
						real_prices.add(ele);
					} else if (c == -1) {
						fake_prices.add(ele);
					} else if (c == 0) {
						latent_prices.add(ele);
					} else {
						// 年款 置之不理
					}
				} else {// 第一个token就都是数字，有可能是model,style和price
					if (ele.endsWith("FPRICE"))
						real_prices.add(ele);
					else
						latent_prices.add(ele);
				}
			}
		}
	}

	public void prepare() {
		if (solr == null)
			return;
		// message =
		// Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message),
		// solr)));
		tokens = Utils.tokenize(message, solr, "message");
		parse();
	}

	public int isValidLine() {
		for(int i=0;i<tokens.size();i++){
			String tmp = tokens.get(i);
			tmp = tmp.substring(tmp.lastIndexOf("|") + 1, tmp.indexOf("#"));
			if(tmp.equals("车架号") || parallelToken.contains(tmp))
				return -1;
		}
		if(!standards.isEmpty()){
			String cur_standard = standards.get(0);
			cur_standard = cur_standard.substring(cur_standard.lastIndexOf("|") + 1, cur_standard.indexOf("#"));
			if (cur_standard.equals("中东") || cur_standard.equals("加版") || cur_standard.equals("欧版")
					|| cur_standard.equals("美规") || cur_standard.equals("墨西哥版")) {
				return -1;
			}
		}
		if (real_prices.size() > 1 || fake_prices.size() > 2)
			return 0;
		if (latent_prices.size() >= 3)
			return 0;
		if (standards.isEmpty())
			return 1;
		return 1;
	}

	public int predict() {
		prepare();
		return isValidLine();
	}

	public static void main(String[] args) {
		USolr solr = new USolr("http://121.40.204.159:8080/solr/");
		String message = "17款道奇公羊";
		message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
		SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
		int mode = simpleMessageClassifier.predict();
		System.out.println(mode);
		if (mode == 0)
			System.out.println("不合格");
		else
			System.out.println("合格");
	}

	public static void ttmain(String[] args) {
		File file = new File(
				"/Users/kehl/Documents/workspace/MessageProcessor/src/com/niuniu/resource/indicator/test_case");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			USolr solr_client = new USolr("http://115.29.240.213:8983/solr/");
			while ((line = reader.readLine()) != null) {
				//Thread.sleep(1000);
				
				String[] arrs = line.split("\\\\n");
				for(String s:arrs){
					SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(s, solr_client);
					int mode = simpleMessageClassifier.predict();
					if (mode==0) {
						System.out.println("不符合规范" + "\t" + s);
						continue;
					}
				}
				//solr_client.close();
				/*
				 * resourceMessageProcessor.process(); System.out.println(
				 * "##########################################################################################"
				 * ); System.out.println(
				 * "##########################################################################################"
				 * ); System.out.println(
				 * "##########################################################################################"
				 * ); System.out.println(
				 * "##########################################################################################"
				 * ); System.out.println(
				 * "##########################################################################################"
				 * );
				 */
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
