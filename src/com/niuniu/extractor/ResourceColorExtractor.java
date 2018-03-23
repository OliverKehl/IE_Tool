package com.niuniu.extractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;

import com.niuniu.BaseCarFinder;
import com.niuniu.Utils;

/*
 * 到颜色抽取这一阶段
 */
public class ResourceColorExtractor {
	
	ArrayList<Integer> indexes;
	ArrayList<String> ele_arr;
	BaseCarFinder baseCarFinder;
	Set<String> result_colors;
	Set<String> base_colors_set;
	String[] base_colors = { "黑", "白", "红", "灰", "棕", "银", "金", "蓝", "紫", "米" };
	String standard_outer_color;
	String standard_inner_color;
	
	public ResourceColorExtractor(BaseCarFinder baseCarFinder){
		this.baseCarFinder = baseCarFinder;
		this.ele_arr = baseCarFinder.getEle_arr();
		this.indexes = baseCarFinder.getIndexes();
		standard_outer_color = baseCarFinder.getQuery_results().get(0).get("outer_color").toString();
		standard_inner_color = baseCarFinder.getQuery_results().get(0).get("inner_color").toString();
		result_colors = new HashSet<String>();
		base_colors_set = new HashSet<String>();
		for (String s : base_colors) {
			base_colors_set.add(s);
		}
	}
	
