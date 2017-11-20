package test.com.niuniu;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.niuniu.CarResource;
import com.niuniu.CarResourceGroup;
import com.niuniu.ResourceMessageProcessor;
import com.niuniu.USolr;

import junit.framework.Assert;

/*
 *  中规、国产车的测试用例
 */
public class TestNewColorExtractor {

	USolr solr_client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testResourceColor() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"LX 1438000外黑内红现车一台");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals("143.8", cr.getGuiding_price());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("[黑色#红色]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"A4 2998 白15下500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奥迪", cr.getBrand_name());
			Assert.assertEquals("29.98", cr.getGuiding_price());
			Assert.assertEquals("[白#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款艾瑞泽5 69900 下12500红");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奇瑞", cr.getBrand_name());
			Assert.assertEquals("6.99", cr.getGuiding_price());
			Assert.assertEquals("1.25", cr.getDiscount_content());
			Assert.assertEquals("[魅影红#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"加版GLS450 水硅钒钙石蓝 黄鹤 豪华 运动 通风 三区 小牛皮 #3919 报关中\\n18622251821 迟庆华	");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("[蓝#黄鹤]", cr.getColors());
		}
	}
	
	@Test
	public void testAdjacentColor(){
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"揽运1198白黄鹤，黑黄鹤优惠13出");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("119.8", cr.getGuiding_price());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals("[法拉隆黑#黄鹤, 富士白#黄鹤]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"别克全新一代君威\\n199800 白金🔻7500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("别克", cr.getBrand_name());
			Assert.assertEquals("19.98", cr.getGuiding_price());
			Assert.assertEquals("0.75", cr.getDiscount_content());
			Assert.assertEquals("[幻白#金]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"别克全新一代君威\\n199800 白金红🔻7500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("别克", cr.getBrand_name());
			Assert.assertEquals("19.98", cr.getGuiding_price());
			Assert.assertEquals("0.75", cr.getDiscount_content());
			Assert.assertEquals("[幻白#, 玛瑙红#, 恒金#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"揽胜2678黑红，黑黄，白黄鹤");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("267.8", cr.getGuiding_price());
			Assert.assertEquals("[富士白#黄鹤, 法拉隆黑#黄, 法拉隆黑#红色]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"极光458黑黑白黑优惠11.3小期");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("45.8", cr.getGuiding_price());
			Assert.assertEquals("[富士白#黑色, 圣托里尼黑#黑色]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"极光458黑黑白优惠11.3小期");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("45.8", cr.getGuiding_price());
			Assert.assertEquals("[圣托里尼黑#黑色, 富士白#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"别克全新一代君威\\n199800 红白金🔻7500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("别克", cr.getBrand_name());
			Assert.assertEquals("19.98", cr.getGuiding_price());
			Assert.assertEquals("0.75", cr.getDiscount_content());
			Assert.assertEquals("[幻白#, 玛瑙红#, 恒金#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款塞纳四驱LE 黑/ 灰 48.7万  白/灰 49万 手续齐全 当天票。");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("塞纳", cr.getCar_model_name());
			Assert.assertEquals("48.7", cr.getDiscount_content());
			Assert.assertEquals("[黑色#灰色]", cr.getColors());
		}
	}
	
	@Test
	public void testWithoutAdjacentColor(){
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"揽运1198白/黄鹤，黑/黄鹤优惠13出");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("119.8", cr.getGuiding_price());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals("[法拉隆黑#黄鹤, 富士白#黄鹤]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"别克全新一代君威\\n199800 白 金 红🔻7500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("别克", cr.getBrand_name());
			Assert.assertEquals("19.98", cr.getGuiding_price());
			Assert.assertEquals("0.75", cr.getDiscount_content());
			Assert.assertEquals("[幻白#, 玛瑙红#, 恒金#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"别克全新一代君威\\n199800 白 金🔻7500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("别克", cr.getBrand_name());
			Assert.assertEquals("19.98", cr.getGuiding_price());
			Assert.assertEquals("0.75", cr.getDiscount_content());
			Assert.assertEquals("[幻白#, 恒金#]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"揽胜2678黑/红，黑/黄，白/黄鹤");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("267.8", cr.getGuiding_price());
			Assert.assertEquals("[富士白#黄鹤, 法拉隆黑#黄, 法拉隆黑#红色]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"极光458黑黑 白黑优惠11.3小期");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("45.8", cr.getGuiding_price());
			Assert.assertEquals("[富士白#黑色, 圣托里尼黑#黑色]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"极光458黑/黑 白黑优惠11.3小期");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("45.8", cr.getGuiding_price());
			Assert.assertEquals("[富士白#黑色, 圣托里尼黑#黑色]", cr.getColors());
		}
	}

	@Test
	public void testExplicitResourceColor() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"RX450H白车棕内，黄鹤，黄水晶棕内869现车售全国");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals("86.9", cr.getGuiding_price());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("[白#棕色, 黄#水晶棕]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"RX450H白车棕内，黄水晶棕内869现车售全国");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals("86.9", cr.getGuiding_price());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("[白#棕色, 黄#水晶棕]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"RX450H 869白扯棕内，黄水晶棕内现车售全国");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals("86.9", cr.getGuiding_price());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("[黄#水晶棕]", cr.getColors());
		}
	}
}
