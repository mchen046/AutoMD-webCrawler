package iii.snsi.iov.carq.crawler;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

//import org.junit.Test;

//import com.gargoylesoftware.htmlunit.html.HtmlPage;

import iii.snsi.iov.carq.crawler.AutomdDiagnoseClient;

public class AutomdDiagnoseClientTest {

	private final String baseUrl = "https://www.automd.com";
	
	// custom 4 space indent JSON pretty printer: http://stackoverflow.com/a/28261746
	private String prettyPrintJSON(AutomdWebPage webPage) throws JsonProcessingException{
		// Create the mapper
		ObjectMapper mapper = new ObjectMapper();

		// Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
		DefaultPrettyPrinter.Indenter indenter = 
		        new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);

		// Serialize it using the custom printer
		return mapper.writer(printer).writeValueAsString(webPage);
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
	 */
	
	/* icChildWebPageList:
	 * 	incomplete childWebPageList
	 *  contains only the parent AutomdWebPage fields, need to further populate the nested
	 *  values -> childWebPageList
	 * gGrandChildWebPageList:
	 * 	great grand childWebPageList
	 */
	private List<AutomdWebPage> buildChildWebPageList(AutomdWebPage currWebPage, String queryUrl, String queryMethod, AutomdDiagnoseClient amdClient, BufferedWriter debug) throws Exception{
		//System.out.println(currWebPage.getAidPair().getKey() + " : " + currWebPage.getAidPair().getVal() + "\n");
		// grab all section block webPages with a title header on the currWebPage
		List<AutomdWebPage> icChildWebPageList = amdClient.buildWebPageList(currWebPage, queryUrl, queryMethod);
		
		List<AutomdWebPage> childWebPageList = new ArrayList<AutomdWebPage>();
		
		//System.out.println(String.format("icChildWebPageList.size() (children retreived from %s): %d", queryUrl, icChildWebPageList.size()));
		
		if(icChildWebPageList.size() == 1 && icChildWebPageList.get(0).getChildWebPageList().size() == 0){ // base case
			return childWebPageList;
		}

		/* for each webPage section with a header. there is only more than 1 section on the first page.
		 * it is written as O(n^2), but runtime is actually more closely resembled by O(n)
		 */
		for(AutomdWebPage icChildWebPage : icChildWebPageList){ 
			
			// init new thread to handle creation of: childWebPage
			
			AutomdWebPage childWebPage = new AutomdWebPage();
			childWebPage.setTitle(icChildWebPage.getTitle());
			childWebPage.setAidPair(icChildWebPage.getAidPair());
			
			List<AutomdWebPage> grandChildWebPageList = new ArrayList<AutomdWebPage>();
			
			for(AutomdWebPage icGrandChildWebPage : icChildWebPage.getChildWebPageList()){
				
				// init new thread to handle creation of: grandChildWebPage
				
				AutomdWebPage grandChildWebPage = new AutomdWebPage();
				grandChildWebPage.setTitle(icGrandChildWebPage.getTitle());
				grandChildWebPage.setAidPair(icGrandChildWebPage.getAidPair());
				
				// need to optimize this, currently does two queries each time
				List<AutomdWebPage> gGrandChildWebPageListQna = buildChildWebPageList(grandChildWebPage, baseUrl + "/diagnose/qna", "GET", amdClient, debug);

				grandChildWebPage.setChildWebPageList( gGrandChildWebPageListQna.size() != 0 ? gGrandChildWebPageListQna : buildChildWebPageList(grandChildWebPage, baseUrl + "/diagnose/next_qna", "GET", amdClient, debug));
				
				/* (step 3 -> step 4)
				 * insert problem webPage / problem object into childWebPageList (should be the only object in the list)
				 */
				if(grandChildWebPage.getChildWebPageList().size() == 0){
					grandChildWebPage.setProblemList(amdClient.buildProblemList(grandChildWebPage));
				}
				
				grandChildWebPageList.add(grandChildWebPage);
			}

			childWebPage.setChildWebPageList(grandChildWebPageList);
			
			childWebPageList.add(childWebPage);
		}
		
		return childWebPageList;
	}
	
	public void testInit() throws Exception {
		AutomdDiagnoseClient amdClient = new AutomdDiagnoseClient();
		amdClient.init(true);
		
		// set initial cookie (preserves car selection)
		//String cookie = amdClient.getCookie(baseUrl + "/diagnose/select_area", "POST", vehicleQueryParam);
		amdClient.setCookie(baseUrl + "/diagnose/select_area", "POST");
		
		// debug file write
		File debugFile = new File("debug.txt");
		FileWriter debugFw = new FileWriter(debugFile.getAbsoluteFile());
		BufferedWriter debug = new BufferedWriter(debugFw);
	
		AutomdWebPage masterWebPage = new AutomdWebPage();
		
		List<AutomdWebPage> childWebPageList = buildChildWebPageList(masterWebPage, baseUrl + "/diagnose/select_area", "POST", amdClient, debug);
		
		masterWebPage.setTitle("webPages");
		// empty aidPair set in class declaration
		masterWebPage.setChildWebPageList(childWebPageList);

		debug.close();

		// pretty print JSON to "aids.json"
		File file = new File("webPages.json");
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
	
		bw.write(prettyPrintJSON(masterWebPage));
		
		bw.close();
		
		amdClient.close();
	}
}
