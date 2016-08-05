package iii.snsi.iov.carq.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*import org.slf4j.Logger;
import org.slf4j.LoggerFactory;*/

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AutomdDiagnoseClient {
	private final String baseUrl = "https://www.automd.com";
	private WebClient client = null;

	// Pair: http://stackoverflow.com/a/521235
	public class Pair<K, V> {

		  private final K key;
		  private final V val;

		  public Pair(K key, V val) {
		    this.key = key;
		    this.val = val;
		  }

		  public K getKey() { return key; }
		  public V getVal() { return val; }
	}
	
	public void init(boolean enableJs) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		client = HtmlUnitCrawler.getWebClient(enableJs);
	}

	public void close() {
		if(client != null) {
			client.close();
		}
	}
	
	/* init Pairs (and the pairs in nextQnaQuery)
	 * qid: original Url also has qid in the query parameters, but leaving it out 
	 *  does not seem to affect the http response. The reason why it is left out is
	 *  because the qid value is only retrievable from the /diagnose/next_qna query
	 *  which originally requires the qid as a query parameter.
	 * option: original Url set as either 1 or 2, should not affect the http response. 
	 * 	default set to 1.
	 */

	private List<Pair<String, String>> buildQueryParam(AutomdWebPage webPage, String queryUrl){
		List<Pair<String, String>> queryParam = new ArrayList<Pair<String, String>>();

		if (queryUrl.equals(baseUrl + "/diagnose/select_area")){
			// build vehicle queryParam
			
			// 2016 Toyota Avalon Hybrid Limited 4 Cyl 2.5L
			Pair<String, String> year = new Pair<String, String>("year", "117");
			Pair<String, String> make = new Pair<String, String>("make", "7");
			Pair<String, String> model = new Pair<String, String>("model", "76");
			Pair<String, String> submodel = new Pair<String, String>("submodel", "658");
			Pair<String, String> engine = new Pair<String, String>("engine", "77");

			queryParam.add(year);
			queryParam.add(make);
			queryParam.add(model);
			queryParam.add(submodel);
			queryParam.add(engine);
		}
		else if (queryUrl.equals(baseUrl + "/diagnose/qna")){
			Pair<String, String> aid = new Pair<String, String>("aid", webPage.getAidPair().getVal());
			Pair<String, String> option = new Pair<String, String>("option", "1");

			queryParam.add(aid);
			queryParam.add(option);
		}
		else if (queryUrl.equals(baseUrl + "/diagnose/next_qna")){
			Pair<String, String> aid = new Pair<String, String>("aid", webPage.getAidPair().getVal());
			Pair<String, String> ajax = new Pair<String, String>("ajax", "1");

			queryParam.add(aid);
			queryParam.add(ajax);
		}
		else if (queryUrl.equals(baseUrl + "/diagnose/problems")){
			Pair<String, String> aid = new Pair<String, String>("aid", webPage.getAidPair().getVal());
			
			queryParam.add(aid);
		}
		
		return queryParam;
		
	}
	
	public String getResponse(String queryUrl, String queryMethod, List<Pair<String, String>> queryParam) throws Exception {
		Query query = new Query();
		return query.getResponse((httpRequest(queryUrl, queryMethod, queryParam)));
	}
	
	public void setCookie(String queryUrl, String queryMethod) throws Exception {
		Query query = new Query();
		AutomdWebPage emptyWebPage = new AutomdWebPage();
		query.setCookie(httpRequest(queryUrl, queryMethod, buildQueryParam(emptyWebPage, queryUrl)));
	}
	
	public HttpsURLConnection httpRequest(String queryUrl, String queryMethod, List<Pair<String, String>> queryParam) throws Exception {
		Query query = new Query();
		
		String url = queryUrl;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		
		if(queryMethod == "GET"){
			con = query.httpGet(queryUrl, queryParam);
		}
		else if(queryMethod == "POST"){
			con = query.httpPost(queryUrl, queryParam);
		}
		// will return the diagnostic homepage if get or post is not specified
		return con;
	}
	
	private List<String> buildProblemIdQueryUrlList(AutomdWebPage currWebPage) throws Exception{
		List<String> problemIdQueryUrlList = new ArrayList<String>();
		
		// step 3
		String queryUrl = baseUrl + "/diagnose/problems";
		List<Pair<String, String>> queryParam = buildQueryParam(currWebPage, queryUrl);
		String response = getResponse(queryUrl, "GET", queryParam);
		
		//System.out.println(response);
		
		/* build list of individual problem_id http links
		 * 
		 * Since we are sending the complete Url directly to the query methods,
		 * queryParam is not needed.
		 * 
		 * <input ... class="btn-continue-to-inspect button-blue button-size-34 pull-right"
		 * ... onclick="...'/diagnose/inspection?problem_id=100'...">
		 * 
		 */

		Document doc = Jsoup.parse(response);
		Elements buttonElements = doc.select("input.btn-continue-to-inspect.button-blue.button-size-34.pull-right");
		//System.out.println("buttonElements.size(): " + buttonElements.size());
		for(Element buttonElement : buttonElements){
			//System.out.println("onclick=" + buttonElement.attr("onclick"));
			Pattern pattern = Pattern.compile("/diagnose/inspection\\?problem_id=\\d+");
			Matcher matcher = pattern.matcher(buttonElement.attr("onclick"));
			if (matcher.find()){
				problemIdQueryUrlList.add(baseUrl + matcher.group(0));
			}
		}
		
		return problemIdQueryUrlList;
	}
	

	// extract data and build problem
	private AutomdProblem buildProblem(String queryUrl) throws Exception{
		AutomdProblem problem = new AutomdProblem();
		
		List<Pair<String, String>> queryParam = Collections.<Pair<String, String>> emptyList();
		String response = getResponse(queryUrl, "GET", queryParam);
		
		Document doc = Jsoup.parse(response);
		
		String title = doc.select("h2.flat-heading-h1.mb20").text();
		String description = doc.select("div.col-sm-12.col-md-8.content-main-left.clearfix.p.flat-paragraph").text();
		
		// create list of inspection steps
		List<String> inspectionSteps = new ArrayList<String>();
		
		// a single inspection guide with multiple stepElements (check later to see if there are multiple inspectionGuides, then refactor code)
		Elements stepElements = doc.select("div.inspection-guide").get(0).select("div.step.clearfix");
		System.out.println("stepElements.size(): " + stepElements.size());
		for(Element stepElement : stepElements){
			inspectionSteps.add(stepElement.select("div.content").get(0).select("p.flat-paragraph").text());
			System.out.println(stepElement.select("div.content").get(0).select("p.flat-paragraph").text());
		}
		
		problem.setTitle(title);
		problem.setDescription(description);
		problem.setInspectionSteps(inspectionSteps);
		
		return problem;
	}
	
	/* end of step 2 -> step 3 -> step 4 -> extract info
	 * buildProblemIdUrlList -> 
	 */
	
	public List<AutomdProblem> buildProblemList(AutomdWebPage currWebPage) throws Exception{

		System.out.println("buildProblemList!");
		
		List<String> queryUrlList = buildProblemIdQueryUrlList(currWebPage);
		
		// query each problem_id url
		List<AutomdProblem> problemList = new ArrayList<AutomdProblem>();
		
		for(String queryUrl : queryUrlList){
			problemList.add(buildProblem(queryUrl));
		}
		return problemList;
	}
	
	public List<AutomdWebPage> buildWebPageList(AutomdWebPage currWebPage, String queryUrl, String queryMethod) throws Exception {
		//System.out.println("in getWebPageList!");
		
		List<Pair<String, String>> queryParam = buildQueryParam(currWebPage, queryUrl);
		String response = getResponse(queryUrl, queryMethod, queryParam);
		
		// check if response is in JSON format (begins with '{' and ends with '}')
		if(response.charAt(0) == '{' && response.charAt(response.length() - 1) == '}'){
			//System.out.println("is JSON!");
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(response);
			JSONObject jsonObject = (JSONObject) obj;
			JSONObject data = (JSONObject) jsonObject.get("data");
			response = (String) data.get("qna_content");
		}
		
		//System.out.println(response);

		/* parse html document to extract key : val 
		 * 
		 * <h3 class="diagnose-header flat-text-bold16 mb20" ... ></h3>
		 * <div .. > 
		 * 	<label> 
		 * 	 <input value="aid_value" ... >
		 *   <span class="label-text flat-paragraph" ... </span>
		 * 	</label>
		 * 	<label> ... </label>
		 * </div>
		 * <h3 ... </h3>
		 */
		Document doc = Jsoup.parse(response);
		
		Elements h3Elements = doc.select("h3.diagnose-header.flat-text-bold16.mb20");
		//System.out.println("h3Elements.size(): " + h3Elements.size());
		
		
		List<AutomdWebPage> webPageList = new ArrayList<AutomdWebPage>();
		for(Element h3 : h3Elements){ // for each webPage that exists on the response
			
			AutomdWebPage webPage = new AutomdWebPage();
			List<AutomdWebPage> childWebPageList = new ArrayList<AutomdWebPage>();
		
			System.out.println("title: " + h3.text());

			webPage.setTitle(h3.text());

			if(queryParam.size() != 0 && queryParam.get(0).getKey() != "" &&  queryParam.get(0).getVal() != ""){
				//System.out.println("queryParam.get(0): " + queryParam.get(0).getKey() + " : " + queryParam.get(0).getVal());
				webPage.setAidPair(queryParam.get(0));
			}
			
			Elements divs = h3.siblingElements();
			//System.out.println("divs.size(): " + divs.size());
			for(Element div : divs){ // for each column in the webPage
				Elements labels = div.select("label");
				//System.out.println("labels.size(): " + labels.size());
				for(Element label : labels){ // for each aidPair
					AutomdWebPage childWebPage = new AutomdWebPage();
					
					String key = label.select("span.label-text.flat-paragraph").first().text();
					String val = label.select("input").first().attr("value");
					
					// key - remove misc symptom description (after the -)
					if(key.indexOf(" -") != -1){
						key = key.substring(0, key.indexOf(" -"));
					}
					else if(key.indexOf("- ") != -1){
						key = key.substring(0, key.indexOf("- "));
					}
					
					//System.out.println("childWebPage.aidPair added!: " + key + " : " + val);
					
					Pair<String, String> aidPair = new Pair<String, String>(key, val);
					childWebPage.setAidPair(aidPair);
					childWebPageList.add(childWebPage);
				}
			}
			
			System.out.println("webPage.getChildWebPageList().size(): " + webPage.getChildWebPageList().size());
			webPage.setChildWebPageList(childWebPageList);
			webPageList.add(webPage);
		}
		//System.out.println("finished getWebPageList!");
		return webPageList;
	}
	
	public HtmlPage getPage(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage page = client.getPage(url);
		return page;

	}

}
