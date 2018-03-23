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
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"凯美瑞\\n2058黑下3.1w出现车\\n2598白下4.9w出现车\\n2598黑下4.9w出现车");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("凯美瑞", cr.getCar_model_name());
			Assert.assertEquals("20.58", cr.getGuiding_price());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("凯美瑞", cr.getCar_model_name());
			Assert.assertEquals("25.98", cr.getGuiding_price());
			
			cr = crg.getResult().get(2);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("凯美瑞", cr.getCar_model_name());
			Assert.assertEquals("25.98", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"759白米+33 9月中旬");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"埃尔法 \\n 759白米+33 9月中旬");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("埃尔法", cr.getCar_model_name());
			Assert.assertEquals("75.9", cr.getGuiding_price());
		}
		
		// 显式的"指导价"
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款SVR路虎5.0V8指导价229.8万，橙黑，，现特价180万，店车店票，裸车，走全国，全新车，正常公里数.17538358260");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("229.8", cr.getGuiding_price());
			Assert.assertTrue(cr.getStyle_name().contains("SVR"));
			Assert.assertEquals("180.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"凯绅\\n1398白金下4000\\n1498（1.6T)白下4000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("起亚", cr.getBrand_name());
			Assert.assertEquals("凯绅", cr.getCar_model_name());
			Assert.assertEquals("13.98", cr.getGuiding_price());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("起亚", cr.getBrand_name());
			Assert.assertEquals("凯绅", cr.getCar_model_name());
			Assert.assertEquals("14.98", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"一汽大众特价出 全部现车\\n[爱心][爱心][爱心]\\n宝来\\n1198手动白金银 下24000\\n1318白金 下24000\\n1418白 下24000\\n");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("宝来", cr.getCar_model_name());
			Assert.assertEquals("11.98", cr.getGuiding_price());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("宝来", cr.getCar_model_name());
			Assert.assertEquals("13.18", cr.getGuiding_price());
			
			cr = crg.getResult().get(2);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("宝来", cr.getCar_model_name());
			Assert.assertEquals("14.18", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"出C200大标 3538 白黑 新款 现车 手慢无");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("35.38", cr.getGuiding_price());
			Assert.assertTrue(cr.getStyle_name().contains("C200"));
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"LS500h \\n9580 银棕");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals("LS", cr.getCar_model_name());
			Assert.assertEquals("95.8", cr.getGuiding_price());
			Assert.assertTrue(cr.getStyle_name().contains("500h"));
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
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"猛禽6218蓝色  红色  银色 加价10万  现车手续齐");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("福特", cr.getBrand_name());
			Assert.assertTrue(cr.getCar_model_name().contains("F150"));
			Assert.assertEquals("62.18", cr.getGuiding_price());
			Assert.assertEquals("3", cr.getDiscount_way());
			Assert.assertEquals("10.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"途安\\n2198 蓝*5 下33000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("途安", cr.getCar_model_name());
			Assert.assertEquals("21.98", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("3.3", cr.getDiscount_content());
			Assert.assertTrue(cr.getColors().contains("蓝"));
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"Lx570 白车红内1438现车一台");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals("LX", cr.getCar_model_name());
			Assert.assertEquals("143.8", cr.getGuiding_price());
			Assert.assertTrue(cr.getColors().contains("白"));
			Assert.assertTrue(cr.getColors().contains("红"));
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"[爱情]传祺 GA5：1993白色优惠5.2折");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("广汽传祺", cr.getBrand_name());
			Assert.assertEquals("传祺GA5", cr.getCar_model_name());
			Assert.assertEquals("19.93", cr.getGuiding_price());
			Assert.assertEquals("1", cr.getDiscount_way());
			Assert.assertEquals("48.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"118 2048下24.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("宝马", cr.getBrand_name());
			Assert.assertEquals("1系", cr.getCar_model_name());
			Assert.assertEquals("20.48", cr.getGuiding_price());
			Assert.assertEquals("1", cr.getDiscount_way());
			Assert.assertEquals("24.5", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"速派2498 灰（18款）38000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("斯柯达", cr.getBrand_name());
			Assert.assertEquals("速派", cr.getCar_model_name());
			Assert.assertEquals("24.98", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("3.8", cr.getDiscount_content());
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
			//Assert.assertEquals("2", cr.getDiscount_way());
			//Assert.assertEquals("1.5", cr.getDiscount_content());
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
					"普瑞维亚 \\n 61红下22000出现车");
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
					"普瑞维亚 61红下22000出现车");
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
					"玛莎拉蒂-总裁 154.88蓝棕，特价116万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("玛莎拉蒂", cr.getBrand_name());
			Assert.assertEquals("总裁", cr.getCar_model_name());
			Assert.assertEquals("154.88", cr.getGuiding_price());
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
			//Assert.assertEquals("2", cr.getDiscount_way());
			//Assert.assertEquals("0.45", cr.getDiscount_content());
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
			Assert.assertEquals("3", cr.getDiscount_way());
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
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"5）帕萨特指导价211900元↓46100");
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
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"朗逸\\n12.49 13款黑黑 下25000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals("朗逸", cr.getCar_model_name());
			Assert.assertEquals("12.49", cr.getGuiding_price());
			//Assert.assertEquals("2", cr.getDiscount_way());
			//Assert.assertEquals("2.5", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【1】17款道奇挑战者SXT （黑/黑）8速自动档，赛道包（加大前卡钳，性能方向盘，运动悬挂，哑光20黑轮毂，拨片换挡），一键启动，加热方向盘，电眼，倒车影像，并到辅助，后视镜加热，多媒体显示屏，丝绒座椅，胎压监测，蓝牙，双尾排，埃尔派高级音响");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("道奇", cr.getBrand_name());
			Assert.assertEquals("挑战者", cr.getCar_model_name());
			Assert.assertEquals(2017, cr.getYear());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"1.A180 236 白／黑 下3.6");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("a级", cr.getCar_model_name().toLowerCase());
			Assert.assertEquals("23.6", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("3.6", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"2.GLA200 2718 灰黑 -1.5裸");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("gla", cr.getCar_model_name().toLowerCase());
			Assert.assertEquals("27.18", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("1.5", cr.getDiscount_content());
		}
	}
	
	@Test
	public void testResourceLaterYearInfo() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【发现神行】\\n408白/黑-9.8万(新款)\\n468白/黑-13万(17款)");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("9.8", cr.getDiscount_content());
			Assert.assertEquals(2018, cr.getYear());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("46.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals(2017, cr.getYear());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【发现神行】\\n408白/黑-9.8万(17款)\\n468白/黑-13万(18款)");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("9.8", cr.getDiscount_content());
			Assert.assertEquals(2017, cr.getYear());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("46.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals(2018, cr.getYear());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【17款发现神行】\\n408白/黑-9.8万\\n468白/黑-13万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("9.8", cr.getDiscount_content());
			Assert.assertEquals(2017, cr.getYear());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("46.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals(2017, cr.getYear());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【新款发现神行】\\n408白/黑-9.8万\\n468白/黑-13万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("9.8", cr.getDiscount_content());
			Assert.assertTrue(cr.getYear()>2016);
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("46.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertTrue(cr.getYear()>2016);
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【新款发现神行】\\n408白/黑-9.8万 17款\\n468白/黑-13万 16款");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("9.8", cr.getDiscount_content());
			Assert.assertEquals(2017, cr.getYear());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("46.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals(2016, cr.getYear());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"【新款发现神行】\\n408白/黑-9.8万 2017款\\n468白/黑-13万 2018款");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("40.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("9.8", cr.getDiscount_content());
			Assert.assertEquals(2017, cr.getYear());
			
			cr = crg.getResult().get(1);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("发现神行", cr.getCar_model_name());
			Assert.assertEquals("46.8", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("13.0", cr.getDiscount_content());
			Assert.assertEquals(2018, cr.getYear());
		}
	}
	
	@Test
	public void testResourceCache() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setDisableCache(false);
			rmp.setMessages(
					"【发现神行】\\n408白/黑-9.8万(新款)\\n468白/黑-13万(17款)");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setDisableCache(false);
			rmp.setMessages(
					"【发现神行】\\n408白/黑-9.8万(新款)\\n468白/黑-13万(17款)");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setDisableCache(false);
			rmp.setMessages(
					"最新 现货 手续齐全 🉐️🉐️\\n【发现神行】\\n378白/黑-8.4万\\n368黑/黑-8.2万(17款)\\n408红/黑-9.7万(18款)\\n408斯灰/黑 黑/黑-9.6万(18款)\\n408白/黑-10.5万(17款)\\n408凯灰/黑 红/黑-10.1万(17款)\\n468白/黑-14万(17款)\\n【揽胜极光】\\n458红/黑 黑/黑-13.8万(17款英伦版)\\n458白/黑-12.6万(18款英伦版)\\n458凯灰/黑 红/黑 黑/黑-12.2万(18款英伦版)🔥\\n【揽胜运动】\\n928白/黑-8.5万(2.2万配置)\\n968白/黑 黑/黑(新能源)🔥🔥\\n1078白/浅褐 黑/浅褐🔥🔥\\n【揽胜行政】\\n1458黑/黑 白/黑-30.3万\\n1658黑/黑(北京现货)\\n2678黑/黄褐-35万(五座) 🔥🔥\\n2678黑/干椒红(四座) 电议🔥🔥\\n3328黑/黄褐 黑/樱桃红 电议🔥🔥\\n【XFL】\\n458黑/棕 卢蓝/棕-10.2万\\n458剧院红/棕-10.2万\\n498白/棕-11.8万\\n498剧院红/棕-11.5万\\n【捷豹FPACE】\\n528水晶蓝 (45000配置)\\n528卢蓝/黑-10万\\n548中国红/黑-11万\\n548卢兰/黑 中国红/黑-10万(45000配置)\\n628白/黑红-13.6万\\n赵珂伟 tel：13301339220");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(27, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setDisableCache(false);
			rmp.setMessages(
					"最新 现货 手续齐全 🉐️🉐️\\n【发现神行】\\n378白/黑-8.4万\\n368黑/黑-8.2万(17款)\\n408红/黑-9.7万(18款)\\n408斯灰/黑 黑/黑-9.6万(18款)\\n408白/黑-10.5万(17款)\\n408凯灰/黑 红/黑-10.1万(17款)\\n468白/黑-14万(17款)\\n【揽胜极光】\\n458红/黑 黑/黑-13.8万(17款英伦版)\\n458白/黑-12.6万(18款英伦版)\\n458凯灰/黑 红/黑 黑/黑-12.2万(18款英伦版)🔥\\n【揽胜运动】\\n928白/黑-8.5万(2.2万配置)\\n968白/黑 黑/黑(新能源)🔥🔥\\n1078白/浅褐 黑/浅褐🔥🔥\\n【揽胜行政】\\n1458黑/黑 白/黑-30.3万\\n1658黑/黑(北京现货)\\n2678黑/黄褐-35万(五座) 🔥🔥\\n2678黑/干椒红(四座) 电议🔥🔥\\n3328黑/黄褐 黑/樱桃红 电议🔥🔥\\n【XFL】\\n458黑/棕 卢蓝/棕-10.2万\\n458剧院红/棕-10.2万\\n498白/棕-11.8万\\n498剧院红/棕-11.5万\\n【捷豹FPACE】\\n528水晶蓝 (45000配置)\\n528卢蓝/黑-10万\\n548中国红/黑-11万\\n548卢兰/黑 中国红/黑-10万(45000配置)\\n628白/黑红-13.6万\\n赵珂伟 tel：13301339220");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(27, crg.getResult().size());
		}
	}
}
