package com.niuniu;

import java.io.BufferedWriter;
import java.util.ArrayList;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.common.SolrDocumentList;

import com.alibaba.fastjson.JSON;
import com.niuniu.cache.CacheManager;

public class ResourceMessageProcessor {
	
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
	
	CarResourceGroup carResourceGroup = new CarResourceGroup();
	
	public ResourceMessageProcessor(){
		init();
		solr_client = new USolr("http://121.40.204.159:8080/solr/");
	}
	
	public ResourceMessageProcessor(USolr solr_client){
		init();
		this.solr_client = solr_client;
	}
	
	public ResourceMessageProcessor(BufferedWriter writer){
		init();
		this.writer = writer;
		solr_client = new USolr("http://121.40.204.159:8080/solr/");
	}
	
	public ResourceMessageProcessor(String messages){
		init();
		this.messages = messages;
		parse();
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
				System.out.println(s);
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
				System.out.println(s);
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
			if("2".equals(standard))
				parallel++;
		}
		return parallel>=Math.min(3, (queryResult.size()-1)/2 + 1)?2:1;
	}
	
	public boolean process(){
		long t1 = System.currentTimeMillis();
		for(String s : message_arr){
			if(s.trim().isEmpty()){
				continue;
			}
			
			String hit = CacheManager.get(user_id + "_" + s);
			if(hit!=null){
				CarResource cr = JSON.parseObject(hit, CarResource.class);
				carResourceGroup.getResult().add(cr);
				last_brand_name = cr.getBrand_name();
				last_model_name = cr.getCar_model_name();
				last_standard_name = cr.getStandard()==2?-1:1;
				continue;
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
			
			if(!status || (baseCarFinder.query_results.size()>=40 && baseCarFinder.query_results.getMaxScore()<3000)){
				status = false;
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
			
			// 找到的款式太多，需要使用头部的信息来缩小范围
			if(mode==1){
				if(reJudgeStandard(baseCarFinder.query_results)==2){
					mode=-1;
				}else{
					if(baseCarFinder.query_results.size()>=3 || baseCarFinder.query_results.getMaxScore()<3000){
						String prefix = rebuildQueryPrefix(baseCarFinder,0);
						if(!prefix.isEmpty()){
							baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
							status = baseCarFinder.generateBaseCarId(s, prefix, mode);
							if(baseCarFinder.query_results.size()==0){
								continue;
							}
							
							if(baseCarFinder.query_results.size()>5){
								String all_prefix = rebuildQueryPrefix(baseCarFinder,1);
								if(!all_prefix.equals(prefix)){
									baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
									status = baseCarFinder.generateBaseCarId(s, all_prefix, mode);
									if(baseCarFinder.query_results.size()==0){
										continue;
									}
								}
							}
							
							if(baseCarFinder.query_results.size()>5 && baseCarFinder.isInvalidMessage()){
								fillHeaderRecord(baseCarFinder);
								writeInvalidInfo(concatWithSpace(s));
								continue;
							}
						}else{
							if(baseCarFinder.query_results.size()>5){
								fillHeaderRecord(baseCarFinder);
								writeInvalidInfo(concatWithSpace(s));
								continue;
							}
						}
					}
					if(baseCarFinder.query_results.getMaxScore()<3000){
						// 置信度较低，查找结果的分数低于某个阈值
						writeInvalidInfo(concatWithSpace(s));
						continue;
					}
					baseCarFinder.generateColors();
					baseCarFinder.generateRealPrice();
					baseCarFinder.addToResponseWithCache(user_id, reserve_s, res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark, this.carResourceGroup, mode);
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
				if(baseCarFinder.query_results.getMaxScore()<3000){
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
				if(baseCarFinder.query_results.getMaxScore()<3000){
					// 置信度较低，查找结果的分数低于某个阈值
					writeInvalidInfo(concatWithSpace(s));
					continue;
				}
				baseCarFinder.generateColors();
				baseCarFinder.addToResponseWithCache(user_id, reserve_s, res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark, this.carResourceGroup, mode);
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
		resourceMessageProcessor.setMessages("2016保时捷卡宴 SE 混合动力\n  黑/黑米   车架号：59148   \n特价：97万 电话： 18198631583\n车型简介\n18轮、多功能运动型方向盘、LED日间行车灯、LED尾灯、巡航定速、后窗加热、自动尾门、双疝气大灯、自动启停、2区空调控制、8向电动座椅调节、全景天窗、前排座椅加热、吸烟包 \n 科雷嘉1748白 棕 蓝 红↓️29000\n科雷嘉1638 白 棕 红↓29000\n科雷嘉1848 白 棕 蓝 红↓29000\n科雷嘉1968 黑 棕↓29000\n科雷傲1928 白 棕↓ 14000\n科雷傲2058 白 棕 金↓14000\n科雷傲2198 白↓14000\n科雷傲2298 白↓14000\n科雷傲2458白 棕 金15000\n科雷傲2698 白 棕 15000\n卡缤 1598  橙白↓️45000\n\n凯迪拉克:\nATSL 2988白、红、黑、紫\nATSL 3188白、红、黑、紫\nXTS 3499黑、白\nXTS 3699黑、白 金\nXT5 3599黑 白\nXT5 3799 黑\nXT5 3899黑、白 摩卡\nXT5 4199 黑、白\nCT6 4399黑、白\nCT6 4699黑 白\nCT6 4899黑  白\n以上现车，发全国！手续齐\n——————————\nDS 现车特价\nDS4S 1499 红，白，紫\nDS4S 1719  红  \nDS4S 1879 白 \n\n5LS 1688白 紫 \n5LS 1868白 紫\n\nDS5 2199白 \nDS5 30.89  金\n\nDS6 2069白 紫 岩\nDS6 2299   紫  岩 白  \n \n————————————\n沃尔沃 有户来电\nS60L\n276900智进 耀目沙 水晶白\n309900智远  枫木棕\n340900智驭  枫木棕\nXC60 \n17.5款\n358900   暮色铜 枫木棕\n378900   水晶白 枫木棕 耀目沙\n398900   枫木棕 耀目沙\n429900   暮色铜  水晶白 枫木棕 暮色铜        \nV40 \n2299 弗拉明戈红          \n2459 水晶白 暮色铜   耀目沙 \n2659 亚马逊蓝\nV60CC          \n3999  暮色铜 醇咖 水晶白\nXC90          \n9386 水晶白\nS90\n3698 枫木棕 贻贝蓝\n4068 玛瑙黑 枫木棕\n4488 玛瑙黑\n5518 枫木棕\n4s店提车，店车店票，手续齐13602159352 邵娟\n\n本公司主营 【jeep】【凯迪拉克】【英菲尼迪】【DS、宝沃】【沃尔沃】【林肯】【进口起亚】【斯巴鲁】【福特】【雷诺】【自家平行进口车】\n\n13602159352 邵娟15033769169 【微信号】");
		/*
		if(!resourceMessageProcessor.checkValidation()){
			System.out.println("不符合规范");
			return;
		}
		*/
		resourceMessageProcessor.process();
	}
}
