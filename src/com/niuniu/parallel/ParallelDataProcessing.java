package com.niuniu.parallel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.niuniu.Utils;

public class ParallelDataProcessing {
	
	Map<String, Integer> candidates;
	
	public ParallelDataProcessing(){
		candidates = new HashMap<String, Integer>();
	}
	
	public void input(String filePath){
		int counter = 0;
		try{
			File file = new File(filePath);
			if(file.isFile() && file.exists()){
				InputStreamReader read = new InputStreamReader(new FileInputStream(file),"utf8");
				BufferedReader bufferedReader = new BufferedReader(read);
				String line = null;
				while( (line = bufferedReader.readLine()) != null ){
					counter++;
					if(counter%1000==0)
						System.out.println(counter);
					line = Utils.removeDuplicateSpace(Utils.normalizePrice(Utils.cleanDate(Utils.normalize(line))));
					String[] arrs = line.split("[:!()&；/,;。，、\\s\\\\t]");
					for(String s:arrs){
						s = s.trim();
						if(s.isEmpty())
							continue;
						
						if(s.length()>10)
							continue;
						
						if(candidates.containsKey(s)){
							candidates.put(s, candidates.get(s)+1);
						}else{
							candidates.put(s, 1);
						}
					}
				}
				read.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void output(String filePath){
		try {
			File file = new File(filePath);
			FileWriter fw = null;
			BufferedWriter writer = null;
			fw = new FileWriter(file);
			writer = new BufferedWriter(fw);
			
			List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(
					candidates.entrySet());
			Collections.sort(entryList,  
	                new Comparator<Map.Entry<String, Integer>>() {  
	                    public int compare(Map.Entry<String, Integer> entry1,  
	                            Map.Entry<String, Integer> entry2) {  
	                        return entry2.getValue().compareTo(entry1.getValue());  
	                    }  
	                });  
			Iterator<Map.Entry<String, Integer>> iter = entryList.iterator();
			Map.Entry<String, Integer> tmpEntry = null;
			while (iter.hasNext()) {
				tmpEntry = iter.next();
				writer.write(tmpEntry.getKey() + "\t" + Integer.toString(tmpEntry.getValue()));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		ParallelDataProcessing pdp = new ParallelDataProcessing();
		pdp.input("/Users/kehl/Documents/workspace/dict_build-0.0.3/bin/hehe_backup");
		System.out.println("reading done.");
		pdp.output("/Users/kehl/Desktop/hehe");
	}
}
