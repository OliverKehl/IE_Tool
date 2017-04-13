package com.niuniu;

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
					   String vin) {
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
		this.remark = remark;
	}

	public static void main(String[] args) {
		CarResource cr = new CarResource("1234", "['珍珠白#黑色', '炫晶黑#黑色']", "2", "3.5", "欢迎来电","宝马","X1", 1, "12345");
		String res = JSON.toJSON(cr).toString();
		System.out.println(res);
		cr = JSON.parseObject(res, CarResource.class);
		System.out.println("done");
	}

}
