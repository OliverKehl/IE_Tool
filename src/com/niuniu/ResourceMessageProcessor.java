package com.niuniu;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.niuniu.cache.CacheManager;
import com.niuniu.classifier.MessageStandardClassifier;
import com.niuniu.classifier.ParallelResourcePriceClassifier;
import com.niuniu.classifier.ParallelResourceVinClassifier;
import com.niuniu.classifier.ResourceTypeClassifier;
import com.niuniu.classifier.SimpleMessageClassifier;
import com.niuniu.config.NiuniuBatchConfig;

public class ResourceMessageProcessor {
	public CarResourceGroup getCarResourceGroup() {
		return carResourceGroup;
	}

	public final static Logger log = LoggerFactory.getLogger(ResourceMessageProcessor.class);
	String last_brand_name;
	String last_model_name;
	String last_style_name;
	int last_standard_name;
	
	String[] message_arr;
	String messages;
	
	BufferedWriter writer;
	
	USolr solr_client;
	String user_id;
	
	ArrayList<String> res_base_car_ids;
	ArrayList<String> res_colors;
	ArrayList<String> res_discount_way;
	ArrayList<String> res_discount_content;
	ArrayList<String> res_remark;
	
	String year_regex = "^(20)?\\d{2}(\\D|$)+";
	Pattern yearPattern = null;
	
	boolean disableCache = false;
	
	public void setDisableCache(boolean disableCache) {
		this.disableCache = disableCache;
	}

	CarResourceGroup carResourceGroup = new CarResourceGroup();
	
	public ResourceMessageProcessor(){
		init();
		//solr_client = new USolr("http://121.40.204.159:8080/solr/");
		solr_client = new USolr(NiuniuBatchConfig.getSolrHost());
	}
	
	public ResourceMessageProcessor(String host){
		init();
		//solr_client = new USolr("http://121.40.204.159:8080/solr/");
		solr_client = new USolr(host);
	}
	
	public ResourceMessageProcessor(USolr solr_client){
		init();
		this.solr_client = solr_client;
	}
	
	public ResourceMessageProcessor(BufferedWriter writer){
		init();
		this.writer = writer;
		//solr_client = new USolr("http://121.40.204.159:8080/solr/");
		solr_client = new USolr(NiuniuBatchConfig.getSolrHost());
	}
	
	public void init(){
		this.messages = "";
		last_brand_name = "";
		last_model_name = "";
		last_style_name = "";
		last_standard_name = 0;
		res_base_car_ids = new ArrayList<String>();
		res_colors = new ArrayList<String>();
		res_discount_way = new ArrayList<String>();
		res_discount_content = new ArrayList<String>();
		res_remark = new ArrayList<String>();
		yearPattern = Pattern.compile(this.year_regex);
	}
	
