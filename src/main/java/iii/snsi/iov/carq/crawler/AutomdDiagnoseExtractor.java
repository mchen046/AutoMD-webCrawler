package iii.snsi.iov.carq.crawler;

import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AutomdDiagnoseExtractor {
	public static AutomdProblem extractProblem(HtmlPage page) {
		String problemBlocksXpath = "/html/body/div[2]/div[1]/div[1]/div";
		/*List<HtmlDivision> problemBlocks = (List<HtmlDivision>)page.getByXPath(problemBlocksXpath);
		problemBlocks.forEach(block-> {
			AutomdProblem problem = new AutomdProblem();
			HtmlAnchor titleAnchor = block.getFirstByXPath("div[2]/div/div[2]/div/div[1]/p[1]/strong/a"); //title
			System.out.println(block.asXml());
			
			problem.setTitle(titleAnchor.getNodeValue());
			problem.setInspectUrl(titleAnchor.getHrefAttribute());
			
		});*/
		return null;
		
	}
}
