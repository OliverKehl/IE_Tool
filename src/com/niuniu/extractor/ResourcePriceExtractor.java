package com.niuniu.extractor;

import java.util.ArrayList;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.niuniu.BaseCarFinder;
import com.niuniu.Utils;
import com.niuniu.classifier.PriceValidationClassifier;

public class ResourcePriceExtractor {
	
	private class PriceUnit{
		int discount_way;
		float discount_content;
		boolean status;
		
		public PriceUnit(boolean status){
			this.discount_way = 5;
			this.discount_content = 0;
			this.status = status;
		}
		
		public PriceUnit(int discount_way, float discount_content){
			this.discount_way = discount_way;
			this.discount_content = discount_content;
			status = true;
		}
	}
	
	private float calcRealPrice(PriceUnit pu, float guiding_price){
		float real_price = 0.0f;
		switch (pu.discount_way) {
		case 1://下点
			real_price = Utils.round(guiding_price * (100 - pu.discount_content) / 100f, 2);
			break;
		case 2://下万
			real_price = guiding_price - pu.discount_content;
			break;
		case 3://加万
			real_price = guiding_price + pu.discount_content;
			break;
		case 4://直接报价
			real_price = pu.discount_content;
			break;
		default:
			break;
		}
		return real_price;
	}
	
	ArrayList<Integer> indexes;
	ArrayList<String> ele_arr;
	SolrDocumentList query_results;
	BaseCarFinder baseCarFinder;
	ArrayList<PriceUnit> priceUnits;
	
	public ResourcePriceExtractor(BaseCarFinder baseCarFinder){
		this.baseCarFinder = baseCarFinder;
		this.query_results = baseCarFinder.getQuery_results();
		priceUnits = new ArrayList<PriceUnit>();
		this.ele_arr = baseCarFinder.getEle_arr();
		this.indexes = baseCarFinder.getIndexes();
	}
	
	//相同车型的base_car是不是要剔除？
	private void cutOffQueryResult(){
		SolrDocumentList cutoff_results = new SolrDocumentList();
		int step = 0;
		float threshold = 0;
		for ( SolrDocument doc: query_results ) {
			if(step==0){
				threshold = NumberUtils.toFloat(doc.getFieldValue("score").toString());
				step++;
				cutoff_results.add(doc);
				continue;
			}else{
				float tmp_score = NumberUtils.toFloat(doc.getFieldValue("score").toString());
				if(tmp_score<threshold){
					break;
				}
				cutoff_results.add(doc);
			}
		}
		this.query_results = cutoff_results;
		this.query_results.setMaxScore(baseCarFinder.getQuery_results().getMaxScore());
	}
	
	/*
	 * 生成中规、国产的实际价格
	 */
	public void extract(){
		/*
		 * 1.截断检索结果
		 * 2.为每个base_car_id生成候选价格列表，这里不同的base_car_id要求车型不同
		 * 3.选取最match的
		 */
		cutOffQueryResult();
		for ( SolrDocument doc: query_results ) {
			PriceUnit pu = generateRealPrice(doc);
			priceUnits.add(pu);
		}
		// 选取最match的
		float bias = Float.MAX_VALUE;
		int ans_idx = 0;
		for(int i=0;i<query_results.size();i++){
			float real_price = calcRealPrice(priceUnits.get(i), NumberUtils.toFloat(query_results.get(i).get("guiding_price_s").toString()));
			float tmp_bias = calcPriceBias(real_price, query_results.get(i));
			if(tmp_bias<bias){
				bias = tmp_bias;
				ans_idx = i;
			}
		}
		SolrDocument tmpSolrDoc = query_results.get(ans_idx);
		baseCarFinder.getQuery_results().clear();
		baseCarFinder.getQuery_results().add(tmpSolrDoc);
		if(priceUnits.get(ans_idx).status){
			baseCarFinder.setDiscount_way(priceUnits.get(ans_idx).discount_way);
			baseCarFinder.setDiscount_content(priceUnits.get(ans_idx).discount_content);
		}else{
			baseCarFinder.setDiscount_way(5);
			baseCarFinder.setDiscount_content(0f);
		}
	}
	
	private PriceUnit processImplicitPrice(int way, String element, float f_price, int ele_idx, SolrDocument solrDoc){
		int discount_way = 0;
		float discount_content = 0.0f;
		
		if (f_price > 500) {
			if(ele_arr.size() - 1 > ele_idx){
				String content = ele_arr.get(ele_idx + 1);
				content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
				if (content.equals("w") || content.equals("万")) {
					discount_way = 4;
					discount_content = f_price;
				}else{
					f_price = f_price / 10000;
					discount_way = judgeMarketingPriceByW(f_price, solrDoc);
					discount_content = f_price;
				}
			}else{
				f_price = f_price / 10000;
				discount_way = judgeMarketingPriceByW(f_price, solrDoc);
				discount_content = f_price;
			}
		} else {
			if (ele_arr.size() - 1 > ele_idx) {
				String content = ele_arr.get(ele_idx + 1);
				content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
				if (content.equals("点")) {
					discount_way = 1;
					discount_content = f_price;
				} else if (content.equals("w") || content.equals("万")) {
					//discount_way = 2;
					discount_way = judgeMarketingPriceByW(f_price, solrDoc);
					discount_content = f_price;
				} else if (content.equals("折") && (f_price > 0 && f_price < 100)) {
					discount_way = 1;
					if (f_price < 10)
						discount_content = 100 - f_price * 10;
					else
						discount_content = 100 - f_price;
				} else {
					// 不确定是下xx点还是下xx万，使用行情价判定
					String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
					discount_way = judgeMarketingPrice(f_price, sdc, solrDoc);
					discount_content = f_price;
				}
			} else {
				String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
				discount_way = judgeMarketingPrice(f_price, sdc, solrDoc);
				discount_content = f_price;
			}
		}
		return new PriceUnit(discount_way, discount_content); 
	}
	
