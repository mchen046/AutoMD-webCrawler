package iii.snsi.iov.carq.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AutomdDiagnoseClient {
	//private final String baseUrl = "https://www.automd.com";
	//private final String translationUrl = "http://api.microsofttranslator.com/v2/Http.svc/Translate";

	private WebClient client = null;

	private static Integer queryCount = 0;
	private static Integer problemCount = 0;
	private static Integer inspectionStepCount = 0;

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

	public static class MicrosoftTranslatorToken {
		public String token_type;
		public String access_token;
		public String expires_in;
		public String scope;

		public MicrosoftTranslatorToken(){
		}

		public MicrosoftTranslatorToken(String token_type, String access_token, String expires_in, String scope) {
			this.token_type = token_type;
			this.access_token = access_token;
			this.expires_in = expires_in;
			this.scope = scope;
		}
	}

	public void init(boolean enableJs) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		client = HtmlUnitCrawler.getWebClient(enableJs);
	}

	public void close() {
		if(client != null) {
			client.close();
		}
	}

	private HttpURLConnection httpRequest(String queryUrl, String queryMethod, List<Pair<String, String>> queryParam) throws Exception {
		System.out.println("queries: " + ++queryCount);
		Query query = new Query();

		String url = queryUrl;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection)obj.openConnection();

		if(queryMethod == "GET"){
			con = query.httpGet(queryUrl, queryParam);
		}
		else if(queryMethod == "POST"){
			con = query.httpPost(queryUrl, queryParam);
		}
		// will return the diagnostic homepage if get or post is not specified
		return con;
	}
	
	// custom 4 space indent JSON pretty printer: http://stackoverflow.com/a/28261746
	public String prettyPrintJSON(AutomdWebPage webPage) throws JsonProcessingException{
		// Create the mapper
		ObjectMapper mapper = new ObjectMapper();

		// Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
		DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);

		// Serialize it using the custom printer
		return mapper.writer(printer).writeValueAsString(webPage);
	}

	/* init Pairs
	 * qid: original Url also has qid in the query parameters, but leaving it out
	 *  does not seem to affect the http response. The reason why it is left out is
	 *  because the qid value is only retrievable from the /diagnose/next_qna query
	 *  which originally requires the qid as a query parameter.
	 * option: original Url set as either 1 or 2, should not affect the http response. 
	 * 	default set to 1.
	 */

	private List<Pair<String, String>> buildQueryParam(AutomdWebPage webPage, String queryUrl){
		List<Pair<String, String>> queryParam = new ArrayList<Pair<String, String>>();

		if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/select_area")){
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
		else if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/qna")){
			Pair<String, String> aid = new Pair<String, String>("aid", webPage.getAidPair().getVal());
			Pair<String, String> option = new Pair<String, String>("option", "1");

			queryParam.add(aid);
			queryParam.add(option);
		}
		else if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/next_qna")){
			Pair<String, String> aid = new Pair<String, String>("aid", webPage.getAidPair().getVal());
			Pair<String, String> ajax = new Pair<String, String>("ajax", "1");

			queryParam.add(aid);
			queryParam.add(ajax);
		}
		else if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/problems")){
			Pair<String, String> aid = new Pair<String, String>("aid", webPage.getAidPair().getVal());
			
			queryParam.add(aid);
		}
		else if (queryUrl.equals(QueryUrl.TRANSLATEURL.url())){ // microsoft translator api
			Pair<String, String> text = new Pair<String, String>("text", webPage.getTitle()); // input text
			Pair<String, String> from = new Pair<String, String>("from", "en"); // english
			Pair<String, String> to = new Pair<String, String>("to", "zh-Hant"); // chinese (traditional)

			queryParam.add(text);
			queryParam.add(from);
            queryParam.add(to);
		}
		else if (queryUrl.equals(QueryUrl.ACCESSTOKENURL.url())){ // microsoft access token post
			Pair<String, String> grant_type = new Pair<String, String>("grant_type", "client_credentials");
			Pair<String, String> client_id = new Pair<String, String>("client_id", "AutoMD-webCrawler");
            Pair<String, String> client_secret = new Pair<String, String>("client_secret", "JBKJ32B4J23B4N23B4VXLVXL342HJ34");
            Pair<String, String> scope = new Pair<String, String>("scope", "http://api.microsofttranslator.com");

			queryParam.add(grant_type);
			queryParam.add(client_id);
			queryParam.add(client_secret);
			queryParam.add(scope);
		}
		
		return queryParam;
		
	}
	
	public String getResponse(String queryUrl, String queryMethod, List<Pair<String, String>> queryParam) throws Exception {
		Query query = new Query();
		return query.getResponse((httpRequest(queryUrl, queryMethod, queryParam)));
	}

	/*public String translate(List<String> sourceTexts) throws Exception{

		String from = "en";
		String ContentType = "text/plain";
		String to = "zh-CHT";

		String requestBodyPrefix =
                    "<TranslateArrayRequest>" +
                        "<AppId />" +
                        "<From>" + from + "</From>" +
                        "<Options>" +
                            " <Category xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
                            "<ContentType xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\">" + ContentType + "</ContentType>" +
                            "<ReservedFlags xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
                            "<State xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
                            "<Uri xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
                            "<User xmlns=\"http://schemas.datacontract.org/2004/07/Microsoft.MT.Web.Service.V2\" />" +
                        "</Options>" +
                        "<Texts>";

		// build sourceTexts xml portion
		String sourceTextsXml = "";
		for(String sourceText : sourceTexts) {
			sourceTextsXml += "<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\">" + sourceText + "</string>";
		}

		String requestBodySuffix =
						"</Texts>" +
                        "<To>" + to + "</To>" +
                    "</TranslateArrayRequest>";

		String requestBody = requestBodyPrefix + sourceTextsXml + requestBodySuffix;
	}*/

	public void renewAccessToken() throws Exception {
		Query query = new Query();
		AutomdWebPage emptyWebPage = new AutomdWebPage();
		query.renewAccessToken(httpRequest(QueryUrl.ACCESSTOKENURL.url(), "POST", buildQueryParam(emptyWebPage, QueryUrl.ACCESSTOKENURL.url())));
	}
	
	public void setCookie(String queryUrl, String queryMethod) throws Exception {
		Query query = new Query();
		AutomdWebPage emptyWebPage = new AutomdWebPage();
		query.setCookie(httpRequest(queryUrl, queryMethod, buildQueryParam(emptyWebPage, queryUrl)));
	}
	
	private List<String> buildProblemIdQueryUrlList(AutomdWebPage currWebPage) throws Exception{
		List<String> problemIdQueryUrlList = new ArrayList<String>();
		
		// step 3
		String queryUrl = QueryUrl.BASEURL.url() + "/diagnose/problems";
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
				problemIdQueryUrlList.add(QueryUrl.BASEURL.url() + matcher.group(0));
			}
		}
		
		return problemIdQueryUrlList;
	}
	

	// extract data and build problem
	private AutomdProblem buildProblem(String queryUrl) throws Exception{
		System.out.println("problems: " + ++problemCount);
		AutomdProblem problem = new AutomdProblem();
		
		List<Pair<String, String>> queryParam = Collections.<Pair<String, String>> emptyList();
		String response = getResponse(queryUrl, "GET", queryParam);
		
		Document doc = Jsoup.parse(response);
		
		String title = doc.select("h2.flat-heading-h1.mb20").text();
		String description = doc.select("div.col-sm-12.col-md-8.content-main-left.clearfix").get(0).select("p.flat-paragraph").get(0).text();

		// create list of inspection steps
		List<String> inspectionSteps = new ArrayList<String>();
		
		// a single inspection guide with multiple stepElements (check later to see if there are multiple inspectionGuides, then refactor code)
		Elements stepElements = doc.select("div.inspection-guide").get(0).select("div.step.clearfix");
		//System.out.println("stepElements.size(): " + stepElements.size());
		for(Element stepElement : stepElements){
			String stepNumber = stepElement.select("div.number").text();
			String stepContent = stepElement.select("div.content").get(0).select("p.flat-paragraph").text();
			inspectionSteps.add(stepNumber + ") " + stepContent);
			System.out.println("inspectionSteps: " + ++inspectionStepCount);
		}

		/*System.out.println("title: " + title);
		System.out.println("description: " + description);
		System.out.println("inspectionSteps: " + inspectionSteps);*/

		problem.setTitle(title);
		problem.setDescription(description);
		problem.setInspectionSteps(inspectionSteps);
		
		//System.out.println(problem.getDescription());
		
		return problem;
	}
	
	/* end of step 2 -> step 3 -> step 4 -> extract info
	 * buildProblemIdUrlList -> 
	 */
	public List<AutomdProblem> buildProblemList(AutomdWebPage currWebPage) throws Exception{

		//System.out.println("buildProblemList!");
		
		List<String> queryUrlList = buildProblemIdQueryUrlList(currWebPage);
		
		// query each problem_id url
		List<AutomdProblem> problemList = new ArrayList<AutomdProblem>();
		

		// multi-thread: order in which problems are added does not matter
		queryUrlList.parallelStream().forEach(queryUrl -> {
			try {
				problemList.add(buildProblem(queryUrl));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return problemList;
	}
	
	public List<AutomdWebPage> buildWebPageList(AutomdWebPage currWebPage, String queryUrl, String queryMethod) throws Exception {
		
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
		
		/* Parse html document response to extract key : val 
		 * 
		 * It is written as O(n^3), but runtime is actually more closely resembled by O(n),
		 * where n is the number of labels (key : val aid pairs found)
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
		
		List<AutomdWebPage> webPageList = new ArrayList<AutomdWebPage>();
		for(Element h3 : h3Elements){ // for each webPage that exists on the response
			
			AutomdWebPage webPage = new AutomdWebPage();
			List<AutomdWebPage> childWebPageList = new ArrayList<AutomdWebPage>();
		
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
					
					// key - remove misc symptom detail (after the -)
					/*if(key.indexOf(" -") != -1){
						key = key.substring(0, key.indexOf(" -"));
					}
					else if(key.indexOf("- ") != -1){
						key = key.substring(0, key.indexOf("- "));
					}*/
					
					//System.out.println("childWebPage.aidPair added!: " + key + " : " + val);
					
					Pair<String, String> aidPair = new Pair<String, String>(key, val);
					childWebPage.setAidPair(aidPair);
					childWebPageList.add(childWebPage);
				}
			}
			
			//System.out.println("webPage.getChildWebPageList().size(): " + webPage.getChildWebPageList().size());
			webPage.setChildWebPageList(childWebPageList);
			webPageList.add(webPage);
		}
		return webPageList;
	}
	
	/* first query on step 2
	 * /diagnose/select_area : POST
	 * no cookie
	 *
	 * second query on step 2
	 * /diagnose/qna : GET 
	 * cookie
	 * 
	 * subsequent nested queries
	 * /diagnose/next_qna : GET
	 * cookie
	 *	
	* icChildWebPageList:
	 * 	incomplete childWebPageList
	 *  contains only the parent AutomdWebPage fields, need to further populate the nested
	 *  values -> childWebPageList
	 * gGrandChildWebPageList:
	 * 	great grand childWebPageList
	 */
	public List<AutomdWebPage> buildChildWebPageList(AutomdWebPage currWebPage, String queryUrl, String queryMethod, AutomdDiagnoseClient amdClient) throws Exception{
		//System.out.println(currWebPage.getAidPair().getKey() + " : " + currWebPage.getAidPair().getVal() + "\n");
		// grab all section block webPages with a title header on the currWebPage
		List<AutomdWebPage> icChildWebPageList = amdClient.buildWebPageList(currWebPage, queryUrl, queryMethod);
		
		List<AutomdWebPage> childWebPageList = new ArrayList<AutomdWebPage>();
		
		//System.out.println(String.format("icChildWebPageList.size() (children retrieved from %s): %d", queryUrl, icChildWebPageList.size()));
		
		if(icChildWebPageList.size() == 1 && icChildWebPageList.get(0).getChildWebPageList().size() == 0){ // base case
			return childWebPageList;
		}

		/* For each webPage section with a header. There is only more than 1 section on the first page.
		 * There are two sections on the first page.
		 * It is written as O(n^2), but runtime is actually more closely resembled by O(n)
		 *
		 * Multithreaded implementation
		 * Order in which childWebPage(s) and grandChildWebPage(s) are added does not matter
		 */
		icChildWebPageList.parallelStream().forEach(icChildWebPage -> {
			try {
				
				AutomdWebPage childWebPage = new AutomdWebPage();
				childWebPage.setTitle(icChildWebPage.getTitle());
				childWebPage.setAidPair(icChildWebPage.getAidPair());
				
				List<AutomdWebPage> grandChildWebPageList = new ArrayList<AutomdWebPage>();
				
				icChildWebPage.getChildWebPageList().parallelStream().forEach(icGrandChildWebPage -> {
					try{

						AutomdWebPage grandChildWebPage = new AutomdWebPage();
						grandChildWebPage.setTitle(icGrandChildWebPage.getTitle());
						grandChildWebPage.setAidPair(icGrandChildWebPage.getAidPair());
						
						// need to optimize this, currently does two queries each time (at most)
						List<AutomdWebPage> gGrandChildWebPageListQna = buildChildWebPageList(grandChildWebPage, QueryUrl.BASEURL.url() + "/diagnose/qna", "GET", amdClient);

						grandChildWebPage.setChildWebPageList( gGrandChildWebPageListQna.size() != 0 ? gGrandChildWebPageListQna : buildChildWebPageList(grandChildWebPage, QueryUrl.BASEURL.url() + "/diagnose/next_qna", "GET", amdClient));
						
						/* (step 3 -> step 4)
						 * insert problem webPage / problem object into childWebPageList (should be the only object in the list)
						 */
						if(grandChildWebPage.getChildWebPageList().size() == 0){
							grandChildWebPage.setProblemList(amdClient.buildProblemList(grandChildWebPage));
						}
						
						grandChildWebPageList.add(grandChildWebPage);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				childWebPage.setChildWebPageList(grandChildWebPageList);
				
				childWebPageList.add(childWebPage);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return childWebPageList;
	}
	
	public HtmlPage getPage(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage page = client.getPage(url);
		return page;
	}

}