	public void resetParallelResourceBasedOnPrice(CarResource cr){
		//int standard = cr.getStandard();
		//if(standard!=2)
			//return;
		SolrDocumentList qrs = cr.getQuery_result();
		if(qrs==null)
			return;
		String target_base_car_id = null;
		float gap = Float.MAX_VALUE;
		float score = NumberUtils.toFloat(cr.getQuery_result().get(0).getFieldValue("score").toString());
		float real_price = NumberUtils.toFloat(cr.getReal_price());
		int step = 0;
		SolrDocument target_doc = null;
		for ( SolrDocument doc: qrs ) {
			String base_car_id = doc.get("id").toString();
			float median_price = 0.0f;
			if(Utils.PRICE_GUIDE_50.containsKey(NumberUtils.toInt(base_car_id)))
				median_price = Utils.PRICE_GUIDE_50.get(NumberUtils.toInt(base_car_id));
			if(median_price==0.0 && Utils.PARALLEL_PRICE_GUIDE.containsKey(NumberUtils.toInt(base_car_id)))
				median_price = Utils.PARALLEL_PRICE_GUIDE.get(NumberUtils.toInt(base_car_id));
			if(step==0){
				if(median_price==0.0){
					return;
				}
				gap = Utils.round(Math.abs(real_price - median_price), 3);
				target_base_car_id = base_car_id;
				target_doc = doc;
				step++;
				continue;
			}
			step++;
			if(median_price==0.0){
				continue;
			}
			
			float tmp_score = NumberUtils.toFloat(doc.getFieldValue("score").toString());
			if(tmp_score<score)
				break;
			float t_gap = Utils.round(Math.abs(real_price - median_price), 3);
			if(t_gap>=gap)
				continue;
			gap = t_gap;
			target_base_car_id = base_car_id;
			target_doc = doc;
		}
		if(target_base_car_id!=null && !target_base_car_id.equals(cr.getId())){
			//重新构建结果
			String brand_name = target_doc.get("brand_name").toString();
			String car_model_name = target_doc.get("car_model_name").toString();
			String base_car_id = target_doc.get("id").toString();
			int year = NumberUtils.toInt(target_doc.get("year").toString());
			String style_name = target_doc.get("base_car_style").toString();
			String standard_name = target_doc.get("standard_name").toString();
			cr.setId(base_car_id);
			cr.setBrand_name(brand_name);
			cr.setCar_model_name(car_model_name);
			cr.setStyle_name(style_name);
			cr.setYear(year);
			cr.setStandard_name(standard_name);
			cr.setQuery_result(null);
		}
	}
	
	public void postProcess(){
		if(carResourceGroup==null)
			carResourceGroup = new CarResourceGroup();
		for(CarResource cr : carResourceGroup.result){
			if(cr.getDiscount_way()==null || cr.getDiscount_way().equals("0") || cr.getDiscount_way().equals("5")){
				cr.setQuery_result(null);
				continue;
			}
			if(cr.getDiscount_way().equals("4")){
				cr.setReal_price(cr.getDiscount_content());
			}else{
				String guiding_price = cr.getGuiding_price();
				if(guiding_price!=null && !guiding_price.equals("0.0")){
					float price = NumberUtils.createFloat(guiding_price);
					float coupon = NumberUtils.toFloat(cr.getDiscount_content());
					if(cr.getDiscount_way().equals("1")){
						cr.setReal_price(Float.toString(Utils.round(price * (100 - coupon) / 100f, 2)));
					}else if(cr.getDiscount_way().equals("2")){
						cr.setReal_price(Float.toString(Utils.round(price - coupon, 2)));
					}else if(cr.getDiscount_way().equals("3")){
						cr.setReal_price(Float.toString(Utils.round(price + coupon, 2)));
					}
				}
			}
			resetParallelResourceBasedOnPrice(cr);
			cr.setQuery_result(null);
		}
	}
	
	public String resultToJson(){
		return JSON.toJSON(carResourceGroup).toString();
	}
	
	public boolean setUserId(String user_id){
		this.user_id = user_id;
		return true;
	}
	
	public boolean setMessages(String messages){
		this.messages = messages;
		parse();
		return true;
	}
	
	private void parse(){
		String[] tmp = messages.split("\\\\n");
		if(tmp.length<2)
			message_arr = tmp;
		else{
			ArrayList<String> hehe = new ArrayList<String>();
			hehe.add(tmp[0]);
			for(int i=1;i<tmp.length;i++){
				if(tmp[i].equals(hehe.get(hehe.size()-1)) || tmp[i].trim().isEmpty()){
					continue;
				}else{
					hehe.add(tmp[i]);
				}
			}
			message_arr = new String[hehe.size()];
			for(int i=0;i<hehe.size();i++){
				message_arr[i] = hehe.get(i);
			}
		}
	}
	
	private void fillHeaderRecord(BaseCarFinder baseCarFinder){
		if(baseCarFinder.brands.size()>0)
			this.last_brand_name = baseCarFinder.brands.get(0);
		else
			this.last_brand_name = baseCarFinder.cur_brand;
		if(baseCarFinder.models.size()>0)
			this.last_model_name = baseCarFinder.models.get(0);
		else
			this.last_model_name = baseCarFinder.cur_model;
		if(baseCarFinder.styles.size()>0)
			this.last_style_name = baseCarFinder.styles.get(0);
		else
			this.last_style_name = "";
		this.last_standard_name = 1;
	}
	