	public void extract(int phase){
		ArrayList<String> colors = baseCarFinder.colors;
		Set<Integer> isUsedSet = new HashSet<Integer>();
		ArrayList<Integer> color_tag = new ArrayList<Integer>();
		if(colors.size()==0)
			return;
		boolean inner_flag = false;//上一个颜色是内饰，则last_inner=true，否则是false
		String last_outer_color = null;
		// greedy
		for(int i=0;i<colors.size();i++){
			String c = colors.get(i);
			if(inner_flag || i==0){
				// String outer_standard = matchStandardColor(c, 0);
				// result_colors.add(fetchValidColor(outer_standard, c) + "#");
				if((!newIsExplicitOuterColor(indexes.get(i), isUsedSet) && newIsExplicitInnerColor(indexes.get(i), isUsedSet)) || c.equals("黄鹤")){
					inner_flag = true;
					color_tag.add(1);
					continue;
				}
				last_outer_color = c;
				inner_flag = false;
				continue;
			}else{
				// 上一个颜色是外饰, 则这个颜色有可能是外饰，有可能是内饰
				int pre = i-1;
				int next = i+1;
				if(c.equals("黄鹤")){
					if(last_outer_color!=null){
						String outer_standard = matchStandardColor(last_outer_color, 0);
						result_colors.add(buildColorString(last_outer_color, c, outer_standard, c));
						last_outer_color = null;
						color_tag.add(0);
					}
					color_tag.add(1);
					inner_flag = true;
					continue;
				}
				if(last_outer_color!=null && c.equals(last_outer_color)){
					String outer_standard = matchStandardColor(last_outer_color, 0);
					String inner_standard = matchStandardColor(c, 1);
					result_colors.add(buildColorString(last_outer_color, c, outer_standard, inner_standard));
					last_outer_color = null;
					inner_flag = true;
					color_tag.add(0);
					color_tag.add(1);
					continue;
				}
				if(isAdjacentColor(indexes.get(pre), indexes.get(i)) || indexes.get(pre)==indexes.get(i)){//
					if(i>=2 && (isAdjacentColor(indexes.get(i-2), indexes.get(i-1)) || indexes.get(i-2)==indexes.get(i-1)) && color_tag.get(i-2)==0){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}else if(next>=colors.size() || (!isAdjacentColor(indexes.get(i), indexes.get(next)) && indexes.get(i)!=indexes.get(next) )){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							String inner_standard = matchStandardColor(c, 1);
							result_colors.add(buildColorString(last_outer_color, c, outer_standard, inner_standard));
							color_tag.add(0);
							last_outer_color = null;
						}
						inner_flag = true;
						color_tag.add(1);
					}else if(next>=indexes.size() || isAdjacentColor(indexes.get(i), indexes.get(next)) || indexes.get(i)==indexes.get(next)){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}
				}else{
					if(newIsExplicitOuterColor(indexes.get(i), isUsedSet)){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}else if(newIsExplicitInnerColor(indexes.get(i), isUsedSet)){
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							String inner_standard = matchStandardColor(c, 1);
							result_colors.add(buildColorString(last_outer_color, c, outer_standard, inner_standard));
							last_outer_color = null;
							color_tag.add(0);
						}
						inner_flag = true;
						color_tag.add(1);
					}else{
						if(last_outer_color!=null){
							String outer_standard = matchStandardColor(last_outer_color, 0);
							result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
							color_tag.add(0);
						}
						last_outer_color = c;
					}
				}
			}
		}
		if(last_outer_color!=null){
			String outer_standard = matchStandardColor(last_outer_color, 0);
			result_colors.add(fetchValidColor(outer_standard, last_outer_color) + "#");
			color_tag.add(0);
		}
		baseCarFinder.setResult_colors(result_colors);
	}
	
	private boolean newIsExplicitOuterColor(int idx, Set<Integer> usedSet) {
		if (idx == 0)
			return false;
		if(!usedSet.contains(idx-1)){
			String c = ele_arr.get(idx - 1);
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("外") || content.equals("外色") || content.equals("外饰")) {
				usedSet.add(idx - 1);
				return true;
			}
		}

		if (idx + 1 == ele_arr.size()) {
			return false;
		}
		if(!usedSet.contains(idx+1)){
			String c = ele_arr.get(idx + 1);
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("外") || content.equals("外色") || content.equals("外饰") || content.equals("车")) {
				usedSet.add(idx + 1);
				return true;
			}
		}
		return false;
	}

	private boolean newIsExplicitInnerColor(int idx, Set<Integer> usedSet) {
		if (idx + 1 == ele_arr.size()) {
			return false;
		}
		String c = ele_arr.get(idx + 1);
		if(!usedSet.contains(idx+1)){
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("内") || content.equals("内色") || content.equals("内饰")) {
				usedSet.add(idx+1);
				return true;
			}
		}

		if (idx == 0)
			return false;
		if(!usedSet.contains(idx-1)){
			c = ele_arr.get(idx - 1);
			String content = c.substring(c.lastIndexOf("|") + 1, c.lastIndexOf("#"));
			if (content.equals("内") || content.equals("内色") || content.equals("内饰")) {
				usedSet.add(idx-1);
				return true;
			}
		}
		return false;
	}

	private boolean isAdjacentColor(int idx1, int idx2) {
		String t1 = ele_arr.get(idx1);
		String t2 = ele_arr.get(idx2);

		t1 = t1.substring(t1.indexOf("-") + 1, t1.indexOf("|"));
		t2 = t2.substring(0, t2.indexOf("-"));
		if (t1.equals(t2))
			return true;

		int it1 = NumberUtils.toInt(t1);
		int it2 = NumberUtils.toInt(t2);
		if (it1 + 1 == it2) {
			char c = baseCarFinder.getOriginal_message().charAt(it1);
			if (c == '/') {
				return true;
			}
		}
		return false;
	}

	private String buildColorString(String outer, String inner, String outer_standard, String inner_standard) {
		return fetchValidColor(outer_standard, outer) + "#" + fetchValidColor(inner_standard, inner);
	}

	private String fetchValidColor(String standard_color, String color) {
		return standard_color == null ? color : standard_color;
	}
	
	private String matchStandardColor(String color, int source) {
		String[] standard_colors = null;
		String tmp = null;
		if (source == 0) {
			tmp = baseCarFinder.getQuery_results().get(0).get("outer_color").toString();
		} else {
			tmp = baseCarFinder.getQuery_results().get(0).get("inner_color").toString();
		}
		standard_colors = tmp.split(",");
		int ans = 1000;
		if(color.endsWith("色") && color.length()>1){
			color = color.substring(0, color.length()-1);
		}
		ArrayList<Integer> candidates = new ArrayList<Integer>();
		for (int i = 1; i < standard_colors.length; i++) {
			int distance = Utils.getEditDistance(color, standard_colors[i]);
			if (distance < Math.max(color.length(), standard_colors[i].length())) {
				// 有一定的相关性
				if (ans == distance)
					candidates.add(i);
				else if (ans > distance) {
					ans = distance;
					candidates.clear();
					candidates.add(i);
				}
			}
		}
		return candidates.size() == 1 ? standard_colors[candidates.get(0)] : null;
	}
}
