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
public class TestCarResourcePostProcess {

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
	public void testFilterLowScoreResource() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"柯斯达 \\n 4125坨白23座下6000（老款）\\n 456香槟金20座特别版下3000新款\\n5588米黄金17座18座19座20座下29000（老款）\\n5588米黄金14座15座16座（老款）");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
			for(CarResource cr:crg.getResult()){
				Assert.assertEquals("丰田", cr.getBrand_name());
				Assert.assertEquals("柯斯达", cr.getCar_model_name());
			}
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"皇冠\\n 2948黑下33000出现车（运动）\\n交强\\n普瑞维亚\\n奔驰3898白下4000出现车(新款)\\n4098白加4000出现车(新款)\\n普拉多\\n4798白加18000交强险\\n4798绿加23000交强险");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(4, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"皇冠\\n 2948黑下33000出现车（运动）\\n交强\\n普瑞维亚\\n3898白下4000出现车(新款)\\n4098白加4000出现车(新款)\\n普拉多\\n4798白加18000交强险\\n4798绿加23000交强险");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"皇冠\\n 2948黑下33000出现车（运动）\\n交强\\n普瑞维亚\\n3898白下4000出现车(新款)\\n4098白加4000出现车(新款)\\n普拉多\\n4798白加18000交强险\\n4798绿加23000交强险");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
		}
	}
	
	/*
	 *  剔除车型是ABA格式中的B车源
	 */
	@Test
	public void testFilterSingularCarModel() {
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"途安\\n1698 白色 蓝色 优惠15500\\n1798 蓝色 优惠15000\\n1988 白色 优惠15000\\n1558 白色 蓝色 优惠10500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(3, crg.getResult().size());
			for(CarResource cr:crg.getResult()){
				Assert.assertEquals("大众", cr.getBrand_name());
				Assert.assertEquals("途安", cr.getCar_model_name());
			}
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"途安\\n1689 白色 蓝色 优惠15500\\n1798 蓝色 优惠15000\\n1988 白色 优惠15000\\n1558 白色 蓝色 优惠10500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(2, crg.getResult().size());
			for(CarResource cr:crg.getResult()){
				Assert.assertEquals("大众", cr.getBrand_name());
				Assert.assertEquals("途安", cr.getCar_model_name());
			}
		}
		
		{
			ResourceMessageProcessor rmp = new ResourceMessageProcessor();
			rmp.setMessages(
					"最新 现货 手续齐全 🉐️🉐️\\n【发现神行】\\n378白/黑-8.4万\\n368黑/黑-8.2万(17款)\\n408红/黑-9.7万(18款)\\n408斯灰/黑 黑/黑-9.6万(18款)\\n408白/黑-10.5万(17款)\\n408凯灰/黑 红/黑-10.1万(17款)\\n468白/黑-14万(17款)\\n【揽胜极光】\\n458红/黑 黑/黑-13.8万(17款英伦版)\\n458白/黑-12.6万(18款英伦版)\\n458凯灰/黑 红/黑 黑/黑-12.2万(18款英伦版)🔥\\n【揽胜运动】\\n928白/黑-8.5万(2.2万配置)\\n968白/黑 黑/黑(新能源)🔥🔥\\n1078白/浅褐 黑/浅褐🔥🔥\\n【揽胜行政】\\n1458黑/黑 白/黑-30.3万\\n1658黑/黑(北京现货)\\n2678黑/黄褐-35万(五座) 🔥🔥\\n2678黑/干椒红(四座) 电议🔥🔥\\n3328黑/黄褐 黑/樱桃红 电议🔥🔥\\n【XFL】\\n458黑/棕 卢蓝/棕-10.2万\\n458剧院红/棕-10.2万\\n498白/棕-11.8万\\n498剧院红/棕-11.5万\\n【捷豹FPACE】\\n528水晶蓝 (45000配置)\\n528卢蓝/黑-10万\\n548中国红/黑-11万\\n548卢兰/黑 中国红/黑-10万(45000配置)\\n628白/黑红-13.6万\\n赵珂伟 tel：13301339220");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(27, crg.getResult().size());
		}
	}
}