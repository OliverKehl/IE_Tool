package test.com.niuniu.classifier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.niuniu.USolr;
import com.niuniu.Utils;
import com.niuniu.classifier.SimpleMessageClassifier;

import junit.framework.Assert;

/*
 *  中规、国产车的测试用例
 */
public class TestSimpleMessageClassifier {

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
	public void testBasic(){
		USolr solr = new USolr("http://101.37.169.138:8983/solr/");
		
		{
			String message = "17款美规奔驰GLS450";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(-1, mode);
		}
		
		{
			String message = "老款朗逸1249下25000";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(1, mode);
		}
		
		{
			String message = "17款美规奔驰GLS450 #1234 #5678";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(-1, mode);
		}
	}
	
	@Test
	public void testIllegalInput(){
		USolr solr = new USolr("http://101.37.169.138:8983/solr/");
		
		{
			String message = "老款朗逸1249 1269 下25000";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(0, mode);
		}
	}
	
	@Test
	public void testImplicitParallel(){
		USolr solr = new USolr("http://101.37.169.138:8983/solr/");
		
		{
			String message = "17款GLS450 黑/黑 #6738 P01全景 方向盘加热 哈曼 二排电动 照明脚踏 后娱预留 停车辅助 驾驶员辅助 雷测 现车手续齐 111";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(-1, mode);
		}
		
		{
			String message = "汉兰达2878白黑下4500";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(1, mode);
		}
		
		{
			String message = "汉兰达2878白黑4500";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(1, mode);
		}
		
		{
			String message = "17款酷路泽4500白黑";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(-1, mode);
		}
		
		{
			String message = "指南者\\n1698白 黑4.25";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(1, mode);
		}
		
		{
			String message = "指南者\\n1698白 黑5.25";
			message = Utils.normalizePrice(Utils.cleanDate(Utils.clean(Utils.normalize(message), solr)));
			SimpleMessageClassifier simpleMessageClassifier = new SimpleMessageClassifier(message, solr);
			int mode = simpleMessageClassifier.predict();
			Assert.assertEquals(0, mode);
		}
	}
	
}
