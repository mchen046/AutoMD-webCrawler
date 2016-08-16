package iii.snsi.iov.carq.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by myco on 8/15/16.
 *
 * for converting to a from a file of AutomdWebPage nested object
 */
public class ProblemListEntry {
    private String key = "";

    private List<AutomdProblem> problemList = new ArrayList<AutomdProblem>();

    public ProblemListEntry(@JsonProperty("key") String key, @JsonProperty("problemList") List<AutomdProblem> problemList) {
        this.key = key;
        this.problemList = problemList;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<AutomdProblem> getProblemList() {
        return problemList;
    }

    public void setProblemList(List<AutomdProblem> problemList) {
        this.problemList = problemList;
    }
}
