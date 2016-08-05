package iii.snsi.iov.carq.crawler;

import java.util.List;

public class AutomdProblem {
	private String title;
	private String description;
	private List<String> inspectionSteps;
	
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
	public List<String> getInspectionSteps() {
		return inspectionSteps;
	}
	public void setInspectionSteps(List<String> inspectionSteps) {
		this.inspectionSteps = inspectionSteps;
	}
}