	private void fillHeaderRecord(BaseCarFinder baseCarFinder, int standard){
		if(baseCarFinder.brands.size()>0)
			this.last_brand_name = baseCarFinder.brands.get(0);
		else
			this.last_brand_name = baseCarFinder.cur_brand;
		if(baseCarFinder.models.size()>0)
			this.last_model_name = baseCarFinder.models.get(0);
		else
			this.last_model_name = baseCarFinder.cur_model;
		if(baseCarFinder.styles.size()>0)
			this.last_style_name = baseCarFinder.styles.get(0);
		//else
			//this.last_style_name = "";
		this.last_standard_name = standard;
	}
	
	private String rebuildQueryPrefix(BaseCarFinder baseCarFinder, int level){
		String standard_query = "";
		if(level==0){
			if(baseCarFinder.brands.size()==0 && last_brand_name !=null && !last_brand_name.isEmpty()){
				return last_brand_name;
			}
			if(baseCarFinder.models.size()!=0 && !baseCarFinder.models.get(0).equals(last_model_name)){
				return "";
			}
			if(baseCarFinder.models.size()==0 && last_model_name !=null && !last_model_name.isEmpty()){
				return last_model_name;
			}
			//if(baseCarFinder.styles.size()==0 && last_style_name !=null && !last_style_name.isEmpty() && !last_style_name.matches("\\d\\d\\d\\d") && !last_style_name.matches("\\d\\d")){
				//return last_style_name;
			//}
		}else{
			if(baseCarFinder.brands.size()==0 && last_brand_name !=null && !last_brand_name.isEmpty()){
				standard_query += " " + last_brand_name;
			}
			if(baseCarFinder.models.size()!=0 && baseCarFinder.models.get(0).length()>1 && !baseCarFinder.models.get(0).equals(last_model_name)){
				return standard_query;
			}
			if(last_model_name !=null && !last_model_name.isEmpty()){
				standard_query += " " + last_model_name;
			}
			
			//只在年款层面继承STYLE，其他的STYLE一律不管
			if(baseCarFinder.years.isEmpty() && baseCarFinder.styles.size()==0 && last_style_name !=null && !last_style_name.isEmpty() && !last_style_name.matches("\\d\\d\\d\\d") && !last_style_name.matches("\\d\\d")){
				if(last_style_name.equals("老") || last_style_name.equals("老款") || last_style_name.equals("新") || last_style_name.equals("新款"))
					standard_query += " " + last_style_name;
				if(last_style_name.replaceAll("\\d\\d款", "").isEmpty())
					standard_query += " " + last_style_name;
			}
		}
		return standard_query.trim();
	}
	
