package com.ecs160.microservices;

import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;

import com.google.gson.Gson;

import com.ecs160.model.Issue;
import com.ecs160.ollama.OllamaClient;
import com.ecs160.ollama.OllamaClientInterface;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

@Microservice
public class BugFinderMicroservice {
    private OllamaClientInterface ollamaClient;
    private String model = "deepcoder:1.5b"; 
    private Gson gson;

    public BugFinderMicroservice() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.gson = new Gson();
    }

    public BugFinderMicroservice(OllamaClientInterface ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.gson = new Gson();
    }

    @Endpoint(url = "/find_bugs")
    public String findBugs(String code) {
        // accepts C file contents and returns a list of json Issues
        try {
            // prep prompt
            String prompt = "You are an AI agent that analyzes C code. Analyze the following C code and identify all potential bugs, errors, and security vulnerabilities. " + 
            "You must follow these rules in the response: If NO bugs are found, return an empty array []. If there are bugs in the code, only return a valid JSON array of issue objects. Do not include explanations, reasoning, or any text outside of the JSON. " + 
            "If a pointer is NULL and dereferenced, always mark it as a null pointer bug. " +
            "Do not include \"<think>\" sections. Do not wrap the JSON text in \"```json```\" formatting. The JSON format for the issue objects must exactly be: \n" +
            "[\n" +
            "  {\n" +
            "    \"bug_type\": \"<specific bug type like NullPointerException, BufferOverflow, MemoryLeak, if it's not a bug then return an empty array!>\",\n" +
            "    \"line\": <line number as integer>,\n" +
            "    \"description\": \"<brief one-line description>\",\n" +
            "    \"filename\": \"<file name or the string 'none' if none is provided>\"\n" +
            "  }\n" +
            "]\n\n" +
            "Here's the C code : \n " + code + "\n\n";

            // get response from ollama
            String result = ollamaClient.generate(model, prompt);
            List<Issue> issues = parseIssues(result);

            return gson.toJson(issues);
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Request interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Ollama exception: " + e.getMessage());
        }

        List<Issue> issues = new ArrayList<>();
         Issue defaultError = new Issue(
            "ProcessingError",
            0,
            "Failed to summarize issue",
            "unknown"
        );
        issues.add(defaultError);

        return gson.toJson(defaultError);

    }

    // get issues from agent response
    private List<Issue> parseIssues(String response) {
        List<Issue> issues = new ArrayList<>();
        
        // find all {} pairs
        int pos = 0;
        while (pos < response.length()) {
            int start = response.indexOf('{', pos);
            if (start == -1) break;
            
            int end = response.indexOf('}', start);
            if (end == -1) break;
            
            try {
                // extract and parse this issue
                String jsonStr = response.substring(start, end + 1);
                
                Issue issue = gson.fromJson(jsonStr, Issue.class);
                
                // set default values for null fields
                if (issue.getBugType() == null) issue.setBugType("GenericBug");
                if (issue.getDescription() == null) issue.setDescription("Bug detected");
                if (issue.getFilename() == null) issue.setFilename("none");
                if (issue.getLine() < 0) issue.setLine(0);
                
                issues.add(issue);
            } catch (Exception e) {
                // skip any malformed issue json
            }
            
            pos = end + 1;
        }
        
        return issues;
    }
}

