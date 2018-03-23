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
import com.niuniu.Utils;

import junit.framework.Assert;

/*
 *  中规、国产车的测试用例
 */
public class TestUtils {

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
	public void testEscapeSpecialMultiplySign() {
		{
			String str = "飞度 888X6台 下4000";
			String ans = Utils.escapeSpecialMultiplySign(str);
			Assert.assertEquals("飞度 888 6台 下4000", ans);
		}
		
		{
			String str = "飞度 888x6台 下4000";
			String ans = Utils.escapeSpecialMultiplySign(str);
			Assert.assertEquals("飞度 888 6台 下4000", ans);
		}
	}
}