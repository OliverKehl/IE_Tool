package com.niuniu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;


public class Utils {
	
	public static Map<Integer, Float> PRICE_GUIDE_25 = new HashMap<Integer, Float>();
	public static Map<Integer, Float> PRICE_GUIDE_50 = new HashMap<Integer, Float>();
	public static Map<Integer, Float> PRICE_GUIDE_75 = new HashMap<Integer, Float>();
	
	private static final String regEx;
	private static Pattern specialCharPattern;
	
	static{
		regEx = "[-`·~!@#$%^&*()+=|{}':;',//[//]<>/?~！@#￥%……&*（）—+|{}【】‘；：”“’。，、？_]";
		specialCharPattern = Pattern.compile(regEx);
	}
	
	private static String replace(String line) {
		char[] c = line.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 12288) {
				c[i] = (char) 32;
				continue;
			}
			if (c[i] > 65280 && c[i] < 65375)
				c[i] = (char) (c[i] - 65248);
		}
		return new String(c);
	}
	
	private static char[] signs = { '+', '-', '!', ':', '?', '*', '~', '^', '"', '\\', '(', ')', '[', ']', '{', '}',
	' ' };
	
	private static boolean needToConvert(char c) {
		for (int i = 0; i < signs.length; i++) {
			if (signs[i] == c)
				return true;
		}
		return false;
	}
	
	/*
	 * 繁体=>简体
	 * 全角=>半角
	 */
	public static String normalize(String query) {
		query = replace(query.trim());

		String regex = " +";
		query = query.replaceAll(regex, " ").trim();
		char[] a = query.toCharArray();
		char[] b = new char[500];
		int i = 0;
		for (char c : a) {
			if (!needToConvert(c)) {
				b[i++] = c;
			} else {
				//b[i++] = '\\';
				b[i++] = c;
			}
		}
		String c = new String(b, 0, i);
		return c;
	}
	
	static{
		File file = new File("/Users/kehl/Documents/workspace/MessageProcessor/src/com/niuniu/resource/indicator/base_car_price_reference");
        BufferedReader reader = null; 
		try{
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.replaceAll(",", "\t");
				String[] arrs = line.split("\t");
				PRICE_GUIDE_25.put(NumberUtils.createInteger(arrs[0]), NumberUtils.createFloat(arrs[1]));
				PRICE_GUIDE_50.put(NumberUtils.createInteger(arrs[0]), NumberUtils.createFloat(arrs[2]));
				PRICE_GUIDE_75.put(NumberUtils.createInteger(arrs[0]), NumberUtils.createFloat(arrs[3]));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static int Minimum(int a, int b, int c) {
		int im = a < b ? a : b;
		return im < c ? im : c;
	}

	public static int getEditDistance(String s, String t) {
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1
		n = s.length();
		m = t.length();
		if (n == 0) {
			return m;
		}
		if (m == 0) {
			return n;
		}
		d = new int[n + 1][m + 1];

		// Step 2
		for (i = 0; i <= n; i++) {
			d[i][0] = i;
		}
		for (j = 0; j <= m; j++) {
			d[0][j] = j;
		}

		// Step 3
		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);
			// Step 4
			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);
				// Step 5
				cost = (s_i == t_j) ? 0 : 1;
				// Step 6
				d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
			}
		}
		// Step 7
		return d[n][m];
	}
	
	public static String escapeFromChineseEmoji(String s){
		return "";
	}
	
	public static SolrDocumentList select(String query, USolr solr){
		if(solr==null || query==null || query.isEmpty())
			return null;
		SolrDocumentList query_results = null;
		solr.clear();
		solr.selectIndex("niuniu_basecars");
		solr.setFields("*", "score");
		solr.setStart(0);
		solr.setRows(100);
		solr.setDefType("niuniuparser");
		solr.setQuery(query);
		//TODO
		//需要把后续的所有tag是STYLE的内容加入到搜索条件中
		solr.addSortField("score", false);// 按年份降序排列
		solr.addSortField("year", false);// 按年份降序排列
		try {
			solr.ExecuteQuery();
			query_results = solr.getQueryResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query_results;
	}
	
	public static ArrayList<String> tokenize(String s, USolr solr, String mode){
		if(solr==null){
			return null;
		}
		ArrayList<String> ele_arr = new ArrayList<String>();
		solr.clear();
		solr.selectIndex("niuniu_basecars");
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("qt", "/tokenize");
		params.set("start", "0");
		params.set("rows", "10");
		params.set("defType", "niuniuparser");
		params.set("q", s);
		//params.set("fl", "id,score");
		params.set("field", mode);
		params.set("detail", true);
		try {
			solr.ExecuteQuery(params);
			String response = solr.queryresponse.toString();
			String parse_result = response.substring(response.lastIndexOf("result") + 7, response.length() - 2).trim();
			String[] elements = parse_result.split(" ");

			for (int i = 0; i < elements.length; i++) {
				ele_arr.add(elements[i]);
			}
			Collections.sort(ele_arr);
			Collections.sort(ele_arr, new Comparator<String>() {
				public int compare(String o1, String o2) {
					int idx1 = NumberUtils.createInteger(o1.substring(0, o1.indexOf("-")));
					int idx2 = NumberUtils.createInteger(o2.substring(0, o2.indexOf("-")));
					if (idx1 - idx2 > 0)
						return 1;
					if (idx1 - idx2 == 0)
						return 0;
					return -1;
				}
			});
		}catch(Exception e){
			e.printStackTrace();
		}
		return ele_arr;
	}

	/*
	 * 把各种降价、加价的符号变成文本，并且去掉其中的emoji符号
	 */
	public static String preProcess(String message) {
		if(message==null)
			return null;
		message = message.replaceAll("⬇️", " 下 ");
		message = message.replaceAll("↓️️", " 下 ");
		message = message.replaceAll("⬇", " 下 ");
		message = message.replaceAll("↓", " 下 ");
		message = message.replaceAll("↓", " 下 ");
		message = message.replaceAll("➕", " 加 ");
		//message = message.replaceAll("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]", "");
		message = message.replaceAll("[\ud83c\uec00-\ud83c\udfff]|[\ud83d\uec00-\ud83d\udfff]|[\u2600-\u27ff]", " ");
		return message.trim();
	}
	/*
	 * 去掉每行头部的起始标识，比如1. 1、 1 
	 */
	public static String removeHeader(String str){
		System.out.println(str);
		String eL = "^\\d[.\\s、]";
		Pattern p = Pattern.compile(eL);
		Matcher m = p.matcher(str);
		String line = m.replaceAll(" ").trim();
		return line.trim();
	}
	
	/*
	 * 单行文本清洗
	 * 去掉[色]、【太阳】等由于表情符号带来的干扰
	 * 全角转半角
	 */
	public static String clean(String source, USolr solr){
		if(source==null)
			return null;
		String tmp = removeHeader(preProcess(source));
		String result = tmp;
		ArrayList<String> res = tokenize(tmp, solr,"filter_word");
		String ele = res.get(0);
		if(ele.endsWith("OTHERS")){
			String a = ele.substring(0, ele.indexOf("-"));
			String b = ele.substring(ele.indexOf("-")+1, ele.indexOf("|"));
			int start = NumberUtils.toInt(a)-1;
			int end = NumberUtils.toInt(b);
			if(start>=0 && end<tmp.length()){
				if((tmp.charAt(start)=='[' || tmp.charAt(start)=='【') && (tmp.charAt(end)==']' || tmp.charAt(end)=='】')){
					result = tmp.substring(0, start) + tmp.substring(end+1);
				}else{
					return result;
				}
			}else{
				return tmp.trim();
			}
		}else{
			return tmp.trim();
		}
		return clean(result, solr);
	}
	
	public static String cleanDate(String str){
		String eL = "^[0-9]{1,2}[月-][0-9]{2}号?";
		Pattern p = Pattern.compile(eL);
		Matcher m = p.matcher(str);
		String line = m.replaceAll("").trim();
		return line.trim();
	}
	
	public static String normalizePrice(String str){
		String eL = "(?<=[^\\d])\\s+\\-(?=\\d+)(?=\\.?)(?=\\d+)";
		Pattern p = Pattern.compile(eL);
		Matcher m = p.matcher(str);
		String line = m.replaceAll(" 下").trim();
		return line.trim();
	}
	
	public static void main(String[] args){
		USolr solr = new USolr("http://121.40.204.159:8080/solr/");
		String line = "1、视金钱如粪土的)";
		//System.out.println(clean(line, solr));
		
	}
	
}