	public void writeInvalidInfo(String s){
		try{
			if(writer!=null){
				writer.newLine();
				writer.write(s);
				writer.flush();
			}else{
				log.info("[batch_processor]\t {} 是无效数据",s);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public String concatWithSpace(String s){
		for(int i=s.length();i<50;i++){
			s = s + " ";
		}
		return s;
	}
	
	public boolean checkValidation(){
		for(String s : message_arr){
			if(s.trim().isEmpty()){
				continue;
			}
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(s, solr_client);
			int mode = simpleMessageClassifier.predict();
			if(mode==1){
				return false;
			}
		}
		return true;
	}
	
	/*
	 * 使用伪相关反馈判断该车型规格
	 */
	private int reJudgeStandard(SolrDocumentList queryResult){
		int parallel = 0;
		for(int i=0;i<5 && i<queryResult.size();i++){
			String standard = queryResult.get(i).get("standard").toString();
			if(i==0 && standard.equals("1"))
				return 1;
			if("2".equals(standard))
				parallel++;
		}
		return parallel>=Math.min(3, (queryResult.size()-1)/2 + 1)?2:1;
	}
	
	private boolean isInvalidInfo(BaseCarFinder baseCarFinder){
		/*
		if(baseCarFinder.query_results.size()>=40){
			if(baseCarFinder.models.isEmpty() && baseCarFinder.prices.isEmpty())
				return true;
			if(baseCarFinder.prices.isEmpty() && baseCarFinder.styles.isEmpty())
				return true;
		}
		*/
		return false;
	}
	
	// 在搜索结果数量较少，例如只有3998这个指导价的情况下，需要判定搜索结果里的品牌个数，如果有多个，则需要向上回溯，找到正确的品牌
	private boolean hasMultiBrands(SolrDocumentList queryResult){
		Set<String> brands_counter = new HashSet<String>();
		if(queryResult.size()==0)
			return false;
		float maxScore = NumberUtils.toFloat(queryResult.get(0).get("score").toString());
		for(int i=0;i<queryResult.size();i++){
			float score = NumberUtils.toFloat(queryResult.get(i).get("score").toString());
			if(score<maxScore)
				break;
			brands_counter.add(queryResult.get(i).get("brand_name").toString());
		}
		return brands_counter.size()>1;
	}
	
	// 在搜索结果数量较少，例如只有3998这个指导价的情况下，需要判定搜索结果里的品牌个数，如果有多个，则需要向上回溯，找到正确的品牌
	private boolean hasMultiModels(SolrDocumentList queryResult){
		Set<String> models_counter = new HashSet<String>();
		if(queryResult.size()==0)
			return false;
		float maxScore = NumberUtils.toFloat(queryResult.get(0).get("score").toString());
		for(int i=0;i<queryResult.size();i++){
			float score = NumberUtils.toFloat(queryResult.get(i).get("score").toString());
			if(score<maxScore)
				break;
			models_counter.add(queryResult.get(i).get("car_model_name").toString());
		}
		return models_counter.size()>1;
	}
	
	//
	private boolean isLatentParallel(BaseCarFinder bcf, String line){
		// 内容中明显包含 指导价，下xx点等情况，那么该行信息就不可能是一条平行进口车车源
		if(MessageStandardClassifier.predict(line)==1)
			return false;
		
		// 内容中没有显式的包含 指导价，下xx点等信息
		// 但是有明显是指导价的token，例如124900等，那么也不可能是平行进口车车源
		for(String ele:bcf.ele_arr){
			if(ele.endsWith("PRICE")){
				String fc = ele.substring(ele.lastIndexOf("|") + 1, ele.indexOf("#"));
				if(!fc.contains(".") && fc.length()>4 && fc.endsWith("00"))
					return false;
				if(!fc.contains(".") && fc.length()>2){//有可能是指导价
					int value = NumberUtils.toInt(fc);
					if(fc.length()==3 && value>500 && !fc.endsWith("0"))
						return false;
				}
			}
		}
		return true;
	}
	
	private void reExtractPriceFromConfiguration(CarResource cr, String info){
		String price = ParallelResourcePriceClassifier.predict(info);
		//从price中提取数字部分
		float p = 0.0f;
		if( price != null){
			if(price.matches("\\d{1,3}(\\.\\d)?(w|W|万)$")){
				price = price.substring(0,  price.length()-1);
			}
		}else{
			ArrayList<String> tokens = Utils.tokenize(info, solr_client, "filter_word");
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
	
	private String reExtractVinFromConfiguration(CarResource cr, String info){
		StringBuilder sb = new StringBuilder(info);
		if(cr.getVin()==null || cr.getVin().isEmpty()){
			String vin = ParallelResourceVinClassifier.predict(sb);
			if(vin!=null)
				cr.setVin(vin);
		}
		return new String(sb);
	}
	
	private boolean isYearInfo(String str) {
		Matcher matcher = yearPattern.matcher(str);
		return matcher.matches();
	}
	
	private void postProcessInvalidLine(String s, String reserve_s){
		if(last_standard_name==-1){
			CarResource tmpCR = carResourceGroup.result.get(carResourceGroup.getResult().size()-1);
			s = Utils.removeDuplicateSpace(Utils.normalizePrice(Utils.clean(Utils.normalize(reserve_s), solr_client)));
			s = reExtractVinFromConfiguration(tmpCR, s);
			if(tmpCR.getResource_type()==null){
				String resource_type = ResourceTypeClassifier.predict(s);
				if(resource_type!=null)
					tmpCR.setResource_type(resource_type);
			}
			if(tmpCR.getDiscount_way().equals("5")){
				reExtractPriceFromConfiguration(tmpCR, s);
			}
			tmpCR.setRemark(tmpCR.getRemark() + "\n" + s);
		}
		writeInvalidInfo(concatWithSpace(s));
	}
	
	public boolean process(){
		long t1 = System.currentTimeMillis();
		for(String s : message_arr){
			
			if(s.trim().isEmpty() || s.length()<2){
				continue;
			}
			
			if(!disableCache && NiuniuBatchConfig.getEnableCache()){
				String hit = CacheManager.get(user_id + "_" + s);
				if(hit!=null){
					CarResource cr = JSON.parseObject(hit, CarResource.class);
					carResourceGroup.getResult().add(cr);
					last_brand_name = cr.getBrand_name();
					last_model_name = cr.getCar_model_name();
					last_standard_name = cr.getStandard()==2?-1:1;
					log.info("[batch_processor]\t {}_{}\t 缓存命中", user_id, s);
					continue;
				}
			}
			
			String reserve_s = s;
			
			s = Utils.removeDuplicateSpace(Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(Utils.escapeSpecialDot(s)), solr_client))));
			/*
			 * 验证该行文本的有效性，如果有多个指导价就放弃一蛤
			 */
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(s, solr_client);
			int mode = simpleMessageClassifier.predict();
			if(mode==0 && !s.isEmpty()){//有可能是隐式的平行进口车,可以再抢救一下
				BaseCarFinder BCF = new BaseCarFinder(solr_client, last_brand_name);
				boolean flag = BCF.generateBaseCarId(s, null, 2);
				if(flag && BCF.query_results.getMaxScore()>5000)
					mode = -1;
			}
			if(mode==0){
				//是上一个平行进口车的配置、备注信息
				if(last_standard_name==-1){
					CarResource tmpCR = carResourceGroup.result.get(carResourceGroup.getResult().size()-1);
					if(s.isEmpty())
						s = reserve_s;
					else
						s = Utils.removeDuplicateSpace(Utils.normalizePrice(Utils.clean(Utils.normalize(reserve_s), solr_client)));
					s = reExtractVinFromConfiguration(tmpCR, s);
					if(tmpCR.getResource_type()==null){
						String resource_type = ResourceTypeClassifier.predict(s);
						if(resource_type!=null)
							tmpCR.setResource_type(resource_type);
					}
					if(tmpCR.getDiscount_way().equals("5")){
						reExtractPriceFromConfiguration(tmpCR, s);
					}
					tmpCR.setRemark(tmpCR.getRemark() + "\n" + s);
				}
				continue;
				// 该行文本包含多个指导价
				// 后续考虑把该行文本的可靠信息，例如品牌，车型等，加入到Header中
				// TODO 
			}
			
			BaseCarFinder baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
			boolean status = false;
			if(mode==-1){
				status = baseCarFinder.generateBaseCarId(s, null, 2);
			}
			else{
				status = baseCarFinder.generateBaseCarId(s, null);
			}
			
			if(!status || (baseCarFinder.query_results.size()>=40 && baseCarFinder.query_results.getMaxScore()<3000) || baseCarFinder.query_results.getMaxScore()<3000){
				status = false;
			}
			
			if(status){
				if(isInvalidInfo(baseCarFinder)){
					fillHeaderRecord(baseCarFinder);
					status = false;
				}
			}
			
			if(!status){
				
				//有可能是平行进口车被误识别成中规国产车，导致有可能把车架号代入到搜索阶段
				if(mode==1 && isLatentParallel(baseCarFinder, s)){
					baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
					status = baseCarFinder.generateBaseCarId(s, null, 2);
					mode = -1;
				}
				if(!status){
					if(last_standard_name==-1){
						CarResource tmpCR = carResourceGroup.result.get(carResourceGroup.getResult().size()-1);
						s = Utils.removeDuplicateSpace(Utils.normalizePrice(Utils.clean(Utils.normalize(reserve_s), solr_client)));
						s = reExtractVinFromConfiguration(tmpCR, s);
						if(tmpCR.getResource_type()==null){
							String resource_type = ResourceTypeClassifier.predict(s);
							if(resource_type!=null)
								tmpCR.setResource_type(resource_type);
						}
						if(tmpCR.getDiscount_way().equals("5")){
							reExtractPriceFromConfiguration(tmpCR, s);
						}
						tmpCR.setRemark(tmpCR.getRemark() + "\n" + s);
					}
					writeInvalidInfo(concatWithSpace(s));
					continue;
				}
			}
			//头部信息，例如 【宝马】 老朗逸等
			if(baseCarFinder.isHeader()){
				fillHeaderRecord(baseCarFinder);
				writeInvalidInfo(concatWithSpace(s));
				continue;
			}
			
			boolean fucking_status = true;
			// 找到的款式太多，需要使用头部的信息来缩小范围
			if(mode==1){
				if(reJudgeStandard(baseCarFinder.query_results)==2){
					mode=-1;
				}else{
					if(baseCarFinder.query_results.size()>=20 && baseCarFinder.models.isEmpty() && baseCarFinder.styles.isEmpty() && baseCarFinder.prices.isEmpty()){
						status = false;
						writeInvalidInfo(concatWithSpace(s));
						continue;
					}
					
					/*
					 * 该行只有指导价，或者只有指导价+年款，所以需要把上一行的所有信息都带过来
					 */
					if(!baseCarFinder.prices.isEmpty() && baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty() && (baseCarFinder.styles.isEmpty() || isYearInfo(baseCarFinder.styles.get(0)))){
						String all_prefix = rebuildQueryPrefix(baseCarFinder,1);
						if(!all_prefix.isEmpty()){
							BaseCarFinder baseCarFinder_new = new BaseCarFinder(solr_client, last_brand_name);
							status = baseCarFinder_new.generateBaseCarId(s, all_prefix, mode);
							if(status){
								baseCarFinder = baseCarFinder_new;
							}else{
								fucking_status = status;
							}
						}else{
							/*
							status = false;
							writeInvalidInfo(concatWithSpace(s));
							continue;
							*/
						}
					}
					if(!status){
						/*
						 * 可能有歧义，例如 730 928 既有宝骏又有宝马
						 */
						if(last_brand_name!=null){
							BaseCarFinder baseCarFinder2 = new BaseCarFinder(solr_client, last_brand_name);
							boolean status2 = baseCarFinder2.generateBaseCarId(s, last_brand_name,mode);
							if(status2){
								baseCarFinder = baseCarFinder2;
							}
						}
					}
					
					
					if(baseCarFinder.query_results.size()>=3 || baseCarFinder.query_results.getMaxScore()<3000 || (baseCarFinder.query_results.size()<=4 && baseCarFinder.query_results.getMaxScore()<3000 && hasMultiBrands(baseCarFinder.query_results))){
						String prefix = rebuildQueryPrefix(baseCarFinder,1);
						if(!prefix.isEmpty()){
							BaseCarFinder baseCarFinder_new = new BaseCarFinder(solr_client, last_brand_name);
							status = baseCarFinder_new.generateBaseCarId(s, prefix, mode);
							if(status){
								baseCarFinder = baseCarFinder_new;
							}
							if(status && hasMultiModels(baseCarFinder.query_results)){
								String simple_prefix = rebuildQueryPrefix(baseCarFinder,0);
								if(!simple_prefix.equals(prefix)){
									baseCarFinder_new = new BaseCarFinder(solr_client, last_brand_name);
									status = baseCarFinder_new.generateBaseCarId(s, simple_prefix, mode);
									if(status){
										baseCarFinder = baseCarFinder_new;
									}
								}
							}
						}
							
						if(baseCarFinder.query_results.size()>=5 && baseCarFinder.isInvalidMessage()){
							if(!(baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty())){
								fillHeaderRecord(baseCarFinder);
							}
							writeInvalidInfo(concatWithSpace(s + "\t 返回结果较多"));
							continue;
						}else{
							if(baseCarFinder.query_results.size()>=5 || (baseCarFinder.query_results.size()<=4 && baseCarFinder.query_results.getMaxScore()<3000 && hasMultiBrands(baseCarFinder.query_results))){
								if(!(baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty())){
									fillHeaderRecord(baseCarFinder);
								}
								writeInvalidInfo(concatWithSpace(s + "\t 返回结果较多"));
								continue;
							}
						}
					}
					if(baseCarFinder.query_results.getMaxScore()<3000  || (baseCarFinder.query_results.size()<=5 && baseCarFinder.query_results.getMaxScore()<3000 && hasMultiBrands(baseCarFinder.query_results))){
						// 置信度较低，查找结果的分数低于某个阈值
						writeInvalidInfo(concatWithSpace(s));
						continue;
					}
					if(!fucking_status && baseCarFinder.query_results.getMaxScore()<3000 && baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty() && baseCarFinder.styles.isEmpty()){
						BaseCarFinder baseCarFinder_new = new BaseCarFinder(solr_client, last_brand_name);
						String simple_prefix = rebuildQueryPrefix(baseCarFinder,0);
						status = baseCarFinder_new.generateBaseCarId(s, simple_prefix, mode);
						if(!status){
							writeInvalidInfo(concatWithSpace(s));
							continue;
						}
					}
					
					//如果有效的搜索信息只有prices里的一个token，而且即使把之前的信息带过来也只有价格本身的分数，即只有3000，这行信息就不该被识别为正常的资源
					if(baseCarFinder.query_results.getMaxScore()<4000 && !baseCarFinder.prices.isEmpty() && baseCarFinder.prices.size()==1 && baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty() && baseCarFinder.styles.isEmpty()){
						writeInvalidInfo(concatWithSpace(s));
						continue;
					}
					
					//baseCarFinder.generateColors(mode,1);
					baseCarFinder.newGenerateColors(mode, 1);
					baseCarFinder.generateRealPrice();
					//TODO 
					//这里如果颜色是空，就重新跑颜色的生成模块
					if(baseCarFinder.result_colors.isEmpty()){
						baseCarFinder.newGenerateColors(mode,2);
					}
					
					baseCarFinder.addToResponseWithCache(user_id, reserve_s, res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark, this.carResourceGroup, mode, null, "现车", disableCache);
					baseCarFinder.printParsingResult(writer);
					fillHeaderRecord(baseCarFinder, mode);
				}
			}
			if(mode==-1){
				//如果是平行进口车
				baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
				boolean tmp_status = baseCarFinder.generateBaseCarId(s, null, 2);
				ArrayList<String> style_not_year = new ArrayList<String>();
				for(String ts:baseCarFinder.styles){
					if(!isYearInfo(ts)){
						style_not_year.add(ts);
					}
				}
				if(tmp_status && baseCarFinder.query_results.size()>=3 && baseCarFinder.models.isEmpty() && style_not_year.isEmpty()){
					status = false;
					writeInvalidInfo(concatWithSpace(s));
					continue;
				}
				if(!tmp_status){
					if(last_standard_name==-1){
						CarResource tmpCR = carResourceGroup.result.get(carResourceGroup.getResult().size()-1);
						s = Utils.removeDuplicateSpace(Utils.normalizePrice(Utils.clean(Utils.normalize(reserve_s), solr_client)));
						s = reExtractVinFromConfiguration(tmpCR, s);
						if(tmpCR.getResource_type()==null){
							String resource_type = ResourceTypeClassifier.predict(s);
							if(resource_type!=null)
								tmpCR.setResource_type(resource_type);
						}
						if(tmpCR.getDiscount_way().equals("5")){
							reExtractPriceFromConfiguration(tmpCR, s);
						}
						tmpCR.setRemark(tmpCR.getRemark() + "\n" + s);
					}
					writeInvalidInfo(concatWithSpace(s));
					continue;
				}
				if(baseCarFinder.query_results.getMaxScore()<3001){
					String prefix = rebuildQueryPrefix(baseCarFinder,0);
					if(!prefix.isEmpty()){
						baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
						status = baseCarFinder.generateBaseCarId(s, prefix, 2);
						if(baseCarFinder.query_results.size()==0){
							postProcessInvalidLine(s, reserve_s);
							continue;
						}
						BaseCarFinder temp_baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
						boolean temp_status = temp_baseCarFinder.generateBaseCarId(prefix, null, 2);
						if(temp_status && baseCarFinder.query_results.getMaxScore().compareTo(temp_baseCarFinder.query_results.getMaxScore())==0){
							postProcessInvalidLine(s, reserve_s);
							continue;
						}
						
						if(baseCarFinder.query_results.size()>5){
							String all_prefix = rebuildQueryPrefix(baseCarFinder,1);
							if(!all_prefix.equals(prefix)){
								baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
								status = baseCarFinder.generateBaseCarId(s, all_prefix, 2);
								if(baseCarFinder.query_results.size()==0){
									postProcessInvalidLine(s, reserve_s);
									continue;
								}
								temp_baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
								temp_status = temp_baseCarFinder.generateBaseCarId(prefix, null, 2);
								if(temp_status && baseCarFinder.query_results.getMaxScore()==temp_baseCarFinder.query_results.getMaxScore()){
									postProcessInvalidLine(s, reserve_s);
									continue;
								}
							}
						}
					}
				}
				if(baseCarFinder.query_results.getMaxScore()<3001){
					// 置信度较低，查找结果的分数低于某个阈值
					writeInvalidInfo(concatWithSpace(s));
					continue;
				}
				baseCarFinder.newGenerateColors(mode, 1);
				String VIN = baseCarFinder.extractVIN();
				baseCarFinder.generarteParellelPrice();
				String resource_type = ResourceTypeClassifier.predict(s);
				baseCarFinder.addToResponseWithCache(user_id, reserve_s, res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark, this.carResourceGroup, mode, VIN, resource_type, disableCache);
				baseCarFinder.printParsingResult(writer);
				fillHeaderRecord(baseCarFinder, -1);
			}
		}
		postProcess();
		long t2 = System.currentTimeMillis();
		carResourceGroup.setQTime(Long.toString(t2-t1));
		log.info("[batch_processor]\t 总耗时： {}", Long.toString(t2-t1));
		return true;
	}
	
	public static void main(String[] args){
		ResourceMessageProcessor resourceMessageProcessor = new ResourceMessageProcessor();
		resourceMessageProcessor.setMessages("别克全新一代君威\\n199800 白 金 红🔻7500");
		//误把1518识别成了前一个车的售价
		resourceMessageProcessor.setMessages("北京现车，荣威RX5. 143800白，151800白，手续随车，18911718669");
		
		//TODO
		//resourceMessageProcessor.setMessages("一汽大众-迈腾 2499开罗金黑×2，优惠25000 自家现车，上汽大众专区");//这个价格识别的bad case实在不好解决。。
		//resourceMessageProcessor.setMessages("18款雷克萨斯570 黑棕 黑红 163 12月25号到港 \\n↘️ 18款宾利添越4.0 V8柴油💰 280W 黑/棕\\n颜色分离-样式D，天窗-标配，前座椅舒适包（五座），脚感电尾，宾利刺绣徽标，脚垫");
		//需要调整底层检索字段的存储方式，从而enable document boost
		//resourceMessageProcessor.setMessages("420   42   白黑     13");
		
		
		resourceMessageProcessor.process();
		//CarResourceGroup crg = resourceMessageProcessor.carResourceGroup;
		//System.out.println(JSON.toJSON(crg));
		System.out.println(resourceMessageProcessor.resultToJson());
	}
}
