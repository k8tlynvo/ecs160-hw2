package com.ecs160.microservices;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

import com.google.gson.Gson;
import com.ecs160.model.Issue;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;

@Microservice
public class IssueComparatorMicroservice {
    private Gson gson;

    public IssueComparatorMicroservice() {
        this.gson = new Gson();
    }

    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String issueJSonArray) {
        // accepts a list of two lists of issues in json format and returns a json list of issues that are both in common  

        try {
            JsonArray outerArray = gson.fromJson(issueJSonArray, JsonArray.class);
    
            if (outerArray == null || outerArray.size() != 2) {
                return "[]";
            }
    
            // parse first list
            JsonArray arr1 = outerArray.get(0).getAsJsonArray();
            List<Issue> list1 = new ArrayList<>();
            for (int i = 0; i < arr1.size(); i++) {
                Issue issue = gson.fromJson(arr1.get(i), Issue.class);
                list1.add(issue);
            }
    
            // parse second list
            JsonArray arr2 = outerArray.get(1).getAsJsonArray();
            List<Issue> list2 = new ArrayList<>();
            for (int i = 0; i < arr2.size(); i++) {
                Issue issue = gson.fromJson(arr2.get(i), Issue.class);
                list2.add(issue);
            }
    
            // get common issues
            List<Issue> common = new ArrayList<>();
            for (Issue issue : list1) {
                if (containsIssue(list2, issue)) {
                    common.add(issue);
                }
            }
    
            // return json list of common issues
            return gson.toJson(common);
    
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    // check if Issue list contains target issue
    private boolean containsIssue(List<Issue> list, Issue target) {
        for (Issue issue : list) {
            if (issue.equals(target)) {
                return true;
            }
        }
        return false;
    }
}

