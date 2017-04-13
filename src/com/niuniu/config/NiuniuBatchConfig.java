package com.niuniu.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.math.NumberUtils;

public class NiuniuBatchConfig {

	// 默认的配置文件路径
	public final String DEFAULT_PATH = "com/niuniu/config/config.xml";
	
	public final String TOKEN_TAG_MODEL = "com/niuniu/config/tags.m";
	
	public final String PRICE_REFERENCE_MODEL = "com/niuniu/config/base_car_price_reference";

	private static final NiuniuBatchConfig NIUNIU_BATCH_CONFIG;

	private Properties props;

	private final String default_solr_host = "http://115.29.240.213:8983/solr/";
	private final String default_solr_core = "niuniu_basecars";
	private final String default_redis_host = "127.0.0.1";
	private final String default_redis_port = "6379";
	private final String default_redis_password = "";
	private final String default_redis_index = "6";
	private final String default_enable_cache = "false";
	private final String default_expired_seconds = "3600";

	public static String getSolrHost() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("solr_host", NIUNIU_BATCH_CONFIG.default_solr_host);
	}
	
	public static String getSolrCore() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("solr_core", NIUNIU_BATCH_CONFIG.default_solr_core);
	}
	
	public static String getRedisHost() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("redis_host", NIUNIU_BATCH_CONFIG.default_redis_host);
	}
	
	public static int getRedisPort() {
		String str = NIUNIU_BATCH_CONFIG.props.getProperty("redis_port", NIUNIU_BATCH_CONFIG.default_redis_port);
		return NumberUtils.toInt(str,0);
	}
	
	public static String getRedisPassword() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("redis_password", NIUNIU_BATCH_CONFIG.default_redis_password);
	}
	
	public static int getRedisIndex() {
		String str = NIUNIU_BATCH_CONFIG.props.getProperty("redis_index", NIUNIU_BATCH_CONFIG.default_redis_index);
		return NumberUtils.toInt(str,0);
	}
	
	public static String getEnableCache() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("enable_cache", NIUNIU_BATCH_CONFIG.default_enable_cache);
	}
	
	public static int getExpiredSeconds() {
		String str = NIUNIU_BATCH_CONFIG.props.getProperty("expired_seconds", NIUNIU_BATCH_CONFIG.default_expired_seconds);
		return NumberUtils.toInt(str,0);
	}
	
	public static String getTokenTagModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("token_tag_model", NIUNIU_BATCH_CONFIG.TOKEN_TAG_MODEL);
	}
	
	public static String getPriceReferenceModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("price_reference_model", NIUNIU_BATCH_CONFIG.PRICE_REFERENCE_MODEL);
	}

	static {
		NIUNIU_BATCH_CONFIG = new NiuniuBatchConfig();
	}

	private NiuniuBatchConfig() {
		props = new Properties();
		InputStream input = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_PATH);
		if (input != null) {
			try {
				props.loadFromXML(input);
			} catch (InvalidPropertiesFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args){
		System.out.println(NiuniuBatchConfig.getSolrHost());
	}
}
