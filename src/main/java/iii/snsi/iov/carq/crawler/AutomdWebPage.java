package iii.snsi.iov.carq.crawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* An object of the AutomdWebPage class is defined as a webPage,
 * where a webPage is defined as a a section block that has
 * 	an aidPair (if exists from the previous page query)
 *  a title
 * 	labels (aidPairs)
 * 
 * The initial loaded webPage for step 2 /diagnose/select_area
 * does not have a previous query as it is the first loaded page, therefore, 
 * it does not have an aidPair. Incidentally, all 'webPages' on
 * /diagnose/select_area do not have a single associated aidPair.
 */


public class AutomdWebPage {
	private String title = "";
	private Pair<String, String> aidPair = new Pair<String, String>("", "");
	private List<AutomdWebPage> childWebPageList = Collections.<AutomdWebPage> emptyList();
	private List<AutomdProblem> problemList = new ArrayList<AutomdProblem>();

	public AutomdWebPage() {
	}


	public AutomdWebPage(String title, Pair<String, String> aidPair, List<AutomdWebPage> childWebPageList) {
		this.title = title;
		this.aidPair = aidPair;
		this.childWebPageList = childWebPageList;
	}

	public AutomdWebPage(String title, Pair<String, String> aidPair, List<AutomdWebPage> childWebPageList, List<AutomdProblem> problemList) {
		this.title = title;
		this.aidPair = aidPair;
		this.childWebPageList = childWebPageList;
		this.problemList = problemList;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Pair<String, String> getAidPair() {
		return aidPair;
	}
	public void setAidPair(Pair<String, String> aidPair) {
		this.aidPair = aidPair;
	}
	public List<AutomdWebPage> getChildWebPageList() {
		return childWebPageList;
	}
	public void setChildWebPageList(List<AutomdWebPage> childWebPageList) {
		this.childWebPageList = childWebPageList;
	}
	public List<AutomdProblem> getProblemList() {
		return problemList;
	}
	public void setProblemList(List<AutomdProblem> problemList) {
		this.problemList = problemList;
	}
}