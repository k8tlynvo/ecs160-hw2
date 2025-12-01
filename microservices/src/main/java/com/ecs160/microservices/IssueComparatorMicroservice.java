package com.ecs160.microservices;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

import com.google.gson.Gson;
import com.ecs160.model.Issue;
import java.util.Arrays;
import java.util.List;
import com.ecs160.ollama.OllamaClient;
import com.ecs160.ollama.OllamaClientInterface;
import java.io.IOException;

@Microservice
public class IssueComparatorMicroservice {
    private OllamaClientInterface ollamaClient;
    private String model = "deepcoder:1.5b";
    private Gson gson;

    public IssueComparatorMicroservice() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.gson = new Gson();
    }

    public IssueComparatorMicroservice(OllamaClientInterface ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.gson = new Gson();
    }

    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String issueJSonArray) {
        // accepts a list of two lists of issues in json format and returns a json list of issues that are both in common  
    
        try {
            String prompt = "You are an AI agent that checks two lists of Issues in Json format and returns a list of Json Issues that are common in both lists. " + 
            "You will be given an array of two arrays of Json Issue objects. You must find Issues that are similar between the two inner arrays. " +
            "Issues are considered similar if they have similar types, lines, and/or description. " +
            "You must follow these rules: Only return ONE array of JSON Issue objects. Do not include long explanations, reasoning, or any text outside of the array. " + 
            "Do not include \"<think>\" sections. The array format must exactly be: \n" +
            "[{\n " + 
            "  \"bug_type\": \"<type of bug>\",\n" +
            "  \"line\": <line number or '0' if unknown>,\n" +
            "  \"description\": \"<brief description>\",\n" +
            "  \"filename\": \"<filename or 'unknown'>\"\n" +
            "}, ... ]" +
            "Here's the two lists of Issues: \n " + issueJSonArray + "\n\n";

            // get response from ollama
            String result = ollamaClient.generate(model, prompt);
            System.out.println(result);
            List<Issue> issue = parseIssues(result);

            return gson.toJson(issue);
    
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Request interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Ollama exception: " + e.getMessage());
        }
        return "[]";
    }

    // get Issues from agent response
    public static List<Issue> parseIssues(String response) {
        try {
            if (response == null) return List.of();
    
            int start = response.indexOf('[');
            if (start == -1) return List.of();
    
            int end = response.lastIndexOf(']');
            if (end == -1 || end < start) return List.of();
    
            // extract json issues
            String json = response.substring(start, end + 1);
            Gson gson = new Gson();
            Issue[] issues = gson.fromJson(json, Issue[].class);
            return Arrays.asList(issues);
    
        } catch (Exception e) {
            System.out.println("Failed to parse issues: " + e.getMessage());
            return List.of();
        }
    }
}

