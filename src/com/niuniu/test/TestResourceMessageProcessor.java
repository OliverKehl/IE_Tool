package com.niuniu.test;

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
					"k3 \n 968白优惠24500");
			rmp.process();
			CarResourceGroup crg = rmp.getCarResourceGroup();
			Assert.assertEquals(1, crg.getResult().size());
			CarResource cr = crg.getResult().get(0);
			Assert.assertEquals("起亚", cr.getBrand_name());
			Assert.assertEquals("9.68", cr.getGuiding_price());
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
	}
}
