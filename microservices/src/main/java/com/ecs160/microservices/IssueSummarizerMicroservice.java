package com.ecs160.microservices;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

import java.io.IOException;
import com.google.gson.Gson;
import com.ecs160.model.Issue;
import com.ecs160.ollama.OllamaClient;
import com.ecs160.ollama.OllamaClientInterface;

@Microservice
public class IssueSummarizerMicroservice {
    private OllamaClientInterface ollamaClient;
    private String model = "deepcoder:1.5b";
    private Gson gson;

    public IssueSummarizerMicroservice() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.gson = new Gson();
    }

    public IssueSummarizerMicroservice(OllamaClientInterface ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.gson = new Gson();
    }

    @Endpoint(url = "/summarize_issue")
    public String summarizeIssue(String issueJson) {
        try {
            // prep prompt
            String prompt = "You are an AI agent that summarizes GitHub issues into structured bug reports. " + 
            "You must follow these rules: Only return a single JSON object. Do not include explanations, reasoning, or any text outside of the JSON. " + 
            "Do not include \"<think>\" sections. Do not wrap the JSON text in \"```json```\" formatting. The JSON format must exactly be: \n" +
            "{\n " + 
            "  \"bug_type\": \"<type of bug>\",\n" +
            "  \"line\": <line number or '0' if unknown>,\n" +
            "  \"description\": \"<brief description>\",\n" +
            "  \"filename\": \"<filename or 'unknown'>\"\n" +
            "}" +
            "Here's the GitHub Issue : \n " + issueJson + "\n\n";

            // get response from ollama
            String result = ollamaClient.generate(model, prompt);
            Issue issue = parseIssue(result);

            return gson.toJson(issue);
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Request interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Ollama exception: " + e.getMessage());
        }
        Issue defaultError = new Issue(
            "ProcessingError",
            0,
            "Failed to summarize issue",
            "unknown"
        );

        return gson.toJson(defaultError);
    }

    // get Issue obj from agent response
    private Issue parseIssue(String response) {
        try {
            // find {}
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
    
            if (start == -1 || end == -1 || end < start) {
                return new Issue(
                    "Generic Issue",
                    0,
                    "Failed to parse: " + response.substring(0, Math.min(100, response.length())),
                    "unknown"
                );
            }
    
            String jsonStr = response.substring(start, end + 1);
    
            // parse json into Issue
            Issue issue = gson.fromJson(jsonStr, Issue.class);
    
            // set default value to null fields
            if (issue.getBugType() == null) issue.setBugType("Generic Issue");
            if (issue.getDescription() == null) issue.setDescription("Issue extracted from GitHub");
            if (issue.getFilename() == null) issue.setFilename("unknown");
            if (issue.getLine() < 0) issue.setLine(0);
    
            return issue;
        } catch (Exception e) {
            return new Issue(
                "Generic Issue",
                0,
                "Failed to parse: " + response.substring(0, Math.min(100, response.length())),
                "unknown"
            );
        }
    }
}

