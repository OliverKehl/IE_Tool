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

public class TestResourceMessageProcessorSpecial {

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

	/*
	 * 如果信息中没有显式的规格，那么就会默认先去中规国产中匹配
	 * 如果匹配失败，则尝试在平行进口车型库中匹配
	 * 而中轨国产和平行进口对于数字的敏感度不同
	 * 如果价格不加显式的后缀，例如"万"或者"W"
	 * 如果是平行进口，且数字在颜色或者OTHER之后，则表明该数字是价格，不能进sub_query用于车型匹配
	 */
	@Test
	public void testRetry() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"揽胜行政 17款 3.0 汽油 HSE/白米/138现车");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("揽胜行政3.0汽油", cr.getCar_model_name());
			Assert.assertEquals("138.0", cr.getDiscount_content());
		}
	}
	
	/*
	 * 
	 */
	@Test
	public void testDualLine() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款宝马中东X5黑黑 \\n 17款，19寸M轮毂，黑色真皮内饰");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("宝马", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("X5", cr.getCar_model_name());
			Assert.assertEquals("中东", cr.getStandard_name());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"宝马320 3259 下25000 \\n 17款，19寸M轮毂，黑色真皮内饰");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("宝马", cr.getBrand_name());
			Assert.assertEquals(2018, cr.getYear());
			Assert.assertEquals("3系", cr.getCar_model_name());
			Assert.assertEquals("32.59", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17欧版汽油揽运S版\\n白黑，20轮，全景，Led 氙灯，雾灯，全尺寸备胎，泊车预热82.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("揽胜运动3.0汽油", cr.getCar_model_name());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("82.5", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款美规汽油行政3.0 黑/咖，2448#\\nHSE (14项电动前排座椅、座椅记忆、牛津真皮打孔座椅、前后座椅加热、20寸铝合金轮毂、电动后视镜、全景天窗、LED氙灯、智能卡、前排座椅通风、电吸门 脚感电尾门) 现车手续齐 133万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("揽胜行政3.0汽油", cr.getCar_model_name());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("133.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"老英朗\\n1199 白、棕、红 ⬇️31500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("别克", cr.getBrand_name());
			Assert.assertEquals("11.99", cr.getGuiding_price());
			Assert.assertTrue(cr.getYear()<2017);
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"宝来\\n1198手动白金银 下24000\\n1318白金 下24000\\n1418白 下24000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"XTS \\n17款 3699白 下10万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("凯迪拉克", cr.getBrand_name());
			Assert.assertEquals("36.99", cr.getGuiding_price());
			Assert.assertTrue(cr.getYear()==2017);
		}
	}
	
	/*
	 * 平行进口车一般会包含多行内容，例如：
	 * "17款加版奔驰GLE43 Coupe 白/黑\\n高级驾驶驶辅助包 现车带关单！"
	 * 这2行内容明显应该对应于一条资源，但是我们这里的逻辑有一定的问题
	 * 第一行识别得到结果以后，把该结果对应的品牌和车型记了下来
	 * 遍历到第二行时，"高级"被识别为MODEL_STYLE，返回结果较多，所以要回溯
	 * 拿到头一行的奔驰和GLE级，再加上高级去进行检索，平行进口车style降级后，就返回了奔驰GLE级的第一条结果，因为没有"高级"对应的信息
	 * 所以这里做处理，如果带过来上一条资源的信息辅助检索，需要判断检索的结果
	 * 例如检索pre_info + query和检索 pre_info的分数相同，则代表该行信息没有生效
	 * 所以不能作为一条资源
	 */
	@Test
	public void testDuallineSpecialCase() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款加版奔驰GLE43 Coupe 白/黑\\n高级驾驶驶辅助包 现车带关单！");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("GLE43", cr.getCar_model_name());
			Assert.assertEquals("高级驾驶驶辅助包 现车带关单!", cr.getRemark());
		}
	}
	
	/*
	 * 有的用户在编辑批量资源的内容时，会使用各种各样奇葩的分隔符，例如：
	 * "x6.838黑棕20.5出"
	 * 我们显然能看出来这是想找指导价为838的X6
	 * 但是分词器把它分为x和6.838
	 * 6.838被当做指导价进入下一阶段
	 * 而显然不可能有3位小数位的指导价，所以在判断token的属性时，添加判断小数是不是合法小数的分支
	 */
	@Test
	public void testInvalidLine() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"x6.838黑棕20.5出");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"黑/黑75.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
	}
	
	/*
	 * 
	 */
	@Test
	public void testSpecialStopWords() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("特价星脉\\n868硅谷银1.6万配置加2？有户来谈（可以后票）[色][色][色]");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("揽胜星脉", cr.getCar_model_name());
			Assert.assertEquals("86.8", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("13款朗逸1249 特价9.89w");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals(2013, cr.getYear());
			Assert.assertEquals("朗逸", cr.getCar_model_name());
			Assert.assertEquals("12.49", cr.getGuiding_price());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("9.89", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("13款朗逸1249 特价9.89");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals(2013, cr.getYear());
			Assert.assertEquals("朗逸", cr.getCar_model_name());
			Assert.assertEquals("12.49", cr.getGuiding_price());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("9.89", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("13款朗逸1249 特价98900");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("大众", cr.getBrand_name());
			Assert.assertEquals(2013, cr.getYear());
			Assert.assertEquals("朗逸", cr.getCar_model_name());
			Assert.assertEquals("12.49", cr.getGuiding_price());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("9.89", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("17款中东版酷路泽 特价69.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("中东", cr.getStandard_name());
			Assert.assertTrue(cr.getCar_model_name().contains("酷路泽"));
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("69.5", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("17款中东版特价酷路泽 69.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("中东", cr.getStandard_name());
			Assert.assertTrue(cr.getCar_model_name().contains("酷路泽"));
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("69.5", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("18款S60L报价2769白 沙 棕");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("沃尔沃", cr.getBrand_name());
			Assert.assertEquals(2018, cr.getYear());
			Assert.assertEquals("S60L", cr.getCar_model_name());
			Assert.assertEquals("27.69", cr.getGuiding_price());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages("①17中东版汽油行政HSE 报价114.2万\\n白/米 批发价优 \\n黑撞顶 滑动全景天窗，自动启停，五门电吸，19轮，LED大灯雾灯，智能卡脚感电尾门，冰箱，备胎，前加热，前后电眼，倒车影像，四区空调 胎压监测。");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("中东", cr.getStandard_name());
			Assert.assertEquals("揽胜行政3.0汽油", cr.getCar_model_name());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("114.2", cr.getDiscount_content());
		}
	}
	
	/*
	 * 有的用户发资源会搞错指导价，或者厂商最近调整了某个车型的指导价，导致某个车型识别不出来
	 * 以前的逻辑是二话不说就去平行进口车车型库里匹配一把，导致会有bad case出现
	 * 例如 "Q7 9298黑棕 19点"
	 * 因为Q7改了价，导致找不到9298的Q7，最终发出来了一个墨西哥版的Q7，这个逻辑硬伤很明显。。
	 */
	@Test
	public void testWrongGuidingPrice() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"Q7 9298黑棕 19点");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
	}
	
	/*
	 * 没有足够的信息被识别为中规国产，而且也明显不是平行进口，不应该去平行进口车型库中匹配
	 */
	@Test
	public void testInsufficientInfoLine() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"自动运动79800");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"自动运动798");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(0, crg.getResult().size());
		}
	}
	
	@Test
	public void testMultipleResourcePrice() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款锐界 3198棕 2个 下40000");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("福特", cr.getBrand_name());
			Assert.assertEquals("锐界", cr.getCar_model_name());
			Assert.assertEquals("31.98", cr.getGuiding_price());
			Assert.assertEquals("2", cr.getDiscount_way());
			Assert.assertEquals("4.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"120-2898曙光金，埃蓝加2900自动泊车29点");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("宝马", cr.getBrand_name());
			Assert.assertEquals("1系", cr.getCar_model_name());
			Assert.assertEquals("28.98", cr.getGuiding_price());
			Assert.assertEquals("1", cr.getDiscount_way());
			Assert.assertEquals("29.0", cr.getDiscount_content());
		}
	}
}
