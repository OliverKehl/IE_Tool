package com.niuniu;

import java.io.BufferedWriter;
import java.util.ArrayList;

public class ResourceMessageProcessor {
	
	String last_brand_name;
	String last_model_name;
	String last_style_name;
	
	String[] message_arr;
	String messages;
	
	BufferedWriter writer;
	
	USolr solr_client;
	
	ArrayList<String> res_base_car_ids;
	ArrayList<String> res_colors;
	ArrayList<String> res_discount_way;
	ArrayList<String> res_discount_content;
	ArrayList<String> res_remark;
	
	public ResourceMessageProcessor(){
		last_model_name="";
		last_style_name="";
		this.messages="";
		solr_client = new USolr("http://121.40.204.159:8080/solr/");
		res_base_car_ids = new ArrayList<String>();
		res_colors = new ArrayList<String>();
		res_discount_way = new ArrayList<String>();
		res_discount_content = new ArrayList<String>();
		res_remark = new ArrayList<String>();
	}
	
	public ResourceMessageProcessor(USolr solr_client){
		last_model_name="";
		last_style_name="";
		this.messages="";
		this.solr_client = solr_client;
		res_base_car_ids = new ArrayList<String>();
		res_colors = new ArrayList<String>();
		res_discount_way = new ArrayList<String>();
		res_discount_content = new ArrayList<String>();
		res_remark = new ArrayList<String>();
	}
	
	public ResourceMessageProcessor(BufferedWriter writer){
		last_model_name="";
		last_style_name="";
		this.messages="";
		this.writer = writer;
		solr_client = new USolr("http://121.40.204.159:8080/solr/");
		res_base_car_ids = new ArrayList<String>();
		res_colors = new ArrayList<String>();
		res_discount_way = new ArrayList<String>();
		res_discount_content = new ArrayList<String>();
		res_remark = new ArrayList<String>();
	}
	
	public ResourceMessageProcessor(String messages){
		last_model_name="";
		last_style_name="";
		this.messages = messages;
		parse();
		res_base_car_ids = new ArrayList<String>();
		res_colors = new ArrayList<String>();
		res_discount_way = new ArrayList<String>();
		res_discount_content = new ArrayList<String>();
		res_remark = new ArrayList<String>();
	}
	
	public boolean setMessages(String messages){
		this.messages = messages;
		parse();
		return true;
	}
	
	private void parse(){
		String[] tmp = messages.split("\n");
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
	
	public boolean process(){
		for(String s : message_arr){
			if(s.trim().isEmpty()){
				continue;
			}
			
			s = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(s), solr_client)));
			
			BaseCarFinder baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
			boolean status = baseCarFinder.generateBaseCarId(s, null);
			
			if(!status){
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
			if(baseCarFinder.query_results.size()>=3 || baseCarFinder.query_results.getMaxScore()<3000){
				String prefix = rebuildQueryPrefix(baseCarFinder,0);
				if(!prefix.isEmpty()){
					baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
					status = baseCarFinder.generateBaseCarId(s, prefix);
					if(baseCarFinder.query_results.size()==0){
						continue;
					}
					
					if(baseCarFinder.query_results.size()>5){
						String all_prefix = rebuildQueryPrefix(baseCarFinder,1);
						if(!all_prefix.equals(prefix)){
							baseCarFinder = new BaseCarFinder(solr_client, last_brand_name);
							status = baseCarFinder.generateBaseCarId(s, all_prefix);
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
			baseCarFinder.addToResponse(res_base_car_ids, res_colors, res_discount_way, res_discount_content, res_remark);
			baseCarFinder.printParsingResult(writer);
			fillHeaderRecord(baseCarFinder);
		}
		return true;
	}
	
	public static void main(String[] args){
		ResourceMessageProcessor resourceMessageProcessor = new ResourceMessageProcessor();
		resourceMessageProcessor.setMessages("捷豹XE \n 398白黑红9.6");
		if(!resourceMessageProcessor.checkValidation()){
			System.out.println("不符合规范");
			return;
		}
		resourceMessageProcessor.process();
	}
}
