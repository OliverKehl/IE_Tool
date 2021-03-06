package com.niuniu;

import org.apache.solr.common.SolrDocumentList;

import com.alibaba.fastjson.JSON;

public class CarResource {

	private String id;
	private String colors;
	private String discount_way;
	private String discount_content;
	private String remark;
	private String brand_name;
	private int standard;
	private String vin;
	private int year;
	private String style_name;
	private String standard_name;
	private String resource_type;
	private String guiding_price;
	private String real_price;
	private SolrDocumentList query_result;
	private int level;

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public SolrDocumentList getQuery_result() {
		return query_result;
	}

	public void setQuery_result(SolrDocumentList query_result) {
		this.query_result = query_result;
	}

	public String getReal_price() {
		return real_price;
	}

	public void setReal_price(String real_price) {
		this.real_price = real_price;
	}

	public String getGuiding_price() {
		return guiding_price;
	}

	public void setGuiding_price(String guiding_price) {
		this.guiding_price = guiding_price;
	}

	public String getResource_type() {
		return resource_type;
	}

	public void setResource_type(String resource_type) {
		this.resource_type = resource_type;
	}

	public String getStandard_name() {
		return standard_name;
	}

	public void setStandard_name(String standard_name) {
		this.standard_name = standard_name;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getStyle_name() {
		return style_name;
	}

	public void setStyle_name(String style_name) {
		this.style_name = style_name;
	}

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	public CarResource(){}
	
	public String getBrand_name() {
		return brand_name;
	}

	public int getStandard() {
		return standard;
	}

	public void setStandard(int standard) {
		this.standard = standard;
	}

	public void setBrand_name(String brand_name) {
		this.brand_name = brand_name;
	}

	public String getCar_model_name() {
		return car_model_name;
	}

	public void setCar_model_name(String car_model_name) {
		this.car_model_name = car_model_name;
	}

	private String car_model_name;
	
	public CarResource(String id, String colors, 
					   String discount_way, String discount_content, 
					   String remark, String brand_name, 
					   String car_model_name, int standard,
					   String vin, int year, String style_name, String standard_name, 
					   String resource_type, String guiding_price, 
					   SolrDocumentList query_result, int level) {
		this.id = id;
		this.colors = colors;
		this.discount_way = discount_way;
		this.discount_content = discount_content;
		this.remark = remark;
		this.brand_name = brand_name;
		this.car_model_name = car_model_name;
		if(standard==1)
			this.standard = 1;
		else
			this.standard = 2;
		if(vin==null)
			this.vin = "";
		else
			this.vin = vin;
		this.year = year;
		this.style_name = style_name;
		this.standard_name = standard_name;
		this.resource_type = resource_type;
		this.guiding_price = guiding_price;
		this.real_price = null;
		this.query_result = query_result;
		this.level = level;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getColors() {
		return colors;
	}

	public void setColors(String colors) {
		this.colors = colors;
	}

	public String getDiscount_way() {
		return discount_way;
	}

	public void setDiscount_way(String discount_way) {
		this.discount_way = discount_way;
	}

	public String getDiscount_content() {
		return discount_content;
	}

	public void setDiscount_content(String discount_content) {
		this.discount_content = discount_content;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark.trim();
	}

	public static void main(String[] args) {
		CarResource cr = new CarResource("1234", "['珍珠白#黑色', '炫晶黑#黑色']", "2", "3.5", "欢迎来电","宝马","X1", 1, "12345", 2017, "终极版", "加版", "现车", "12.5", null, 2);
		cr.setResource_type("期货");
		String res = JSON.toJSON(cr).toString();
		System.out.println(res);
		cr = JSON.parseObject(res, CarResource.class);
		System.out.println("done");
	}

}
