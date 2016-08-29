package iii.snsi.iov.carq.crawler;

import java.util.List;

public class AutomdProblem {
	private String title;
	private String description;
	private List<String> inspectionList;

	public AutomdProblem() {
	}

	public AutomdProblem(String title, String description, List<String> inspectionList) {
		this.title = title;
		this.description = description;
		this.inspectionList = inspectionList;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getInspectionList() {
		return inspectionList;
	}
	public void setInspectionList(List<String> inspectionList) {
		this.inspectionList = inspectionList;
	}
}
