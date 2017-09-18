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
					"17款中东gle400，九速 全景 灯光包 停车辅助包（自动泊车、前后电眼、车道偏离预警）前排电动座椅➕记忆➕4项腰部支撑 自动防炫目内外后视镜 主动刹车辅助 遮阳帘 温控杯架 吸烟包 自动折叠后视镜 倒影 自动空调 发光迎宾脚踏 镀铬内饰 脚踏 大屏 氛围灯 电尾门 智能卡 一键启动 双排气管 19轮 多路况模式 LOGO地射灯\\n\\n☎️：15822736077");
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
					"17款加版坦途1794 黑棕 \\n配置：天窗 并道辅助 真皮座椅加热 通风 USB蓝牙 大屏 JBL音响 倒影 雷达 巡航 防侧滑 多功能方向盘 后视镜加热 LED日行灯 大灯高度调节 桃木内饰 字标扶手箱 后货箱内衬 20寸轮毂 主副驾驶电动调节 后挡风玻璃自动升降 自动恒温空调 电动折叠后视镜\\n现车手续齐\\n电话：15822736077\\n");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("加版", cr.getStandard_name());
			Assert.assertEquals("坦途", cr.getCar_model_name());
			Assert.assertEquals("[黑色#棕色]", cr.getColors());
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
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"坦途1794 6548# 蓝 /棕 手续齐 （新航库、）45.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("坦途", cr.getCar_model_name());
			Assert.assertTrue(cr.getStyle_name().contains("1794"));
			Assert.assertEquals("6548", cr.getVin());
		}
	}
	
	@Test
	/*
	 * 平行进口车理论上不存在一定要命中的style，所以在search时要指定search_level=low
	 */
	public void testParallelResourceStyle(){
		/*
		 * style miss
		 */
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款霸道2700 天窗底挂 白米 现车 37.8\\n");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("霸道2700", cr.getCar_model_name());
			Assert.assertFalse(cr.getStyle_name().contains("天窗"));
		}
		
		/*
		 * style hit
		 */
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"坦途1794 #6548 蓝 /棕 手续齐 （新航库、）45.5");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals("坦途", cr.getCar_model_name());
			Assert.assertTrue(cr.getStyle_name().contains("1794"));
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
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款美规 路虎揽胜行政 白/黑 133W\\n车架号：3382\\n配置：3.0T 汽油 乌木顶  HSE包(380马力 14项座椅调节 座椅记忆 20寸轮  可开启全景天窗 自动防眩目车内车外后视镜 打孔牛津真皮座椅 前排座椅通风/加热 后排座椅加热 方向盘加热 前档加热 巡航定速 前后雷达 倒车影像 车道偏离预警 TFT全液晶仪表盘 10英寸中控触摸屏 电吸门 胎压监测 小尺寸备胎 智能卡 脚感应电尾 Meridian 380音响  后隐私玻璃 氛围灯 氙灯Led 感应雨刷) 路虎保护包 现车\\n齐航男：15320100188");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("美规", cr.getStandard_name());
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("3382", cr.getVin());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("133.0", cr.getDiscount_content());
			Assert.assertEquals("[白色#黑色]", cr.getColors());
		}
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款奔驰GLS450 黑/咖 1900#\\n配置：P01 全景 灯光包 外观包 二排进入 无烟煤杨木内饰 轮毂锁 后娱预留。\\n9.20合同，108.5w\\n18617587777\\n18617587777");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("GLS450", cr.getCar_model_name());
			Assert.assertEquals("1900", cr.getVin());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("108.5", cr.getDiscount_content());
			Assert.assertEquals("期货", cr.getResource_type());
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
			Assert.assertEquals("[白色#米色]", cr.getColors());
			Assert.assertEquals("今天现车 43万", cr.getRemark());
			Assert.assertEquals("38706", cr.getId());
			Assert.assertTrue(cr.getStyle_name().contains("黎巴嫩"));
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"2017揽胜行政3.0柴油 黑黑 125");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("揽胜行政3.0柴油", cr.getCar_model_name());
			Assert.assertEquals("[黑色#黑色]", cr.getColors());
		}
	}
	
	@Test
	public void testResourceColor(){
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					" XC90 高配 灰/黑 白/琥珀 20轮 7座 智能卡 电动座椅记忆调节 真皮加热座椅通风 腿托 前后电眼 泊车辅助 液晶仪表盘 大屏导航 雷测 全景天窗 氙灯 LED 电尾门 大灯清洗 四驱空调 NAPPA真皮 后加热 环影 换挡拨片 抬头显示 电动折叠后视镜 桃木内饰 后遮阳帘 并道 真皮仪表台 底盘升降 多轮廓座椅及腰部支撑 全车包围 青铜/琥珀 白/黑 黑黑 ");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("沃尔沃", cr.getBrand_name());
			Assert.assertEquals("XC90", cr.getCar_model_name());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("[白色#琥珀, 灰色#黑色]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款3.0柴油创世短轴 白／黄鹤 黑／黄鹤\\n配置：商务4座，滑动全景天窗，智能卡，氙灯LED，脚感电尾门，电吸门，二代全地形反馈，鹿皮顶，黑色钢琴漆内饰，钢琴漆多功能方向盘带加热，换挡拨片，内外自动防眩目后视镜，牛津打孔真皮座椅，前后排座椅电动调节、加热、通风、按摩带记忆【前排18项，后排14项】，前后飞翼头枕，前后冰箱，大屏导航，液晶仪表盘，四区空调，后娱10.2寸，发光迎宾踏板，多彩氛围灯，825W高级音响19扬声器，外后视镜电动调节带加热、记忆、折叠及倒车自动照地功能，车道偏离预警，并道辅助，前雾灯，大灯清洗，360环影，前后电眼，定速巡航，胎压监测，21轮，全尺寸备胎 143.5万（谈！谈！谈！） \\n免消费税的17款揽胜5.0四座，创世加长版黑/黄鹤（谈！谈！谈！！！！）\\n22轮，4座， 创世包（前后排座椅 通风加热按摩 前冰箱 爱马仕缝制顶棚 创世套件）半苯胺拓展真皮 二代地形反馈 盲点提醒 环车影像 四区空调 26项前排座椅调节 后排电动座椅 前后飞翼头枕 10.2后排娱乐 电吸门脚感尾门 HUD抬头显示 825高级音响，雷达测距系统。现车手续齐 246 大谈");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("揽胜行政3.0柴油", cr.getCar_model_name());
			Assert.assertEquals("[黑色#黄鹤, 白色#黄鹤]", cr.getColors());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"墨版XC90 T6/7座/2.0L/4缸/4驱/12.3寸数字仪表盘/空气净化系统/第三排折叠座椅/4区空调/私密玻璃/Nappa真皮座椅/前部电动调节座椅带腰部支撑带记忆/前排座椅加热/无钥匙进入/自动防炫目内后视镜/电尾门/可选择驾驶模式/倒影/感应雨刷/导航，全景天窗/LED大灯/LED日间行车灯（雷神之锤）/大灯清洗/银色车顶行李架/可伸缩电动外后视镜/双镀铬尾喉/20轮/巡航定速/雷达测距/碰撞保护系统/前后电眼/坡道起步辅助/陡坡缓降控制/胎压监测/Bowers&Wilkins高级音响/低音炮/360度环影/黑色顶棚/抬头显示\\n68万\\n☎️：13911910022 小伍");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("沃尔沃", cr.getBrand_name());
			Assert.assertEquals("XC90", cr.getCar_model_name());
			Assert.assertEquals("[]", cr.getColors());
		}
	}
	
	@Test
	public void testResourceImplicitStandard(){
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款揽运HSE版汽油 白黑3台 \\n 7830# 8513# 7855# 滑动天窗19轮 真皮方向盘 16项座椅电动调节 后视镜自动防眩目 前挡风加热 前雾灯 LED氙灯带大灯清洗 车道偏离警示 电尾 倒影 倒车助手 前后侧身隔热防噪音玻璃 现车90万");
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
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款GLS450 黑/黑 #6738 P01全景 方向盘加热 哈曼 二排电动 照明脚踏 后娱预留 停车辅助 驾驶员辅助 雷测 现车手续齐 111");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals(2, cr.getStandard());
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("GLS450", cr.getCar_model_name());
			Assert.assertEquals("6738", cr.getVin());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("111.0", cr.getDiscount_content());
			Assert.assertEquals("P01全景 方向盘加热 哈曼 二排电动 照明脚踏 后娱预留 停车辅助 驾驶员辅助 雷测 现车手续齐 111", cr.getRemark());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款GLS450 黑/黑 6738  # P01全景 方向盘加热 哈曼 二排电动 照明脚踏 后娱预留 停车辅助 驾驶员辅助 雷测 现车手续齐 111");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals(2, cr.getStandard());
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("GLS450", cr.getCar_model_name());
			Assert.assertEquals("6738", cr.getVin());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("111.0", cr.getDiscount_content());
			Assert.assertEquals("P01全景 方向盘加热 哈曼 二排电动 照明脚踏 后娱预留 停车辅助 驾驶员辅助 雷测 现车手续齐 111", cr.getRemark());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款GLS450黑黑，通体黑\\nP01，全景，方向盘加热，拖钩，哈曼卡顿音响，二排电动，后预留，发光星标，轮毂锁，灯光包，停车辅助包，360度环车影像，自动泊车，电眼，外观包（10副轮毂），发光脚踏板，镀铬套件，手动遮阳帘，冷热杯架，二排座椅加热，现车，手续齐，111\\n☎️15313009876");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals(2, cr.getStandard());
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals("GLS450", cr.getCar_model_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("111.0", cr.getDiscount_content());
		}
	}
	
	@Test
	public void testResourcePrice() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款美规奔驰GLS450 \\n颜色：黑/咖（9498）\\n配置：P01，全景，灯光包，外观包，停车辅助包，方向盘加热，二排电动，哈曼音响，桉木内饰\\n天津现车    远方宏达库\\n价格：113.88万\\n");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奔驰", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("113.88", cr.getDiscount_content());
			Assert.assertEquals(
					"颜色:黑/咖(9498)\n配置:P01,全景,灯光包,外观包,停车辅助包,方向盘加热,二排电动,哈曼音响,桉木内饰\n天津现车 远方宏达库\n价格:113.88万",
					cr.getRemark());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款美规揽胜行政3.0 白/白。0816 HSE 、视觉辅助包、驾驶员辅助包、825W豪华音响包、保护包、可加热方向盘 皓月库现车 138");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2016, cr.getYear());
			Assert.assertEquals("138.0", cr.getDiscount_content());
			Assert.assertEquals("0816", cr.getVin());
			Assert.assertEquals(
					"HSE 、视觉辅助包、驾驶员辅助包、825W豪华音响包、保护包、可加热方向盘 皓月库现车 138",
					cr.getRemark());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款柴油3.0加长创世，黑黄鹤，0835#，22钻石轮，雷达测距，抬显，4座，前冰箱，大号清洗液，镀铬脚踏，盲点检测，双触屏，数字广播，小备胎，全景滑动天窗，后排10.2 \\n 7月15日交车 165w");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("165.0", cr.getDiscount_content());
			Assert.assertEquals("0835", cr.getVin());
			Assert.assertEquals("期货", cr.getResource_type());
			Assert.assertEquals(
					"22钻石轮,雷达测距,抬显,4座,前冰箱,大号清洗液,镀铬脚踏,盲点检测,双触屏,数字广播,小备胎,全景滑动天窗,后排10.2\n7月15日交车 165w",
					cr.getRemark());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"1	飞驰 17款 4.0T V8 S	冰川白/红	279.00	欧版-现车,打税放\\n配置：17款 欧规 宾利飞驰 V8S 4.0T 白红 3081 5座 20轮 前加热 前后电动座椅 智能卡 一键启动 红卡钳 倒影 天窗 电尾 前后 电眼 氙灯 LED\\n备注：车架号：3081");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("宾利", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("279.0", cr.getDiscount_content());
			Assert.assertEquals("3081", cr.getVin());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款欧规3.0汽油 奥迪q7 \\n黑棕 配置:液晶仪表 真皮座椅 全景天窗 7座 Led日间行车灯 一键启动前排电动座椅带加热 停车辅助 电尾门 倒影 外后视镜电动调节/加热/折叠 真皮多功能方向盘 定速巡航 胎压监测\\n☎18920621369 小颖");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("奥迪", cr.getBrand_name());
			Assert.assertEquals(2016, cr.getYear());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("0.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"💝揽胜行政 17款 3.0 柴油 白/黑10台\\n8月2日到港预计8月20日交车\\n配置:V6 HSE版 全景天窗 智能卡 电尾门 二代地形 电吸门 LED氙气大灯 雾灯 19轮 8速 导航 蓝牙 380W音响\\n☎️1522253841");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("揽胜行政3.0柴油", cr.getCar_model_name());
			Assert.assertEquals("5", cr.getDiscount_way());
			Assert.assertEquals("0.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款酷路泽5700 黑棕 顶配 140");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("丰田", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("酷路泽5700", cr.getCar_model_name());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("140.0", cr.getDiscount_content());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款3.0柴油创世短轴 白／黄鹤 黑／黄鹤\\n配置：商务4座，滑动全景天窗，智能卡，氙灯LED，脚感电尾门，电吸门，二代全地形反馈，鹿皮顶，黑色钢琴漆内饰，钢琴漆多功能方向盘带加热，换挡拨片，内外自动防眩目后视镜，牛津打孔真皮座椅，前后排座椅电动调节、加热、通风、按摩带记忆【前排18项，后排14项】，前后飞翼头枕，前后冰箱，大屏导航，液晶仪表盘，四区空调，后娱10.2寸，发光迎宾踏板，多彩氛围灯，825W高级音响19扬声器，外后视镜电动调节带加热、记忆、折叠及倒车自动照地功能，车道偏离预警，并道辅助，前雾灯，大灯清洗，360环影，前后电眼，定速巡航，胎压监测，21轮，全尺寸备胎 143.5万（谈！谈！谈！） \\n免消费税的17款揽胜5.0四座，创世加长版黑/黄鹤（谈！谈！谈！！！！）\\n22轮，4座， 创世包（前后排座椅 通风加热按摩 前冰箱 爱马仕缝制顶棚 创世套件）半苯胺拓展真皮 二代地形反馈 盲点提醒 环车影像 四区空调 26项前排座椅调节 后排电动座椅 前后飞翼头枕 10.2后排娱乐 电吸门脚感尾门 HUD抬头显示 825高级音响，雷达测距系统。现车手续齐 246 大谈");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals("揽胜行政3.0柴油", cr.getCar_model_name());
			Assert.assertEquals("4", cr.getDiscount_way());
			Assert.assertEquals("143.5", cr.getDiscount_content());
		}
	}
	
	/*
	 * 平行进口车在搜索时用的search_level=low，所以会出来大量的搜索结果，但是命中term的数量相差较多，所以需要截断搜索结果
	 * 以两个style的term命中为gap
	 * 即以2000分为gap
	 * 但是考虑到浮点数带来的精度影响，以1999分为gap
	 * 如果结果的分数小于(maxScore-gap)
	 * 则结束，返回满足条件的结果
	 */
	@Test
	public void testPruningQueryResult() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"16款 欧版路虎 SV 3.0柴油混动加长创世 黑/黄(6407)");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2016, cr.getYear());
			Assert.assertEquals("欧版", cr.getStandard_name());
			Assert.assertEquals("6407", cr.getVin());
		}
	}
	
	/*
	 * 有的批量信息，内容中并没有显式的规格信息，也没有隐式的规格信息，例如"车架号"等关键字眼
	 * "16款 路虎 \\n SV 3.0柴油混动加长创世 黑/黄(6407)"
	 * 所以，该条信息会被用于搜索中规国产车
	 * 所以这个case就会被用于搜索指导价为6407的16款路虎。。
	 * 显然是错误的
	 * 所以如果我们判断规格错误导致车型识别失败，就要退化到平行进口车中去找，这样6407就会被当做车架号无视掉，不会干扰车型检索
	 * 
	 */
	@Test
	public void testStandardNotHitResource() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17款 路虎 \\n SV 3.0柴油混动加长创世 黑/黄(6407)");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("路虎", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("6407", cr.getVin());
		}
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"2017款莱万特S，白/红 标配（430匹马力 3.0T发动机 8速变速箱 空气悬挂 氙灯 LED日行灯 LED尾灯 真皮电动座椅带记忆一键启动 后排隐私玻璃 巡航定速 倒车影像 刹车辅助 玛莎车身稳定系统 19轮）黑卡钳 加热风挡 氙灯 运动踏板 不锈钢门槛+红色豪华座椅 皓岳库。手续齐全113.8万");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("玛莎拉蒂", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("113.8", cr.getDiscount_content());
		}
	}
	
	/*
	 * 有的品牌比较特殊，即它只有一个平行进口车车型，车型下对应的款式也很少
	 * 例如“17加版雷克萨斯”
	 * 这个肯定对应的是LX570
	 * 而17款LX570也就有2个款式，所以我们可以认为是OK的
	 * 但是LX570下的20轮没有资源，是否要剔除干扰呢？？？(已剔除“八座20轮带来的干扰”)
	 */
	@Test
	public void testSpecialBaseCar() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"17加版雷克萨斯");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("雷克萨斯", cr.getBrand_name());
			Assert.assertEquals(2017, cr.getYear());
			Assert.assertEquals("加版", cr.getStandard_name());
			Assert.assertEquals("LX570", cr.getCar_model_name());
			Assert.assertEquals("17款 八座21轮", cr.getStyle_name());
		}
	}
}
