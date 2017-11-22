package com.niuniu.classifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niuniu.Utils;
import com.niuniu.config.NiuniuBatchConfig;

public class PriceValidationClassifier {
	public final static Logger log = LoggerFactory.getLogger(PriceValidationClassifier.class);
	Map<String, PriceTrend> priceTrends;//行情价信息
	Map<String, PriceThreshold> priceThresholds;//价格偏差容忍区间
	
	private class PriceThreshold {
		public float price_upper;// 行情价偏差上限
		public float price_lower;// 行情价偏差下限
		public float base_price_upper;// 指导价偏差上限
		public float base_price_lower;// 指导价偏差下限

		private PriceThreshold(float price_lower, float price_upper, float base_price_lower,
				float base_price_upper) {
			this.price_lower = price_lower;
			this.price_upper = price_upper;
			this.base_price_lower = base_price_lower;
			this.base_price_upper = base_price_upper;
		}
	}
	
	private class PriceTrend {
		public float price25;// 行情价-low
		public float price50;// 行情价-medium
		public float price75;// 行情价-high

		private PriceTrend(float price25, float price50, float price75) {
			this.price25 = price25;
			this.price50 = price50;
			this.price75 = price75;
		}
	}

	private static final PriceValidationClassifier singleton;

	static {
		singleton = new PriceValidationClassifier();
	}

	private boolean initPriceReferences(){
		InputStream is = null;
		BufferedReader reader = null;
		priceTrends = new HashMap<String, PriceTrend>();
		try {
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getPriceReferenceModel());
			if (is == null) {
				return false;
			}

			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;
				String[] arrs = line.split("\t");
				if (arrs.length != 4)
					continue;
				PriceTrend pt = new PriceTrend(NumberUtils.toFloat(arrs[1]),
											   NumberUtils.toFloat(arrs[2]),
											   NumberUtils.toFloat(arrs[3]));
				priceTrends.put(arrs[0], pt);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	private boolean initPriceThresholds(){
		InputStream is = null;
		BufferedReader reader = null;
		priceThresholds = new HashMap<String, PriceThreshold>();
		try {
			is = Utils.openResource(this.getClass().getClassLoader(), NiuniuBatchConfig.getPriceThresholdModel());//TODO
			if (is == null) {
				//log.error("[batch_processor]\t {} \t车辆行情价文件不存在", NiuniuBatchConfig.getParallelPriceModel());
				return false;
			}

			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;
				String[] arrs = line.split("\t");
				if (arrs.length < 5)
					continue;
				PriceThreshold pt = new PriceThreshold(NumberUtils.toFloat(arrs[1]), 
													   NumberUtils.toFloat(arrs[2]), 
													   NumberUtils.toFloat(arrs[3]), 
													   NumberUtils.toFloat(arrs[4]));
				priceThresholds.put(arrs[0], pt);
			}
			//log.info("[batch_processor]\t {} \t平行进口车价格正则表达式初始化完成", NiuniuBatchConfig.getParallelPriceModel());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(is != null){
                    is.close();
                    is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	private PriceValidationClassifier() {
		boolean status = initPriceReferences();
		if(!status){
			log.error("[batch_processor]\t {} \t车辆行情价文件不存在", NiuniuBatchConfig.getPriceReferenceModel());
			return;
		}
		log.info("[batch_processor]\t {} \t车辆行情价初始化完成", NiuniuBatchConfig.getPriceReferenceModel());
		
		status = initPriceThresholds();
		if(!status){
			log.error("[batch_processor]\t {} \t车辆价格波动区间文件不存在", NiuniuBatchConfig.getPriceThresholdModel());
		}
		log.info("[batch_processor]\t {} \t车辆价格波动区间初始化完成", NiuniuBatchConfig.getPriceThresholdModel());
	}

	private static float calcPriceBias(float expected_price, float guiding_price, boolean use_abs) {
		if(use_abs)
			return Utils.round(Math.abs(expected_price - guiding_price) / guiding_price, 2);
		return Utils.round((expected_price - guiding_price) / guiding_price, 2);
		
	}
	
	//TODO
	public static int predict(String base_car_id, float real_price, float base_price) {
		PriceThreshold priceThreshold = singleton.priceThresholds.get(base_car_id);
		PriceTrend priceTrend = singleton.priceTrends.get(base_car_id);
		if(priceThreshold==null)
			priceThreshold = singleton.priceThresholds.get("0");
		if(priceTrend!=null){
			float low = priceTrend.price25;
			float medium = priceTrend.price50;
			float high = priceTrend.price75;
			float threshold = Math.min(Math.abs(priceThreshold.price_lower), Math.abs(priceThreshold.price_upper));
			threshold /= 5.0f;
			float diff = Math.min(calcPriceBias(real_price, low, true), Math.min(calcPriceBias(real_price, medium, true), calcPriceBias(real_price, high, true)));
			diff *= 100;
			if( diff < threshold){
				return 1;
			}else if(diff<Math.min(Math.abs(priceThreshold.price_lower), Math.abs(priceThreshold.price_upper))){
				//这个价格能过关，但是还需要看看后面还有没有价格的token
				return 0;
			}else{
				//价格偏离行情价较多，那么就电议好了
				return -1;
			}
		}
		float tmp = calcPriceBias(real_price, base_price, false);
		tmp *= 100;
		if(tmp>=priceThreshold.base_price_lower/3 && tmp<=priceThreshold.base_price_upper/3)
			return 1;
		else if(tmp>=priceThreshold.base_price_lower && tmp<=priceThreshold.base_price_upper)
			return 0;
		else
			return -1;
	}

	public static void main(String[] args) {
		System.out.println(PriceValidationClassifier.predict("9806", 26.29f, 31.98f));
	}
}
