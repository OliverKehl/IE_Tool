package com.niuniu.extractor;

import com.niuniu.BaseCarFinder;
import com.niuniu.Utils;

public class ResourceRemarkExtractor {
	/*
	 * 后处理备注信息
	 */
	public static void extract(BaseCarFinder baseCarFinder) {
		String remark = baseCarFinder.getOriginal_message().substring(baseCarFinder.getBackup_index());
		if (remark == null)
			return;
		remark = remark.trim();
		if (remark.length() == 0)
			return;

		char c = remark.charAt(0);
		if (c == ',' || c == ')' || c == ']' || c == '}'){
			baseCarFinder.setResult_remark(remark.substring(1).trim());
			return;
		}

		if (remark.startsWith("万") || remark.startsWith("点") || remark.startsWith("元") || remark.startsWith("w")
				|| remark.startsWith("折")) {
			if (remark.length() == 1) {
				return;
			} else {
				remark = remark.substring(1).trim();
			}
		}
		baseCarFinder.setResult_remark(Utils.removeRemarkIllegalHeader(remark.trim()));
	}
	
}
