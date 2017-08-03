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
public class TestResourceMessageProcessor {

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
	public void testResourceBasic() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"k3 \\n 968白优惠24500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("起亚", cr.getBrand_name());
			Assert.assertEquals("9.68", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"博越\\n1088 优惠 7000 白色");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("吉利汽车", cr.getBrand_name());
			Assert.assertEquals("10.88", cr.getGuiding_price());
		}
	}
	
	@Test
	public void testResourceRemark() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款神行408粽金黑优惠8.8万（16年12月产）");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2016, cr.getYear());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals(
					"16年12月产)",
					cr.getRemark());
		}
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"大众朗逸1249");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("12.49", cr.getGuiding_price());
			Assert.assertEquals(
					"",
					cr.getRemark());
		}
	}
	
	@Test
	public void testResourcePrice() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"瑞纳自动929 77折");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("现代", cr.getBrand_name());
			Assert.assertEquals("瑞纳", cr.getCar_model_name());
			Assert.assertEquals("9.29", cr.getGuiding_price());
			Assert.assertEquals("1", cr.getDiscount_way());
			Assert.assertEquals("23.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"瑞虎7 115900 红 橙 21000\\n123900 蓝 橙 21000\\n16款艾瑞泽5 69900 红 12500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奇瑞", cr.getBrand_name());
			Assert.assertEquals("瑞虎7", cr.getCar_model_name());
			Assert.assertEquals("11.59", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("2.1", cr.getDiscount_content());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("奇瑞", cr.getBrand_name());
			Assert.assertEquals("瑞虎7", cr.getCar_model_name());
			Assert.assertEquals("12.39", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("2.1", cr.getDiscount_content());
			
			cr = crg.getResult().get(2);
			Assert.assertEquals("奇瑞", cr.getBrand_name());
			Assert.assertEquals("艾瑞泽5", cr.getCar_model_name());
			Assert.assertEquals("6.99", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("1.25", cr.getDiscount_content());
		}
	}
	
	@Test
	public void testResourceMultiDigitalToken() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"118 256 白黑 下15000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("宝马", cr.getBrand_name());
			Assert.assertEquals("25.6", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("1.5", cr.getDiscount_content());
			Assert.assertTrue(cr.getStyle_name().contains("118"));
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"朗逸1249 1269 下25000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
	}
	
	@Test
	/*
	 * 测试特殊指导价的车型，例如只有2位数指导价的车，普瑞维亚
	 */
	public void testResourceSpecialGuidingPrice() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"普瑞维亚 \\n 61红下22000出现车（新款");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("普瑞维亚", cr.getCar_model_name());
			Assert.assertEquals("61", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("2.2", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"普瑞维亚 61红下22000出现车（新款");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("普瑞维亚", cr.getCar_model_name());
			Assert.assertEquals("61", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("2.2", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"轩逸 15红下12000 呵呵哒");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("日产", cr.getBrand_name());
			Assert.assertEquals("轩逸", cr.getCar_model_name());
			Assert.assertEquals("15", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("1.2", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"玛莎拉蒂-总裁 146蓝棕，特价116万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("玛莎拉蒂", cr.getBrand_name());
			Assert.assertEquals("总裁", cr.getCar_model_name());
			Assert.assertEquals("146", cr.getGuiding_price());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("116.0", cr.getDiscount_content());
		}
		
		/*
		 * 轩逸15万这个指导价实在太过特殊了，为了解决更常见的模式，先把这个扔一边
		 * 后续对这个case额外特殊处理
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"轩逸 \\n 15红下12000 呵呵哒");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("日产", cr.getBrand_name());
			Assert.assertEquals("轩逸", cr.getCar_model_name());
			Assert.assertEquals("15", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("1.2", cr.getDiscount_content());
		}
		*/
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
	}

	@Test
	/*
	 * 在进行规格判定时，我们把一些特殊的token，例如公羊，1500，4500，2700等信息作为平行进口车的佐证
	 * 但是这里没有考虑到下1500或者加4500的情况，所以要额外做处理，把这些特殊数字前有价格相关的信息给过滤掉，
	 * 即如果是下4500，那么这个车不能作为平行进口车的佐证
	 * 
	 * 或者如果4500之前是颜色，即没有显式的指定是下多少钱或者多少点，但是在颜色后的数字基本都是下多少或者加多少钱
	 */
	public void testResourceSpecialStandardInfo() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"汉兰达2878白黑 下4500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("汉兰达", cr.getCar_model_name());
			Assert.assertEquals("28.78", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("0.45", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"汉兰达2878白黑4500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("汉兰达", cr.getCar_model_name());
			Assert.assertEquals("28.78", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("0.45", cr.getDiscount_content());
		}
	}
	
	@Test
	public void testResourceSpecialHeader() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"5：帕萨特指导价211900元↓46100");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("帕萨特", cr.getCar_model_name());
			Assert.assertEquals("21.19", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("4.61", cr.getDiscount_content());
		}
	}
}
