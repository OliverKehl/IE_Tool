package com.niuniu;

import java.util.ArrayList;

public class CarResourceGroup {
	ArrayList<CarResource> result;
	String status;
	String QTime;
	
	public String getQTime() {
		return QTime;
	}

	public void setQTime(String qTime) {
		QTime = qTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public CarResourceGroup(){
		result = new ArrayList<CarResource>();
		status = "200";
		QTime = "0";
	}

	public ArrayList<CarResource> getResult() {
		return result;
	}

	public void setResult(ArrayList<CarResource> result) {
		this.result = result;
	}
}
