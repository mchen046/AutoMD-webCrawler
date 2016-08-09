package iii.snsi.iov.carq.crawler;

import java.util.List;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import iii.snsi.iov.carq.crawler.AutomdDiagnoseClient;

public class AutomdDiagnoseClientTest {

	private final String baseUrl = "https://www.automd.com";
	
	public void testInit() throws Exception {
		AutomdDiagnoseClient amdClient = new AutomdDiagnoseClient();
		amdClient.init(true);
		
		// set initial cookie (preserves car selection)
		//String cookie = amdClient.getCookie(baseUrl + "/diagnose/select_area", "POST", vehicleQueryParam);
		amdClient.setCookie(baseUrl + "/diagnose/select_area", "POST");
		
		AutomdWebPage masterWebPage = new AutomdWebPage();
		
		List<AutomdWebPage> childWebPageList = amdClient.buildChildWebPageList(masterWebPage, baseUrl + "/diagnose/select_area", "POST", amdClient);
		
		masterWebPage.setTitle("webPages");
		// empty aidPair set in class declaration
		masterWebPage.setChildWebPageList(childWebPageList);

		// pretty print JSON to "aids.json"
		File file = new File("webPages.json");
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
	
		bw.write(amdClient.prettyPrintJSON(masterWebPage));
		
		bw.close();
		
		amdClient.close();
	}
}
