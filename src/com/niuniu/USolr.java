package com.niuniu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.*;
import java.util.Map.Entry;


import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.client.solrj.response.TermsResponse;

public class USolr {
	private HttpSolrServer server = null;
	private String server_url = "http://121.40.204.159:8080/solr/";
	private String local_url = "http://localhost:8084/solr/";
	private String url = server_url;
	public SolrQuery solrquery = null;
	private SolrDocumentList queryresult = null;
	private Map<String, Map<String, List<String>>> highlightresult = null;
	public QueryResponse queryresponse = null;
	public USolr() {
		solrquery = new SolrQuery();
		try {
			if (server == null) {
				server = new HttpSolrServer(url);
				server.setSoTimeout(100000); // socket read timeout
				server.setConnectionTimeout(100000);
				server.setDefaultMaxConnectionsPerHost(100);
				server.setMaxTotalConnections(100);
				server.setFollowRedirects(false); // defaults to false
				// allowCompression defaults to false.
				// Server side must support gzip or deflate for this to have any
				// effect.
				server.setAllowCompression(true);
				server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public USolr(String base) {
		this.url = base;
		solrquery = new SolrQuery();
		try {
			if (server == null) {
				server = new HttpSolrServer(url);
				server.setSoTimeout(10000); // socket read timeout
				server.setConnectionTimeout(1000);
				server.setDefaultMaxConnectionsPerHost(100);
				server.setMaxTotalConnections(100);
				server.setFollowRedirects(false); // defaults to false
				// allowCompression defaults to false.
				// Server side must support gzip or deflate for this to have any
				// effect.
				server.setAllowCompression(true);
				server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setIndexWithUrl(String full_url) {
		server.setBaseURL(full_url);
	}

	// 删除对应索引库下的索引
	public void DeleteIndex() throws Exception {
		server.deleteByQuery("*:*");
		server.commit();
		System.out.println("Delete done");
	}

	// 以下一系列函数为 为Solr的server构造查询参数 的接口
	// 选择索引库，index为solr的core名称，例如poi,street等
	public void selectIndex(String index) {
		server.setBaseURL(url + index);
	}

	public void clear() {
		queryresponse=null;
		solrquery.clear();
		solrquery.clearSorts();
	}

	public boolean setQuery(String query) {
		if (solrquery == null || query == null)
			return false;
		else {
			try {
				solrquery.setQuery(query);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	public String getQuery(){
		if(solrquery==null)
			return null;
		return solrquery.getQuery();
	}
	
	
	//test for terms!!!!===========================================================================
	public boolean setTestQuery(String query) {
		SolrQuery params = solrquery;  
        params.set("q", query);  
        params.set("qt", "/terms");  
          
        //parameters settings for terms requestHandler   
        // 参考（refer to）http://wiki.apache.org/solr/TermsComponent  
        params.set("terms", "true");  
        params.set("terms.fl","text");  
          
       // params.set("terms.lower", ""); //term lower bounder开始的字符  
      //  params.set("terms.lower.incl", "true");  
       // params.set("terms.mincount", "1");  
       // params.set("terms.maxcount", "100");  
          
        //http://localhost:8983/solr/terms?terms.fl=text&terms.prefix=学 // using for auto-completing  
        params.set("terms.prefix", "大");   
        //params.set("terms.regex", "");  
        //params.set("terms.regex.flag", "case_insensitive");  
          
        params.set("terms.limit", "50");  
   //     params.set("terms.upper", ""); //结束的字符  
    //    params.set("terms.upper.incl", "false");  
          
        params.set("terms.raw", "true");  
        params.set("terms.sort", "index");
        return true;
	}
	//test for terms!!!!===========================================================================
	
	
	public String getFields(){
		if(solrquery==null)
			return null;
		return solrquery.getFields();
	}
	public boolean setFields(String... fields) {
		if (solrquery == null || fields == null)
			return false;
		else {
			try {
				solrquery.setFields(fields);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public String[] getFilter(){
		if(solrquery==null)
			return null;
		return solrquery.getFilterQueries();
	}
	public boolean addFilter(String... filter) {
		if (solrquery == null || filter == null)
			return false;
		else {
			try {
				solrquery.addFilterQuery(filter);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	public boolean removeFilter(String fq) {
		return solrquery.removeFilterQuery(fq);
	}

	// order为true则升序，order为false则降序
	public boolean addSortField(String sortfield, boolean order) {
		if (solrquery == null || sortfield == null)
			return false;
		else {
			try {
				
				if (order)
					solrquery.addSort(sortfield, SolrQuery.ORDER.asc);
				else
					solrquery.addSort(sortfield, SolrQuery.ORDER.desc);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	public String[] getSortFields(){
		if(solrquery==null)
			return null;
		return solrquery.getSortFields();
	}
	public String getSortField(){
		if(solrquery==null)
			return null;
		return solrquery.getSortField();
	}
	public boolean removeSortField(String sortfield, boolean order) {
		if (solrquery == null || sortfield == null)
			return false;
		else {
			try {
				if (order)
					solrquery.removeSortField(sortfield, SolrQuery.ORDER.asc);
				else
					solrquery.removeSortField(sortfield, SolrQuery.ORDER.desc);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	public boolean setChaos(boolean chaos){
		if (solrquery == null)
			return false;
		else {
			solrquery.set("chaos", chaos);
			return true;
		}
	}
	
	public boolean setNiufacet(boolean niufacet){
		if (solrquery == null)
			return false;
		else {
			solrquery.set("niufacet", niufacet);
			return true;
		}
	}
	
	public boolean setGroupdasan(String groupdasan){
		if (solrquery == null)
			return false;
		else {
			solrquery.set("group_dasan", "filter");
			return true;
		}
	}
	
	public boolean getHighlight(){
		if(solrquery==null)
			return false;
		return solrquery.getHighlight();
	}
	public boolean setHighlight(boolean attribute) {
		if (solrquery == null)
			return false;
		else {
			solrquery.setHighlight(attribute);
			return true;
		}
	}

	public String[] getHighlightField(){
		if(solrquery==null)
			return null;
		return solrquery.getHighlightFields();
	}
	public boolean addHighlightField(String highlight) {
		if (solrquery == null || highlight == null)
			return false;
		else {
			try {
				solrquery.addHighlightField(highlight);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	public boolean removeHighlightField(String highlight) {
		if(solrquery==null)
			return false;
		return solrquery.removeHighlightField(highlight);
	}

	public String getHighlightSimplePre(){
		if(solrquery==null)
			return null;
		return solrquery.getHighlightSimplePre();
	}
	public boolean setHighlightSimplePre(String str) {
		if (solrquery == null || str == null)
			return false;
		else {
			try {
				solrquery.setHighlightSimplePre(str);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public String getHighlightSimplePost() {
		if(solrquery==null)
			return null;
		return solrquery.getHighlightSimplePost(); 
	}	
	public boolean setHighlightSimplePost(String str) {
		if (solrquery == null || str == null)
			return false;
		else {
			try {
				solrquery.setHighlightSimplePost(str);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public int getStart(){
		if(solrquery==null)
			return -1;
		return solrquery.getStart();
	}

	public boolean setStart(int start) {
		if (solrquery == null || start < 0)
			return false;
		else {
			try {
				solrquery.setStart(start);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	public int getRows(){
		if(solrquery==null)
			return -1;
		return solrquery.getRows();
	}

	public boolean setRows(int rows) {
		if (solrquery == null || rows < 0)
			return false;
		else {
			try {
				solrquery.setRows(rows);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public boolean getDismax(){
		if(solrquery==null)
			return false;
		String tmp = solrquery.get("defType");
		if(tmp.equals("edismax"))
			return true;
		return false;
	}
	
	public boolean setDismax(boolean flag) {
		if (solrquery == null || flag == false)
			return false;
		else {
			try {
				solrquery.set("defType", "edismax");
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	public boolean setNiuniuParser(boolean flag) {
		if (solrquery == null || flag == false)
			return false;
		else {
			try {
				solrquery.set("defType", "niuniuparser");
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	// if qf contains not only one field, just part them with space,
	// eg:"var_poi_chinese var_address_chinese"
	public String getDismaxField(){
		if(solrquery==null)
			return null;
		return solrquery.get("qf");
	}
	public boolean setDismaxField(String qf) {
		if (solrquery == null || qf == null)
			return false;
		else {
			try {
				solrquery.set("qf", qf);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	public boolean removeDismaxField(String qf) {
		if (solrquery == null || qf == null)
			return false;
		else {
			try {
				solrquery.remove("qf", qf);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	public boolean setSpellCheck(){
		if(solrquery==null)
			return false;
		try{
			solrquery.setParam("spellcheck", "true");
			return true;
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setSpellCheckQuery(String qString){
		if(solrquery==null)
			return false;
		try{
			solrquery.setParam("spellcheck.q", qString);
			return true;
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean setDebugQuery(boolean flag) {
		if (solrquery == null)
			return false;
		else {
			try {
				if (flag)
					solrquery.set("debugQuery", "on");
				else
					solrquery.set("debugQuery", "off");
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public void ExecuteQuery() throws Exception {
		try {
			queryresponse = server.query(solrquery);
			}
		catch (SolrServerException e) {
			System.out.println("第二次执行...");
			queryresponse = server.query(solrquery);
		}
	}
	
	public void ExecuteQuery(SolrParams params) throws Exception {
		try {
			queryresponse = server.query(params);
			}
		catch (SolrServerException e) {
			System.out.println("第二次执行...");
			queryresponse = server.query(params);
			//e.printStackTrace();
		}
	}

	// close
	/*
	 * two interfaces left. 1. return a normal result for query 2. return the
	 * highlight result.
	 */
	public SolrDocumentList getQueryResult() {
		queryresult = queryresponse.getResults();
		return this.queryresult;
	}

	public Map<String, Map<String, List<String>>> getHightlightResult() {
		highlightresult = queryresponse.getHighlighting();
		return this.highlightresult;
	}

	public void close() {
		server.shutdown();
	}
	
	public boolean checkValidation(String query){
		List<SolrDocument> docs2 = getQueryResult();// 得到结果集
		//int s = docs2.size();
		//System.out.println(query +"\t"+ Integer.toString(s));
		return docs2.size()>0;
	}
	
	public void print() {
		List<SolrDocument> docs2 = getQueryResult();// 得到结果集
		int ik = 0;
		for (SolrDocument doc : docs2) {// 遍历结果集
			System.out.println(doc.getFieldValue("score"));
			for (Iterator iter = doc.iterator(); iter.hasNext();) {
				Map.Entry<String, Object> entry = (Entry<String, Object>) iter
						.next();
				System.out.print("Key :" + entry.getKey() + "  ");
				System.out.println("Value :" + entry.getValue());
				Object ob = new Object();

			}
			ik++;
			System.out.println("----------------------------------");
		}
		System.out.println(ik);
	}
	public void printTerms(){
		if(queryresponse != null ){  
            System.out.println("查询耗时（ms）：" + queryresponse.getQTime());  
            //System.out.println(response.toString());  
              
            TermsResponse termsResponse = queryresponse.getTermsResponse();  
            if(termsResponse != null) {  
                Map<String, List<TermsResponse.Term> > termsMap = termsResponse.getTermMap();  
                  
                for(Map.Entry<String, List<TermsResponse.Term> > termsEntry : termsMap.entrySet()) {  
                    System.out.println("Field Name: " + termsEntry.getKey());  
                    List<TermsResponse.Term> termList = termsEntry.getValue();  
                    System.out.println("Term    :  Frequency");  
                    for(TermsResponse.Term term : termList) {  
                        System.out.println(term.getTerm() + "   :   " + term.getFrequency());  
                    }  
                    System.out.println();  
                }
            }
		}
	}
	
	public boolean setDefType(String deftype){
		if (solrquery == null || deftype == null)
			return false;
		else {
			try {
				solrquery.set("defType", deftype);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	public boolean process(){
		try{
			selectIndex("staging");
			setFields("*", "score");
			setStart(0);
			setRows(15);
			solrquery.set("defType", "niuniuparser");
			File file = new File("/Users/kehl/solr_home/prefix/target.txt");
			File file2 = new File("/Users/kehl/solr_home/prefix/filter_result.txt");
			FileWriter fw = null;
		    BufferedWriter writer = null;
			if(file.isFile() && file.exists()){
				InputStreamReader read = new InputStreamReader(new FileInputStream(file),"utf8");
				fw = new FileWriter(file2);
				writer = new BufferedWriter(fw);
				BufferedReader bufferedReader = new BufferedReader(read);
				
				String line = null;
				while( (line = bufferedReader.readLine()) != null){
					line = line.trim();
					String[] arrs = line.split("\t");
					String query = arrs[0];
					setQuery(query);
					ExecuteQuery();
					if(checkValidation(query)){
						writer.write(line);
						writer.newLine();
					}else{
						System.out.println(query);
					}
					//Thread.sleep(1000);
				}
				writer.close();
				read.close();
			}
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		USolr solr = new USolr();
		
		solr.selectIndex("niuniu_basecars");
		solr.setFields("*", "score");
		solr.setStart(0);
		solr.setRows(15);
		//solr.setDismax(true);
		//solr.addFilter("area1","上海市");
		//solr.addFilter("{!geofilt}&sfield=position&pt=31.298528645833333,121.50142795138889&d=5&sort=geodist() asc");
		//solr.setDismaxField("BigTag");
		String query = "*:*";
		solr.clear();
		solr.selectIndex("niuniu_basecars");
		solr.setFields("*", "score");
		solr.setStart(0);
		solr.setRows(10);
		solr.setDefType("niuniuparser");
		solr.setQuery(query);
		solr.addFilter("standard:\\" + Integer.toString(2));//国产、中规
		//solr.setNiuniuParser(true);
		
		// solr.setDebugQuery(true);
		System.out.println(solr.solrquery);
		//solr.addSortField("frequency", false);
		long t1 = System.currentTimeMillis();
		try {
			solr.ExecuteQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(System.currentTimeMillis() - t1);
		solr.print();
		solr.close();
	}
	public static void main2(String[] args) throws Exception {
		USolr solr = new USolr();
		solr.process();
	}
}