	private PriceUnit processExplicitPrice(int way, String element, float f_price, int ele_idx, SolrDocument solrDoc){
		int discount_way = 0;
		float discount_content = 0.0f;
		if (f_price > 500) {// 例如下25000
			
			discount_way = way <= 0 ? 2 : 3;
			discount_content = f_price / 10000f;
			baseCarFinder.setBackup_index(
					Math.max(
							NumberUtils.createInteger(element.substring(element.indexOf("-") + 1, element.indexOf("|"))),
							baseCarFinder.getBackup_index()
							)
					);
			if (discount_way != 5 || discount_content != 0) {
				int status = validatePrice(discount_way, discount_content, solrDoc);
				if(status==1){
					baseCarFinder.setBackup_index(
							Math.max(
									NumberUtils.createInteger(element.substring(element.indexOf("-") + 1, element.indexOf("|"))),
									baseCarFinder.getBackup_index()
									)
							);
					return new PriceUnit(discount_way, discount_content);
				}
				discount_way = 5;
				discount_content = 0;
			}
			//return true;
		} else {// 万、点
			if (way == 1) {
				discount_way = 3;
				discount_content = f_price;// 加XX万
			} else if (ele_arr.size() - 1 > ele_idx) {
				String content = ele_arr.get(ele_idx + 1);
				content = content.substring(content.lastIndexOf("|") + 1, content.indexOf("#"));
				if (content.equals("点")) {
					discount_way = 1;
					discount_content = f_price;
				} else if (content.equals("w") || content.equals("万")) {
					discount_way = 2;
					discount_content = f_price;
				} else if (content.equals("折") && (f_price > 0 && f_price < 100)) {
					discount_way = 1;
					if (f_price < 10)
						discount_content = 100 - f_price * 10;
					else
						discount_content = 100 - f_price;
				}else {
					String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
					// 不确定是下xx点还是下xx万，使用行情价判定
					discount_way = judgeMarketingPriceWithDiscount(f_price, sdc, solrDoc);
					discount_content = f_price;
				}
			} else {
				String sdc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
				// 不确定是下xx点还是下xx万，使用行情价判定
				if(way==0)
					discount_way = judgeMarketingPrice(f_price,sdc, solrDoc);
				else
					discount_way = judgeMarketingPriceWithDiscount(f_price,sdc, solrDoc);
				discount_content = f_price;
			}
		}
		return new PriceUnit(discount_way, discount_content);
	}
	
	//TODO
	private boolean isQuantOrBehave(int idx){
		return Utils.isQuantOrBehaveToken(ele_arr, idx, baseCarFinder.getOriginal_message());
	}
	
	private PriceUnit generateSinglePrice(String element, int ele_idx, SolrDocument solrDoc){
		String fc = element.substring(element.lastIndexOf("|") + 1, element.indexOf("#"));
		float cf = NumberUtils.toFloat(fc, 0.0f);
		PriceUnit pu = null;
		if (element.endsWith("PRICE") || (cf != 0.0f && cf < 10000000)) {
			if (isQuantOrBehave(ele_idx))
				return null;
			float f = NumberUtils
					.createFloat(element.substring(element.lastIndexOf("|") + 1, element.indexOf("#")));
			int way = 0;
			if (ele_idx > 0) {
				way = getDiscountWay(ele_idx - 1);
			}
			// 找到实际价格
			
			if (way != 0) {
				pu = processExplicitPrice(way, element, f, ele_idx, solrDoc);
			} else {
				pu = processImplicitPrice(way, element, f, ele_idx, solrDoc);
			}
		}
		return pu;
	}
	
