package com.niuniu.test;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.niuniu.CarResource;
import com.niuniu.CarResourceGroup;
import com.niuniu.ResourceMessageProcessor;
import com.niuniu.USolr;
import com.niuniu.config.NiuniuBatchConfig;

import junit.framework.Assert;

public class TestParallelResourceMessageProcessor {

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
	public void testParallelResourecBasic() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款中东gle400，九速 全景 灯光包 停车辅助包（自动泊车、前后电眼、车道偏离预警）前排电动座椅➕记忆➕4项腰部支撑 自动防炫目内外后视镜 主动刹车辅助 遮阳帘 温控杯架 吸烟包 自动折叠后视镜 倒影 自动空调 发光迎宾脚踏 镀铬内饰 脚踏 大屏 氛围灯 电尾门 智能卡 一键启动 双排气管 19轮 多路况模式 LOGO地射灯\n\n☎️：15822736077");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("GLE400", cr.getCar_model_name());
			Assert.assertEquals(
					"九速 全景 灯光包 停车辅助包(自动泊车、前后电眼、车道偏离预警)前排电动座椅 加 记忆 加 4项腰部支撑 自动防炫目内外后视镜 主动刹车辅助 遮阳帘 温控杯架 吸烟包 自动折叠后视镜 倒影 自动空调 发光迎宾脚踏 镀铬内饰 脚踏 大屏 氛围灯 电尾门 智能卡 一键启动 双排气管 19轮 多路况模式 LOGO地射灯\n:15822736077",
					cr.getRemark());
		}
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款加版坦途1794 黑棕 \n配置：天窗 并道辅助 真皮座椅加热 通风 USB蓝牙 大屏 JBL音响 倒影 雷达 巡航 防侧滑 多功能方向盘 后视镜加热 LED日行灯 大灯高度调节 桃木内饰 字标扶手箱 后货箱内衬 20寸轮毂 主副驾驶电动调节 后挡风玻璃自动升降 自动恒温空调 电动折叠后视镜\n现车手续齐\n电话：15822736077\n");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("加版", cr.getStandard_name());
			Assert.assertEquals("坦途", cr.getCar_model_name());
			Assert.assertEquals("[黑#棕色]", cr.getColors());
			Assert.assertTrue(cr.getStyle_name().contains("1794"));
			Assert.assertEquals(
					"配置:天窗 并道辅助 真皮座椅加热 通风 USB蓝牙 大屏 JBL音响 倒影 雷达 巡航 防侧滑 多功能方向盘 后视镜加热 LED日行灯 大灯高度调节 桃木内饰 字标扶手箱 后货箱内衬 20寸轮毂 主副驾驶电动调节 后挡风玻璃自动升降 自动恒温空调 电动折叠后视镜\n现车手续齐\n电话:15822736077",
					cr.getRemark());
		}
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"1⃣️2⃣️ 17款加版GLS450 #6621 黑/黑 金属漆 豪华包 运动包 灯光包 驾驶员辅助包 脚踏板 行李架 6月底交车131万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("加版", cr.getStandard_name());
			Assert.assertEquals("GLS450", cr.getCar_model_name());
			//Assert.assertEquals("[黑#棕色]", cr.getColors());
			Assert.assertEquals("期货", cr.getResource_type());
			Assert.assertEquals( "131.0", cr.getDiscount_content());
			Assert.assertEquals(
					"金属漆 豪华包 运动包 灯光包 驾驶员辅助包 脚踏板 行李架 6月底交车131万",
					cr.getRemark());
		}
	}
	
	@Test
	public void testParallelResourecVin() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款公羊1500皮卡6545# 6548# 蓝 /棕 手续齐 （新航库、）45.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("加版", cr.getStandard_name());
			Assert.assertEquals("道奇", cr.getBrand_name());
			Assert.assertEquals("公羊1500", cr.getCar_model_name());
			Assert.assertEquals("6545", cr.getVin());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("45.5", cr.getDiscount_content());
		}
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款加版Q7（8418）金米 手续齐");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("加版", cr.getStandard_name());
			Assert.assertEquals("奥迪", cr.getBrand_name());
			Assert.assertEquals("Q7", cr.getCar_model_name());
			Assert.assertEquals("8418", cr.getVin());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("[金#米色]", cr.getColors());
			Assert.assertEquals("手续齐", cr.getRemark());
		}
	}
	
	@Test
	public void testResourceStyle(){
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"[耶][耶]17款霸道2700白米 黎巴嫩版 今天现车 43万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("中东", cr.getStandard_name());
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("霸道2700", cr.getCar_model_name());
			Assert.assertEquals("", cr.getVin());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("43.0", cr.getDiscount_content());
			Assert.assertEquals("[白#米色]", cr.getColors());
			Assert.assertEquals("今天现车 43万", cr.getRemark());
			Assert.assertEquals("38706", cr.getId());
			Assert.assertTrue(cr.getStyle_name().contains("黎巴嫩"));
		}
	}
	
	@Test
	public void testResourceImplicitStandard(){
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款揽运HSE版汽油 白黑3台 \n 7830# 8513# 7855# 滑动天窗19轮 真皮方向盘 16项座椅电动调节 后视镜自动防眩目 前挡风加热 前雾灯 LED氙灯带大灯清洗 车道偏离警示 电尾 倒影 倒车助手 前后侧身隔热防噪音玻璃 现车90万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("欧版", cr.getStandard_name());
			Assert.assertEquals(2, cr.getStandard());
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("揽胜运动3.0汽油", cr.getCar_model_name());
			Assert.assertEquals("8513", cr.getVin());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("90.0", cr.getDiscount_content());
			Assert.assertEquals("3台\n7830  7855# 滑动天窗19轮 真皮方向盘 16项座椅电动调节 后视镜自动防眩目 前挡风加热 前雾灯 LED氙灯带大灯清洗 车道偏离警示 电尾 倒影 倒车助手 前后侧身隔热防噪音玻璃 现车90万", cr.getRemark());
		}
	}
	
	@Test
	public void testResourceRemark() {
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
}
