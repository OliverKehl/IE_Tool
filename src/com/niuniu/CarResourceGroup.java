package com.niuniu;

import java.util.ArrayList;

import com.alibaba.fastjson.JSON;

public class CarResourceGroup {
	ArrayList<CarResource> result;
	String status;
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public CarResourceGroup(){
		result = new ArrayList<CarResource>();
		status = "200";
	}

	public ArrayList<CarResource> getResult() {
		return result;
	}

	public void setResult(ArrayList<CarResource> result) {
		this.result = result;
	}
	
	public static void main(String[] args){
		CarResource cr = new CarResource("1234","['珍珠白#黑色', '炫晶黑#黑色']", "2","3.5","欢迎来电");
		CarResourceGroup carResourceGroup = new CarResourceGroup();
		carResourceGroup.result.add(cr);
		carResourceGroup.result.add(new CarResource("5678","", "2","3.5","欢迎来电"));
		System.out.println(JSON.toJSON(carResourceGroup));
	}
}
