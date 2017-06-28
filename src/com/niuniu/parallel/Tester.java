package com.niuniu.parallel;

public class Tester {
	public static void main(String[] args){
		String str = "自家车100台\t已到港[愉快]  途观(进口) 16款 2.0T 自动 两驱 S  黑白蓝 25万 5台起批 多台价优  配置:17轮、2驱、行李架、";
		String[] arrs = str.split("[,;。，、\\s\\t()]");
		System.out.println(arrs.length);
		for(String s:arrs){
			System.out.println(s);
		}
	}
}
