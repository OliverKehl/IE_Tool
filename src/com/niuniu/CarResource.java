package com.niuniu;

import com.alibaba.fastjson.JSON;

public class CarResource {
	
	private String id;
	private String colors;
	private String discount_way;
	private String discount_content;
	private String remark;
	
	public CarResource(String id, String colors, String discount_way, String discount_content, String remark){
		this.id = id;
		this.colors = colors;
		this.discount_way = discount_way;
		this.discount_content = discount_content;
		this.remark = remark;
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
	
	public static void main(String[] args){
		CarResource cr = new CarResource("1234","['珍珠白#黑色', '炫晶黑#黑色']", "2","3.5","欢迎来电");
		System.out.println(JSON.toJSON(cr));
	}
	
}
