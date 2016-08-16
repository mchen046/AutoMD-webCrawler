package iii.snsi.iov.carq.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.HttpURLConnection;
import java.net.URLEncoder;

import java.text.MessageFormat;

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

	private WebClient client = null;

	private Integer queryCount = 0;
	private Integer problemCount = 0;
	private Integer inspectionStepCount = 0;
	private Integer translationCharCount = 0;
	private Integer invalidProblemCount = 0;

	private String language = LanguageCode.ENGLISH.code();

	// HashMap<String, problemList> -> List<ProblemListEntry> -> file -> List<ProblemListEntry -> HashMap<String, problemList>

	private HashMap<String, List<AutomdProblem>> problemListLibrary = new HashMap<String, List<AutomdProblem>>();

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public HashMap<String, List<AutomdProblem>> getProblemListLibrary() {
		return problemListLibrary;
	}

	public void setProblemListLibrary(HashMap<String, List<AutomdProblem>> problemListLibrary) {
		this.problemListLibrary = problemListLibrary;
	}

	public void init(boolean enableJs) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		client = HtmlUnitCrawler.getWebClient(enableJs);
	}

	public void close() {
		if(client != null) {
			client.close();
		}
	}

	// custom 4 space indent JSON pretty printer: http://stackoverflow.com/a/28261746
	public String prettyPrintJSON(AutomdWebPage webPage, Boolean pretty) throws JsonProcessingException {
		// Create the mapper
		ObjectMapper mapper = new ObjectMapper();

		// Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
		if (pretty) {
			DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
			DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
			printer.indentObjectsWith(indenter);
			printer.indentArraysWith(indenter);

            // Serialize it using the custom printer
			return mapper.writer(printer).writeValueAsString(webPage);
		}

		// plain JSON print
		return mapper.writer().writeValueAsString(webPage);
	}

	public String prettyPrintJSON(List<ProblemListEntry> problemListEntries, Boolean pretty) throws JsonProcessingException {
		// Create the mapper
		ObjectMapper mapper = new ObjectMapper();

		// Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
		if (pretty) {
			DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
			DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
			printer.indentObjectsWith(indenter);
			printer.indentArraysWith(indenter);

			// Serialize it using the custom printer
			return mapper.writer(printer).writeValueAsString(problemListEntries);
		}

		// plain JSON print
		return mapper.writer().writeValueAsString(problemListEntries);
	}

	private HttpURLConnection httpRequest(String queryUrl, String queryMethod, Map<String, String> queryParam) throws Exception {
		System.out.println(language + " - queries: " + ++queryCount);
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

	/**
	 * <pre>Google翻譯</pre>
	 *
	 * @author catty
	 * @version 1.0, Created on 2011/9/2
	 *
	 * @param text
	 * @param srcLang 來源語系
	 * @param targetLang 目標語系
	 * @return
	 * @throws Exception
	 */
	public String translate(final String text, final String srcLang, final String targetLang)
			throws Exception {

		/* prevent unnecessary query
		 * do not translate aid
		 */

		if(srcLang.equals(targetLang) || text.equals("aid")){
			return text;
		}


		final String ENCODING = "UTF-8";
		final String ID_RESULTBOX = "result_box";
		Document doc = null;
		Element ele = null;

		try {
			// create URL string
			String queryUrl = MessageFormat.format(QueryUrl.GOOGLETRANSLATEURL.url(),
					URLEncoder.encode(srcLang + "|" + targetLang, ENCODING),
					URLEncoder.encode(text, ENCODING));

			String response = getResponse(queryUrl, "GET", buildQueryParam(new AutomdWebPage(), QueryUrl.GOOGLETRANSLATEURL.url()));

			System.out.println(response);

			// parse html by Jsoup
			doc = Jsoup.parse(response);
			ele = doc.getElementById("result_box");

			String result = ele != null ? ele.text() : "";
			return result;

		} finally {
			doc = null;
			ele = null;
		}
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
	 * init Pairs
	 * qid: original Url also has qid in the query parameters, but leaving it out
	 *  does not seem to affect the http response. The reason why it is left out is
	 *  because the qid value is only retrievable from the /diagnose/next_qna query
	 *  which originally requires the qid as a query parameter.
	 * option: original Url set as either 1 or 2, should not affect the http response. 
	 * 	default set to 1.
	 */
	private Map<String, String> buildQueryParam(AutomdWebPage webPage, String queryUrl){
		Map<String, String> queryParam = new HashMap<String, String>();

		if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/select_area")){
			// build vehicle queryParam
			
			// 2016 Toyota Avalon Hybrid Limited 4 Cyl 2.5L
			queryParam.put("year", "117");
			queryParam.put("make", "7");
			queryParam.put("model", "76");
			queryParam.put("submodel", "658");
			queryParam.put("engine", "77");
		}
		else if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/qna")){
			queryParam.put("aid", webPage.getAidPair().getVal());
			queryParam.put("option", "1");
		}
		else if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/next_qna")){
			queryParam.put("aid", webPage.getAidPair().getVal());
			queryParam.put("ajax", "1");
		}
		else if (queryUrl.equals(QueryUrl.BASEURL.url() + "/diagnose/problems")){
			queryParam.put("aid", webPage.getAidPair().getVal());
		}
		else if (queryUrl.equals(QueryUrl.GOOGLETRANSLATEURL.url())){
		}

		return queryParam;
	}
	
	public String getResponse(String queryUrl, String queryMethod, Map<String, String> queryParam) throws Exception {
		Query query = new Query();
		return query.getResponse((httpRequest(queryUrl, queryMethod, queryParam)));
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
		Map<String, String> queryParam = buildQueryParam(currWebPage, queryUrl);
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
		AutomdProblem problem = new AutomdProblem();
		
		Map<String, String> queryParam = Collections.emptyMap();
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
			System.out.println(language + " - inspectionSteps: " + ++inspectionStepCount);
		}

		problem.setTitle(title);
		problem.setDescription(description);
		problem.setInspectionSteps(inspectionSteps);
		
		/*System.out.println("title: " + title);
		System.out.println("description: " + description);
		System.out.println("inspectionSteps: " + inspectionSteps);*/
		
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
				AutomdProblem problem = buildProblem(queryUrl);
				// do not add an invalid problem
				if(!problem.getDescription().equals("AutoMD™ Disclaimer:" +
						" AutoMD is for informational use only and is intended to be used as a guide." +
						" We do not take any responsibility for automotive service decisions or automotive" +
						" work decided upon as a result of using AutoMD. Always consult a certified" +
						" automotive mechanic before making important automotive repair and service decisions." +
						" Use our Service Shop Finder to locate service shops and mechanics near you.")){

					problemList.add(problem);
				}
				else{
					System.out.println(language + " - invalidProblemCount: " + ++invalidProblemCount + " w/ key: " + currWebPage.getAidPair().getKey());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return problemList;
	}

	public AutomdWebPage repairAndTranslate(AutomdWebPage icWebPage, String srcLang, String targetLang) throws Exception {

		/*System.out.println("in repairAndTranslate!" +
				"\nkey: " + icWebPage.getAidPair().getKey() +
				"\nicWebPage.getChildWebPageList.size(): " + icWebPage.getChildWebPageList().size() +
				"\nicWebPage.getProblemList.size(): " + icWebPage.getProblemList().size());*/

		AutomdWebPage webPage = icWebPage;
		List<AutomdWebPage> childWebPageList = new ArrayList<AutomdWebPage>();

		/* base case
		 *
		 * validate leaf node &&
		 * empty problemList &&
		 * problemListLibrary contains a valid corresponding problemList
		 */
		if(webPage.getChildWebPageList().size() == 0){ // is a leaf node

			List<AutomdProblem> problemList = new ArrayList<AutomdProblem>();

			if(webPage.getProblemList().size() != 0){
				problemList = webPage.getProblemList();
			}
			else if(webPage.getProblemList().size() == 0 &&
				problemListLibrary.containsKey(webPage.getAidPair().getKey())) { // need to repair problemList

				System.out.println("repairing: " + webPage.getAidPair().getKey() + " : " + webPage.getAidPair().getVal());
				problemList = problemListLibrary.get(webPage.getAidPair().getKey());
			}

			// translate problemList
			List<AutomdProblem> translatedProblemList = new ArrayList<AutomdProblem>();
			for(AutomdProblem problem : problemList){

				AutomdProblem translatedProblem = problem;

				List<String> translatedInspectionSteps = new ArrayList<String>();
				for(String inspectionStep : problem.getInspectionSteps()){
					translatedInspectionSteps.add(translate(inspectionStep, srcLang, targetLang));
				}

				//System.out.println("translatedInspectionSteps.size(): " + translatedInspectionSteps.size());

				translatedProblem = new AutomdProblem(
						translate(problem.getTitle(), srcLang, targetLang),
						translate(problem.getDescription(), srcLang, targetLang),
						translatedInspectionSteps
				);

                translatedProblemList.add(translatedProblem);
			}

			webPage = new AutomdWebPage(
					translate(webPage.getTitle(), srcLang, targetLang),
					new Pair<String, String>(translate(webPage.getAidPair().getKey(), srcLang, targetLang),
							webPage.getAidPair().getVal()),
					childWebPageList,
					translatedProblemList
			);

			return webPage;
		}

		icWebPage.getChildWebPageList().parallelStream().forEach(icChildWebPage -> {
			try {

				AutomdWebPage childWebPage = icChildWebPage;

				//System.out.println("childWebPage.getTitle()): " + childWebPage.getTitle());

				List<AutomdWebPage> grandChildWebPageList = new ArrayList<AutomdWebPage>();

				icChildWebPage.getChildWebPageList().parallelStream().forEach(icGrandChildWebPage -> {
					try{
						//System.out.println("grandChildWebPage.getTitle()): " + grandChildWebPage.getTitle());

						AutomdWebPage grandChildWebPage = repairAndTranslate(icGrandChildWebPage, srcLang, targetLang);

						//System.out.println("grandChildWebPage.getAidPair().getKey(): " + grandChildWebPage.getAidPair().getKey());

						grandChildWebPageList.add(grandChildWebPage);

					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				childWebPage = new AutomdWebPage(
                        translate(childWebPage.getTitle(), srcLang, targetLang),
						new Pair<String, String>(translate(childWebPage.getAidPair().getKey(), srcLang, targetLang),
								childWebPage.getAidPair().getVal()),
						grandChildWebPageList
				);

				childWebPage.setChildWebPageList(grandChildWebPageList);

				childWebPageList.add(childWebPage);

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		webPage = new AutomdWebPage(
				translate(webPage.getTitle(), srcLang, targetLang),
				new Pair<String, String>(translate(webPage.getAidPair().getKey(), srcLang, targetLang),
						webPage.getAidPair().getVal()),
				childWebPageList
		);

		return webPage;
	}
	
	public List<AutomdWebPage> buildWebPageList(AutomdWebPage currWebPage, String queryUrl, String queryMethod) throws Exception {
		
		Map<String, String> queryParam = buildQueryParam(currWebPage, queryUrl);
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

			if(queryParam.size() != 0 && queryParam.containsKey("aid")){
				//System.out.println("queryParam.get(0): " + queryParam.get(0).getKey() + " : " + queryParam.get(0).getVal());
				webPage.setAidPair(new Pair<String, String>("aid", queryParam.get("aid")));
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

					/* properly format dash
					 * google translate requires ' - '
					 */
					Integer indexOfDash = 0;
					if(key.indexOf(" - ") == -1 &&
							(
									(indexOfDash = key.indexOf("- ")) != -1 || (indexOfDash = key.indexOf(" -")) != -1
							)

                        ) {

						key = key.substring(0, indexOfDash) +
								" - " +
								key.substring(indexOfDash + 2, key.length() - 1);
					}
					
					System.out.println("childWebPage.aidPair added!: " + key + " : " + val);
					
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

	/* icChildWebPageList:
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
				
				AutomdWebPage childWebPage = icChildWebPage;

				//System.out.println("childWebPage.getTitle()): " + childWebPage.getTitle());
				
				List<AutomdWebPage> grandChildWebPageList = new ArrayList<AutomdWebPage>();
				
				icChildWebPage.getChildWebPageList().parallelStream().forEach(icGrandChildWebPage -> {
					try{

						AutomdWebPage grandChildWebPage = icGrandChildWebPage;

						//System.out.println("grandChildWebPage.getTitle()): " + grandChildWebPage.getTitle());

						// need to optimize this, currently does two queries each time (at most)
						List<AutomdWebPage> gGrandChildWebPageListQna = buildChildWebPageList(grandChildWebPage, QueryUrl.BASEURL.url() + "/diagnose/qna", "GET", amdClient);

						grandChildWebPage.setChildWebPageList( gGrandChildWebPageListQna.size() != 0 ? gGrandChildWebPageListQna : buildChildWebPageList(grandChildWebPage, QueryUrl.BASEURL.url() + "/diagnose/next_qna", "GET", amdClient));
						
						// (step 3 -> step 4)
						if(grandChildWebPage.getChildWebPageList().size() == 0){
							List<AutomdProblem> problemList = amdClient.buildProblemList(grandChildWebPage);

							if(problemList.size() != 0){
								grandChildWebPage.setProblemList(problemList);

								System.out.println(language + " - problems: " + (problemCount += problemList.size()));
							}

							/* store into problemListEntries iff:
							 *
							 * key != No
							 * key != Yes
							 * 	We cannot store No and Yes keys because they are too common.
							 * 	Doing so is redundant and it is also impossible to replace common keys
							 * 	with different problemLists.
							 *
							 * 	The ideal method would be to have each problem attributed to a key...?
							 *
							 */
							if(problemList.size() != 0 &&
									!grandChildWebPage.getAidPair().getKey().equals("Yes") &&
									!grandChildWebPage.getAidPair().getKey().equals("No")
                                ){

								problemListLibrary.put(grandChildWebPage.getAidPair().getKey(), problemList);
							}
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
