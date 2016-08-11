package iii.snsi.iov.carq.crawler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

import iii.snsi.iov.carq.crawler.AutomdDiagnoseClient.Pair;
import iii.snsi.iov.carq.crawler.AutomdDiagnoseClient.MicrosoftTranslatorToken;

// https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
public class Query {

	private final String USER_AGENT = "Mozilla/5.0";
	private static String cookie = "";
	private static String access_token = "";
	
	private String buildQueryString(List<Pair<String, String>> queryParam){
		String queryString = "";
		
		for(Iterator<Pair<String, String>> it = queryParam.iterator(); it.hasNext();){
			Pair<String, String> pair = it.next();
			queryString += pair.getKey() + "=" + pair.getVal();
			if(it.hasNext()){ // not the last element
				queryString += "&";
			}
		}
		
		return queryString;
	}

    public void renewAccessToken(HttpURLConnection con) throws Exception {

        if(con instanceof HttpsURLConnection){
            System.out.println("casting to HttpsURLConnection!");
            con = (HttpsURLConnection)con;
            //httpsCon.setHostnameVerifier();
        }

        String response = getResponse(con);

        ObjectMapper objectMapper = new ObjectMapper();

        MicrosoftTranslatorToken token = objectMapper.readValue(response, MicrosoftTranslatorToken.class);

        access_token = token.access_token;

        System.out.println("access_token: " + access_token);
    }

    public void setCookie(HttpURLConnection con) throws Exception{

        if(con instanceof HttpsURLConnection){
            System.out.println("casting to HttpsURLConnection!");
            con = (HttpsURLConnection)con;
            //httpsCon.setHostnameVerifier();
        }

        List<String> cookies = con.getHeaderFields().get("Set-Cookie");
        String cookieDiagnose = "", cookieSavedVehicles = "";
        for(String cookieSection : cookies){
        /*System.out.println(cookieSection.substring(0, 9));
        System.out.println(cookieSection.substring(0, 17));*/

            if(cookieSection.substring(0, 9).equals("diagnose=")){
                cookieDiagnose = cookieSection;
                System.out.println(cookieSection);
            }
            else if(cookieSection.substring(0, 17).equals("AMDSavedVehicles=")){
                cookieSavedVehicles = cookieSection;
                System.out.println(cookieSection);
            }
        }
        cookie = cookieDiagnose + ", " + cookieSavedVehicles;
        System.out.println("setCookie: " + cookie);
    }

    public String getResponse(HttpURLConnection con) throws Exception{

        if(con instanceof HttpsURLConnection){
            System.out.println("casting to HttpsURLConnection!");
            con = (HttpsURLConnection)con;
            //httpsCon.setHostnameVerifier();
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    // HTTP GET request
    public HttpURLConnection httpGet(String queryUrl, List<Pair<String, String>> queryParam) throws Exception {

        String queryString = buildQueryString(queryParam);

        String url = queryUrl;
        if(queryString.length() != 0){
            // question mark separates base url from query parameters
            url += "?" + queryString;
        }

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection)obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);

        if(queryUrl.equals(QueryUrl.TRANSLATEURL.url())){ // add access_token request header
            con.setRequestProperty("Authorization", "Bearer" + " " + access_token);
        }

        if(cookie != ""){ // set cookie
            con.setRequestProperty("Cookie", cookie);
        }

        //int responseCode = con.getResponseCode();
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);
        //System.out.println("response : " + getResponse(con));

        return con;
    }

    // HTTP POST request
    public HttpURLConnection httpPost(String queryUrl, List<Pair<String, String>> queryParam) throws Exception {

        String queryString = buildQueryString(queryParam);

        String url = queryUrl;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnectionn) obj.openConnection();

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        // send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(queryString);
        wr.flush();
        wr.close();

        //int responseCode = con.getResponseCode();
        //System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Post param : " + queryString);
        //System.out.println("Response Code : " + responseCode);

        return con;
    }
}