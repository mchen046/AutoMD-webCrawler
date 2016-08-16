package iii.snsi.iov.carq.crawler;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

public class AutomdDiagnoseClientTest {

    private void printToFile(AutomdDiagnoseClient amdClient, AutomdWebPage masterWebPage, String fileName, Boolean pretty) throws IOException {
		File file = new File(fileName);
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		bw.write(amdClient.prettyPrintJSON(masterWebPage, pretty));

		bw.close();
	}

	private void printToFile(AutomdDiagnoseClient amdClient, List<ProblemListEntry> problemListEntries, String fileName, Boolean pretty) throws IOException {
		File file = new File(fileName);
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		bw.write(amdClient.prettyPrintJSON(problemListEntries, pretty));

		bw.close();
	}

    private AutomdWebPage buildWebPageByQuery(AutomdDiagnoseClient amdClient) throws Exception {

		// set initial cookie (preserves car selection)
		amdClient.setCookie(QueryUrl.BASEURL.url() + "/diagnose/select_area", "POST");

		AutomdWebPage masterWebPage = new AutomdWebPage();

		List<AutomdWebPage> childWebPageList = amdClient.buildChildWebPageList(masterWebPage, QueryUrl.BASEURL.url() + "/diagnose/select_area", "POST", amdClient);

		masterWebPage.setTitle("webPages");
		masterWebPage.setChildWebPageList(childWebPageList);

		return masterWebPage;
	}

	private AutomdWebPage buildWebPageByFile(String language) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		AutomdWebPage masterWebPage = mapper.readValue(new File("/Users/myco/Documents/III/crawler/webPages_" + language + "_plain.json"), AutomdWebPage.class);
		return masterWebPage;
	}

	// problemListEntries_language_plain.json -> List<ProblemListEntry> -> HashMap<String, List<AutomdProblem>
	private HashMap<String, List<AutomdProblem>> buildProblemListLibraryByFile(String language) throws IOException{

		ObjectMapper mapper = new ObjectMapper();
		JavaType listOfProblemListEntryType = mapper.getTypeFactory().constructCollectionType(List.class, ProblemListEntry.class);

		List<ProblemListEntry> problemListEntries =
				mapper.readValue(new File("/Users/myco/Documents/III/crawler/problemListEntries_" +
						language +
						"_plain.json"), listOfProblemListEntryType);

		HashMap<String, List<AutomdProblem>>  problemListLibrary = new HashMap<String, List<AutomdProblem>>();

		for(ProblemListEntry problemListEntry : problemListEntries){
			problemListLibrary.put(problemListEntry.getKey(), problemListEntry.getProblemList());
		}

		return problemListLibrary;
	}

	private List<ProblemListEntry> buildProblemListEntries(HashMap<String, List<AutomdProblem>> problemListLibrary){
		List<ProblemListEntry> problemListEntries = new ArrayList<ProblemListEntry>();
		for (Map.Entry<String, List<AutomdProblem>> problemListEntry : problemListLibrary.entrySet()) {
			String key = problemListEntry.getKey();
			List<AutomdProblem> problemList = problemListEntry.getValue();
			problemListEntries.add(new ProblemListEntry(key, problemList));
		}
		return problemListEntries;
	}

	private void executeWithLanguage(String language) throws Exception {

	}

	public void testInit() throws Exception {
		AutomdDiagnoseClient amdClient = new AutomdDiagnoseClient();
		amdClient.init(true);

		String srcLang = LanguageCode.ENGLISH.code();

		amdClient.setLanguage(srcLang);

		AutomdWebPage masterWebPage = new AutomdWebPage();

		// --------------------------- saving respective queried objects -------------------------------

		masterWebPage = buildWebPageByQuery(amdClient);

		HashMap<String, List<AutomdProblem>> problemListLibrary = amdClient.getProblemListLibrary();

		List<ProblemListEntry> problemListEntries = buildProblemListEntries(problemListLibrary);

		printToFile(amdClient, problemListEntries, "problemListEntries_"  + srcLang + "_plain.json", false);
		printToFile(amdClient, problemListEntries, "problemListEntries__"  + srcLang + "_pretty.json", true);

		printToFile(amdClient, masterWebPage, "webPages_"  + srcLang + "_pretty.json", true);
		printToFile(amdClient, masterWebPage, "webPages_"  + srcLang + "_plain.json", false);

		// --------------------------- end saving respective queried objects -------------------------------

		// --------------------------- begin testing without querying -------------------------------

		/*masterWebPage = buildWebPageByFile(srcLang);

		HashMap<String, List<AutomdProblem>> problemListLibrary = buildProblemListLibraryByFile(srcLang);

		amdClient.setProblemListLibrary(problemListLibrary);*/

		// --------------------------- end testing without querying -------------------------------

		String targetLang = LanguageCode.TRADITIONAL_CHINESE.code();

		masterWebPage = amdClient.repairAndTranslate(masterWebPage, srcLang, targetLang);

		printToFile(amdClient, masterWebPage, "webPages_"  + targetLang + "_pretty_repaired.json", true);

		printToFile(amdClient, masterWebPage, "webPages_"  + targetLang + "_plain_repaired.json", false);

		amdClient.close();
	}
}