	public PriceUnit generateRealPrice(SolrDocument doc) {
		ArrayList<Integer> status_arr = new ArrayList<>();
		ArrayList<PriceUnit> localPriceUnits = new ArrayList<PriceUnit>();
		try {
			for (int i = baseCarFinder.getVital_info_index(); i < ele_arr.size(); i++) {
				String element = ele_arr.get(i);
				PriceUnit pu = generateSinglePrice(element,i, doc);
				if (pu!=null && (pu.discount_way != 5 || pu.discount_content != 0)) {
					int status = validatePrice(pu.discount_way, pu.discount_content, doc);
					if(status==1){
						baseCarFinder.setBackup_index(
								Math.max(
										NumberUtils.createInteger(element.substring(element.indexOf("-") + 1, element.indexOf("|"))),
										baseCarFinder.getBackup_index()
								)
						);
						return pu;
					}else{
						//status_arr.add(status);
						//localPriceUnits.add(pu);
					}
				}
			}
			
			// 从所有可能的价格里挑一个？？？还是不挑？？
			// 如果分数低于5001，表明这条资源的置信度不算很高，那么如果价格也没有，就索性不添加？还是说加上价格，让鹰眼把它过滤掉?
			// 考虑到，鹰眼可能无法过滤掉，那么就滚蛋？还是电议？
			// 挑偏差最小的
			float max_score = query_results.getMaxScore();
			
			// 没有价格，资源分数也低，那就不发, 否则，电议
			if(localPriceUnits.isEmpty())
	            return max_score>5000?new PriceUnit(true):new PriceUnit(false);
			
			PriceUnit ans = localPriceUnits.get(0);
			int flag = status_arr.get(0);
			
			// 选了个合适的价格，是不是最接近不好说，因为这里没有一个数据记录价格的偏差
			for(int i=1;i<localPriceUnits.size();i++){
				if(status_arr.get(i)>flag){
					ans = localPriceUnits.get(i);
					flag = status_arr.get(i);
				}
			}
			
			if(max_score<5010){
				ans.status = false;
				return ans;
			}else{
				if(flag==-1){
					return new PriceUnit(true);
				}
				return ans;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new PriceUnit(false);
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

	private float calcPriceBias(float real_price, SolrDocument solrDoc) {
		int base_car_id = NumberUtils.toInt(solrDoc.get("id").toString());
		if (Utils.PRICE_GUIDE_25.containsKey(base_car_id)) {
			float price_25 = Utils.PRICE_GUIDE_25.get(base_car_id);
			float price_50 = Utils.PRICE_GUIDE_50.get(base_car_id);
			float price_75 = Utils.PRICE_GUIDE_75.get(base_car_id);
			return Math.min(calcSinglePriceBias(real_price, price_25),
					Math.min(calcSinglePriceBias(real_price, price_50), calcSinglePriceBias(real_price, price_75)));
		} else {
			float guiding_price = NumberUtils.createFloat(solrDoc.get("guiding_price_s").toString());
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
	private int judgeMarketingPriceByW(float coupon, SolrDocument solrDoc) {
		Object guiding_price = solrDoc.get("guiding_price_s");
		if (guiding_price == null) {
			return 4;
		}
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price2 = price - coupon;
		float bias2 = calcPriceBias(price2,solrDoc);
		float price3 = coupon;
		float bias3 = calcPriceBias(price3, solrDoc);
		float price4 = price + coupon;
		float bias4 = calcPriceBias(price4, solrDoc);
		
		float mini = Math.min(Math.min(bias2, bias3), bias4);
		if(bias2==mini){
			return 2;
		}else if (bias3 == mini) {
			return 4;
		}else{
			return 3;
		}
	}

	/*
	 * params sdc: string_discount_content  
	 */
	private int judgeMarketingPriceWithDiscount(float coupon, String sdc, SolrDocument solrDoc){
		Object guiding_price = solrDoc.get("guiding_price_s");
		if (guiding_price == null) {
			return 4;
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
			return 2;
		}
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price1 = Utils.round(price * (100 - coupon) / 100f, 2);
		float bias1 = calcPriceBias(price1, solrDoc);
		float price2 = price - coupon;
		float bias2 = calcPriceBias(price2, solrDoc);
		if(bias1<bias2){
			return 1;
		}else{
			return 2;
		}
	}
	
	/*
	 * params sdc: string_discount_content
	 */
	private int judgeMarketingPrice(float coupon, String sdc, SolrDocument solrDoc) {
		Object guiding_price = solrDoc.get("guiding_price_s");
		if (guiding_price == null) {
			return 4;
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
		
		float price = NumberUtils.createFloat(guiding_price.toString());
		float price1 = Utils.round(price * (100 - coupon) / 100f, 2);
		float bias1 = calcPriceBias(price1, solrDoc);
		float price2 = price - coupon;
		float bias2 = calcPriceBias(price2, solrDoc);
		float price3 = coupon;
		float bias3 = calcPriceBias(price3, solrDoc);
		if(eliminate_1){
			if(bias2<=bias3){
				return 2;
			}else{
				return 4;
			}
		}else{
			// 下xx点
			if (bias1 < bias2 && bias1 < bias3) {
				return 1;
			}
			// 下xx万
			if (bias2 < bias1 && bias2 < bias3) {
				return 2;
			}
			// 直接报价
			if (bias3 < bias1 && bias3 < bias2) {
				return 4;
			}
		}
		return 4;
	}
	
	public int validatePrice(int discount_way, float discount_content, SolrDocument solrDoc){
        float guiding_price = NumberUtils.toFloat(solrDoc.get("guiding_price_s").toString());
        float real_price = calcRealPrice(new PriceUnit(discount_way, discount_content), guiding_price);
        return PriceValidationClassifier.predict(solrDoc.get("id").toString(), real_price, guiding_price);
	}
}
