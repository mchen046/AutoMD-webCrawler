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

// Quartz library
public class renewAccessTokenJob implements org.quartz.Job {

	public renewAccessTokenJob() {
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			renewAccessToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

public void scheduleRenewAccessToken() throws SchedulerException, Exception{
	// initial access_token set
	renewAccessToken();

	// schedule renewAccessToken()
	Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

	// define the job and tie it to our renewAccessTokenJob class
	JobDetail job = newJob(renewAccessTokenJob.class)
			.withIdentity("job1", "group1")
			.build();

	// Trigger the job to run now, and then repeat a minute before expiration
	Trigger trigger = newTrigger()
			.withIdentity("trigger1", "group1")
			.startNow()
			.withSchedule(simpleSchedule()
					// access_token expires every 10 minutes
					.withIntervalInMinutes(9)
					.repeatForever())
			.build();

	// Tell quartz to schedule the job using our trigger
	scheduler.scheduleJob(job, trigger);
}
// Microsoft Translation API Implementation
private String buildTranslateArrayRequestBody(List<String> sourceTexts){
	String from = LanguageCode.ENGLISH.code();
	String ContentType = "text/plain";
	String to = language;

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

	return requestBodyPrefix + sourceTextsXml + requestBodySuffix;
}

public List<String> buildTranslations(List<String> sourceTexts) throws Exception{
	String requestBody = buildTranslateArrayRequestBody(sourceTexts);

	AutomdWebPage fakeWebPage = new AutomdWebPage();
	fakeWebPage.setTitle(requestBody);

	String response = getResponse(QueryUrl.TRANSLATEARRAYURL.url(), "POST", buildQueryParam(fakeWebPage, QueryUrl.TRANSLATEARRAYURL.url()));
	Document doc = Jsoup.parse(response, "", Parser.xmlParser());

	List<String> translations = new ArrayList<String>();

	Elements translateArrayResponses = doc.select("TranslateArrayResponse");
	System.out.println("translateArrayResponses.size(): " + translateArrayResponses.size());

	//int i = 0;
	for(Element translateArrayResponse : translateArrayResponses) {
		String translatedText = translateArrayResponse.select("TranslatedText").text();
		System.out.println("translatedText: " + translatedText);
		//Pair<String, String> translation = new Pair<String, String>(sourceTexts.get(i++), TranslatedText);
		translations.add(translatedText);
		translationCharCount += translatedText.length();
	}

	System.out.println(language + " - translationCharCount: " + translationCharCount);

	return translations;
}

//in buildQueryParam
else if (queryUrl.equals(QueryUrl.TRANSLATEARRAYURL.url())) { // TranslateArray method api
	queryParam.put("requestBody", webPage.getTitle());
}
else if (queryUrl.equals(QueryUrl.ACCESSTOKENURL.url())){ // microsoft access token post
	queryParam.put("grant_type", "client_credentials");
	queryParam.put("client_id", "AutoMD-webCrawler");
	queryParam.put("client_secret", "JBKJ32B4J23B4N23B4VXLVXL342HJ34");
	queryParam.put("scope", "http://api.microsofttranslator.com");
}

public void renewAccessToken() throws Exception {
		System.out.println("renewing token!");
		Query query = new Query();
		AutomdWebPage emptyWebPage = new AutomdWebPage();
		query.renewAccessToken(httpRequest(QueryUrl.ACCESSTOKENURL.url(), "POST", buildQueryParam(emptyWebPage, QueryUrl.ACCESSTOKENURL.url())));
}

//in buildProblem
// translate if language is not set to English
if(!language.equals(LanguageCode.ENGLISH.code())){
	List<String> sourceTexts = new ArrayList<String>();

	sourceTexts.add(title);
	sourceTexts.add(description);

	List<String> translations = buildTranslations(sourceTexts);

	title = translations.get(0);
	description = translations.get(1);
	inspectionSteps = buildTranslations(inspectionSteps);
}

private AutomdWebPage initTitleAndAidPair(AutomdWebPage icWebPage, String srcLang, String targetLang) throws Exception{
    AutomdWebPage webPage = new AutomdWebPage();

	String webPageTitle = icWebPage.getTitle();
    String webPageAidPairKey = icWebPage.getAidPair().getKey();

	// translate if srcLang and targetLang are different
	if(!srcLang.equals(targetLang)){
    	// Microsoft Translation API implementation
        List<String> sourceTexts = new ArrayList<String>();

        sourceTexts.add(webPageTitle);
        sourceTexts.add(webPageAidPairKey);

        List<String> translations = buildTranslations(sourceTexts);

        webPageTitle = translations.get(0);
        webPageAidPairKey = translations.get(1);

		System.out.println("webPageTitle: " + webPageTitle);
		System.out.println("webPageAidPairKey: " + webPageAidPairKey);

        // Google Translate API implementation
		webPageTitle = translate(webPageTitle, srcLang, targetLang);
		webPageAidPairKey = translate(webPageAidPairKey, srcLang, targetLang);
    }

	webPage.setTitle(webPageTitle);

	Pair<String, String> webPageAidPair =
			new Pair<String, String>(webPageAidPairKey, icWebPage.getAidPair().getVal());

	webPage.setAidPair(webPageAidPair);

	return webPage;
}

// Query.buildQueryString
// special case: queryString is requestBody, of the form xml
if(queryParam.size() == 1 && queryParam.get(0).getKey().equals("requestBody")){
    queryString = queryParam.get(0).getVal();
}

// in Query
public void renewAccessToken(HttpURLConnection con) throws Exception {
    if(con instanceof HttpsURLConnection){
        //System.out.println("casting to HttpsURLConnection!");
        con = (HttpsURLConnection)con;
        //httpsCon.setHostnameVerifier();
    }

    String response = getResponse(con);

    ObjectMapper objectMapper = new ObjectMapper();

    MicrosoftTranslatorToken token = objectMapper.readValue(response, MicrosoftTranslatorToken.class);

    access_token = token.access_token;

    System.out.println("access_token: " + access_token);
}