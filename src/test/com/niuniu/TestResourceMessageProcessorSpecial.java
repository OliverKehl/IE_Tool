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
			Assert.assertEquals(2017, cr.getYear());
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
}
