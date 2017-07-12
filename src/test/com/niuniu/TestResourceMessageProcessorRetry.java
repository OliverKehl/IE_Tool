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

public class TestResourceMessageProcessorRetry {

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
}
