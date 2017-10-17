package com.niuniu.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NiuniuBatchConfig {

	public final static Logger log = LoggerFactory.getLogger(NiuniuBatchConfig.class);
	// 默认的配置文件路径
	public final String TOKEN_REPLACE_FILE = "com/niuniu/config/token_replace_file";
	
	public final String DEFAULT_PATH = "com/niuniu/config/config.xml";
	
	public final String TOKEN_TAG_MODEL = "com/niuniu/config/tags.m";
	
	public final String RESOURCE_TYPE_PATTERN = "com/niuniu/config/resource_type.pattern";
	
	public final String PARALLEL_VIN_PATTERN = "com/niuniu/config/parallel_vin.pattern";
	
	public final String PARALLEL_PRICE_PATTERN = "com/niuniu/config/parallel_price.pattern";
	
	public final String PRICE_REFERENCE_MODEL = "com/niuniu/config/base_car_price_reference";
	
	public final String PARALLEL_PRICE_REFERENCE_MODEL = "com/niuniu/config/parallel_base_car_price";
	
	public final String STANDARD_PATTERN = "com/niuniu/config/standard.pattern";

	private static final NiuniuBatchConfig NIUNIU_BATCH_CONFIG;

	private Properties props;

	private final String default_solr_host = "http://115.29.240.213:8983/solr/";
	private final String default_solr_core = "niuniu_basecars";
	private final String default_redis_host = "127.0.0.1";
	private final String default_redis_port = "6379";
	private final String default_redis_password = "";
	private final String default_redis_index = "6";
	private final String default_expired_seconds = "3600";
	
	private boolean enable_cache = true;

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
	
	public static boolean getEnableCache() {
		return NIUNIU_BATCH_CONFIG.enable_cache;
	}
	
	public static int getExpiredSeconds() {
		String str = NIUNIU_BATCH_CONFIG.props.getProperty("expired_seconds", NIUNIU_BATCH_CONFIG.default_expired_seconds);
		return NumberUtils.toInt(str,0);
	}
	
	public static String getTokenTagModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("token_tag_model", NIUNIU_BATCH_CONFIG.TOKEN_TAG_MODEL);
	}
	
	public static String getResourceTypeModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("resource_type_pattern", NIUNIU_BATCH_CONFIG.RESOURCE_TYPE_PATTERN);
	}
	
	public static String getParallelVinModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("parallel_vin_pattern", NIUNIU_BATCH_CONFIG.PARALLEL_VIN_PATTERN);
	}
	
	public static String getParallelPriceModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("parallel_price_pattern", NIUNIU_BATCH_CONFIG.PARALLEL_PRICE_PATTERN);
	}
	
	public static String getPriceReferenceModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("price_reference_model", NIUNIU_BATCH_CONFIG.PRICE_REFERENCE_MODEL);
	}
	
	public static String getTokenReplaceFile(){
		return NIUNIU_BATCH_CONFIG.props.getProperty("token_replace_file", NIUNIU_BATCH_CONFIG.TOKEN_REPLACE_FILE);
	}
	
	public static String getParallelPriceReferenceModel(){
		return NIUNIU_BATCH_CONFIG.props.getProperty("parallel_base_car_price", NIUNIU_BATCH_CONFIG.PARALLEL_PRICE_REFERENCE_MODEL);
	}
	
	public static String getStandardModel() {
		return NIUNIU_BATCH_CONFIG.props.getProperty("standard_pattern", NIUNIU_BATCH_CONFIG.STANDARD_PATTERN);
	}


	static {
		NIUNIU_BATCH_CONFIG = new NiuniuBatchConfig();
	}

	private NiuniuBatchConfig() {
		props = new Properties();
		String separator = System.getProperty("file.separator");
		String target = System.getProperty("catalina.base") + separator + "config.xml";
		log.info(target);
		try{
			InputStream input = openResource(this.getClass().getClassLoader(), target);
			if(input==null){
				log.error("找不到批量发布的配置文件，使用默认配置文件...");
				input = openResource(this.getClass().getClassLoader(), DEFAULT_PATH);
			}
			if (input != null) {
				try {
					props.loadFromXML(input);
					String str = props.getProperty("enable_cache");
					enable_cache = Boolean.parseBoolean(str);
				} catch (InvalidPropertiesFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				log.error("找不到批量发布的配置文件，终止服务");
				System.exit(1);
			}
		}catch (Exception e) {
			log.warn("exception caught when initializing.");
		}
	}
	
	public InputStream openResource(ClassLoader classLoader, String resource) throws IOException {
	    InputStream is=null;
	    try {
	      File f0 = new File(resource);
	      File f = f0;
	      if (f.isFile() && f.canRead()) {
	        return new FileInputStream(f);
	      } else if (f != f0) { // no success with $CWD/$configDir/$resource
	        if (f0.isFile() && f0.canRead())
	          return new FileInputStream(f0);
	      }
	      // delegate to the class loader (looking into $INSTANCE_DIR/lib jars)
	      is = classLoader.getResourceAsStream(resource);
	      if (is == null)
	        return null;
	    } catch (Exception e) {
	      throw new IOException("Error opening " + resource, e);
	    }
	    return is;
	  }
	
	public static void main(String[] args){
		System.out.println(NiuniuBatchConfig.getResourceTypeModel());
	}
}
