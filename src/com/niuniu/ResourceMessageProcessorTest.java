package com.niuniu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ResourceMessageProcessorTest {
	
	static File file = null;
	static FileWriter fw = null;
	static BufferedWriter writer = null;
	
	static{
		try{
			file = new File("/Users/kehl/compare.txt");
			fw = null;
			writer = null;
			fw = new FileWriter(file);
			writer = new BufferedWriter(fw);
		}catch(Exception e){
			
		}
	}
	
	public static void main(String[] args){
		File file = new File("/Users/kehl/Documents/workspace/MessageProcessor/src/com/niuniu/resource/indicator/test_case");
        BufferedReader reader = null; 
		try{
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				ResourceMessageProcessor resourceMessageProcessor = new ResourceMessageProcessor(writer);
				resourceMessageProcessor.setMessages(line);
				resourceMessageProcessor.process();
				System.out.println("##########################################################################################");
				System.out.println("##########################################################################################");
				System.out.println("##########################################################################################");
				System.out.println("##########################################################################################");
				System.out.println("##########################################################################################");
				writer.newLine();
				writer.write("##########################################################################################");
				writer.newLine();
				writer.write("##########################################################################################");
				writer.flush();
				Thread.sleep(1000);
			}
			writer.flush();
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
