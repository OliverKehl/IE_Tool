package com.niuniu;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.niuniu.cache.CacheManager;
import com.niuniu.config.NiuniuBatchConfig;

public class ResourceMessageProcessor {
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
	}
	
	public String resultToJson(){
		if(carResourceGroup==null)
			carResourceGroup = new CarResourceGroup();
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
				if(tmp[i].equals(hehe.get(hehe.size()-1))){
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
		else
			this.last_style_name = "";
		this.last_standard_name = standard;
	}
	
	private String rebuildQueryPrefix(BaseCarFinder baseCarFinder, int level){
		String standard_query = "";
		if(level==0){
			if(baseCarFinder.brands.size()==0 && last_brand_name !=null && !last_brand_name.isEmpty()){
				return last_brand_name;
			}
			if(baseCarFinder.models.size()==0 && last_model_name !=null && !last_model_name.isEmpty()){
				return last_model_name;
			}
			if(baseCarFinder.styles.size()==0 && last_style_name !=null && !last_style_name.isEmpty()){
				return last_style_name;
			}
		}else{
			if(baseCarFinder.brands.size()==0 && last_brand_name !=null && !last_brand_name.isEmpty()){
				standard_query += " " + last_brand_name;
			}
			
			if(baseCarFinder.models.size()==0 && last_model_name !=null && !last_model_name.isEmpty()){
				standard_query += " " + last_model_name;
			}
			
			if(baseCarFinder.styles.size()==0 && last_style_name !=null && !last_style_name.isEmpty()){
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
				log.info("[batch_processor]\t" + s + "是无效数据");
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
		if(baseCarFinder.query_results.size()>=40){
			if(baseCarFinder.models.isEmpty() && baseCarFinder.prices.isEmpty())
				return true;
			if(baseCarFinder.prices.isEmpty() && baseCarFinder.styles.isEmpty())
				return true;
		}
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
	
	public boolean process(){
		long t1 = System.currentTimeMillis();
		for(String s : message_arr){
			if(s.trim().isEmpty()){
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
					log.info("[batch_processor]\t" + user_id + "_" + s + "\t 缓存命中");
					continue;
				}
			}
			
			
			String reserve_s = s;
			
			s = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(s), solr_client)));
			
			/*
			 * 验证该行文本的有效性，如果有多个指导价就放弃一蛤
			 */
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(s, solr_client);
			int mode = simpleMessageClassifier.predict();
			if(mode==0){
				//是上一个平行进口车的配置、备注信息
				if(last_standard_name==-1){
					CarResource tmpCR = carResourceGroup.result.get(carResourceGroup.getResult().size()-1);
					tmpCR.setRemark(tmpCR.getRemark() + "\n" + s);
				}
				continue;
				// 该行文本包含多个指导价
				// 后续考虑把该行文本的可靠信息，例如品牌，车型等，加入到Header中
				// TODO 
			}
			
			BaseCarFinder baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
			boolean status = baseCarFinder.generateBaseCarId(s, null);
			
			if(!status || (baseCarFinder.query_results.size()>=40 && baseCarFinder.query_results.getMaxScore()<2500)){
				status = false;
			}
			
			if(status){
				if(isInvalidInfo(baseCarFinder)){
					fillHeaderRecord(baseCarFinder);
					status = false;
				}
			}
			
			if(!status){
				if(last_standard_name==-1){
					CarResource tmpCR = carResourceGroup.result.get(carResourceGroup.getResult().size()-1);
					tmpCR.setRemark(tmpCR.getRemark() + "\n" + s);
				}
				writeInvalidInfo(concatWithSpace(s));
				continue;
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
					 * 该行只有指导价，所以需要把上一行的所有信息都带过来
					 */
					if(!baseCarFinder.prices.isEmpty() && baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty() && baseCarFinder.styles.isEmpty()){
						String all_prefix = rebuildQueryPrefix(baseCarFinder,1);
						if(!all_prefix.isEmpty()){
							BaseCarFinder baseCarFinder_new = new BaseCarFinder(solr_client, last_brand_name);
							status = baseCarFinder_new.generateBaseCarId(s, all_prefix, mode);
							if(status){
								baseCarFinder = baseCarFinder_new;
							}else{
								fucking_status = status;
							}
						}
					}
					if(!status){
						/*
						 * 可能有歧义，例如 730 928 既有宝骏又有宝马
						 */
						BaseCarFinder baseCarFinder2 = new BaseCarFinder(solr_client, last_brand_name);
						boolean status2 = baseCarFinder2.generateBaseCarId(s, last_brand_name,mode);
						if(status2){
							baseCarFinder = baseCarFinder2;
						}
					}
					
					
					if(baseCarFinder.query_results.size()>=3 || baseCarFinder.query_results.getMaxScore()<2500 || (baseCarFinder.query_results.size()<=3 && baseCarFinder.query_results.getMaxScore()<3000 && hasMultiBrands(baseCarFinder.query_results))){
						String prefix = rebuildQueryPrefix(baseCarFinder,1);
						if(!prefix.isEmpty()){
							BaseCarFinder baseCarFinder_new = new BaseCarFinder(solr_client, last_brand_name);
							status = baseCarFinder_new.generateBaseCarId(s, prefix, mode);
							if(status){
								baseCarFinder = baseCarFinder_new;
							}else{
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
							
						if(baseCarFinder.query_results.size()>5 && baseCarFinder.isInvalidMessage()){
							if(!(baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty())){
								fillHeaderRecord(baseCarFinder);
							}
							writeInvalidInfo(concatWithSpace(s));
							continue;
						}else{
							if(baseCarFinder.query_results.size()>5 || (baseCarFinder.query_results.size()<=3 && baseCarFinder.query_results.getMaxScore()<3000 && hasMultiBrands(baseCarFinder.query_results))){
								if(!(baseCarFinder.brands.isEmpty() && baseCarFinder.models.isEmpty())){
									fillHeaderRecord(baseCarFinder);
								}
								writeInvalidInfo(concatWithSpace(s));
								continue;
							}
						}
					}
					if(baseCarFinder.query_results.getMaxScore()<2500  || (baseCarFinder.query_results.size()<=3 && baseCarFinder.query_results.getMaxScore()<3000 && hasMultiBrands(baseCarFinder.query_results))){
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
					
					baseCarFinder.generateColors();
					baseCarFinder.generateRealPrice();
					baseCarFinder.addToResponseWithCache(user_id, reserve_s, res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark, this.carResourceGroup, mode, null, disableCache);
					baseCarFinder.printParsingResult(writer);
					fillHeaderRecord(baseCarFinder, mode);
				}
			}
			if(mode==-1){
				//如果是平行进口车
				boolean tmp_status = baseCarFinder.generateBaseCarId(s, null, 2);
				if(tmp_status){
					//平行进口车车型库没找到。。。是不是还要再回到中规国产去找呢？？？
					// TODO
				}
				if(baseCarFinder.query_results.getMaxScore()<2500){
					String prefix = rebuildQueryPrefix(baseCarFinder,0);
					if(!prefix.isEmpty()){
						baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
						status = baseCarFinder.generateBaseCarId(s, prefix, 2);
						if(baseCarFinder.query_results.size()==0){
							continue;
						}
						
						if(baseCarFinder.query_results.size()>5){
							String all_prefix = rebuildQueryPrefix(baseCarFinder,1);
							if(!all_prefix.equals(prefix)){
								baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
								status = baseCarFinder.generateBaseCarId(s, all_prefix, 2);
								if(baseCarFinder.query_results.size()==0){
									continue;
								}
							}
						}
					}
				}
				if(baseCarFinder.query_results.getMaxScore()<2500){
					// 置信度较低，查找结果的分数低于某个阈值
					writeInvalidInfo(concatWithSpace(s));
					continue;
				}
				baseCarFinder.generateColors();
				String VIN = baseCarFinder.extractVIN();
				baseCarFinder.generarteParellelPrice();
				baseCarFinder.addToResponseWithCache(user_id, reserve_s, res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark, this.carResourceGroup, mode, VIN, disableCache);
				baseCarFinder.printParsingResult(writer);
				fillHeaderRecord(baseCarFinder, -1);
			}
		}
		long t2 = System.currentTimeMillis();
		carResourceGroup.setQTime(Long.toString(t2-t1));
		return true;
	}
	
	public static void main(String[] args){
		ResourceMessageProcessor resourceMessageProcessor = new ResourceMessageProcessor();
		resourceMessageProcessor.setMessages("新捷达\n856 白 银15000\n976 白 银15500\n956 白 金 银14000\n老捷达1056白19000\n  宝来\n1078白20000\n1078黑 金19000\n1198白 黑 灰 银 金18800手动\n1198白18000自动\n1198金19000自动\n1318白 黑 金 银17500\n1418白19000\n  蔚领\n1259 棕金 9000\n1489 白 黑 棕9000\n1519 白9000\n  高尔夫百万\n1549 红 白 金16500\n1659 白 金16500\n1719 白16000\n嘉旅\n1679白17500\n1439橙17500\n  速腾\n1318 白 20000\n1408 灰 白 银 黑19500\n1528 黑 白 20000\n1488 黑 白18500\n1588 白21000\n1628 黑 19000\n1728 黑 白21000\n1628 银 白 金21500\n  迈腾\n1979 黑21000\n2209黑21000\n2349黑21500\n2589流沙金26000\n2589黑25000\n2499银\n一汽大众特许经销商\n王良18910383379\n");
		resourceMessageProcessor.process();
	}
}
