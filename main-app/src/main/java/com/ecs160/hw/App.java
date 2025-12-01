package com.ecs160.hw;

import com.ecs160.persistence.RedisDB;
import com.ecs160.hw.model.Repo;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import com.ecs160.hw.model.Issue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Main application for Part D steps 1-4:
 * 1. Pick a C/C++ repo from Redis
 * 2. Create selected_repo.dat file
 * 3. Load repo and issues from Redis
 * 4. Clone the repository
 */
public class App {
    public static void main(String[] args) {
        try {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, 
                    (JsonSerializer<ZonedDateTime>) (src, typeOfSrc, context) -> 
                        context.serialize(src.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)))
                .registerTypeAdapter(ZonedDateTime.class,
                    (JsonDeserializer<ZonedDateTime>) (json, typeOfT, context) ->
                        ZonedDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .create();

            // part 1 & 2: Read selected_repo.dat
            // First line is repoId, rest are file paths
            Path datFile = Paths.get("selected_repo.dat");
            if (!Files.exists(datFile)) {
                System.err.println("Error: selected_repo.dat not found!");
                System.err.println("Please create selected_repo.dat with:");
                System.err.println("  Line 1: repoId (e.g., repos:C:1)");
                System.err.println("  Lines 2+: C file paths (e.g., src/main.c)");
                return;
            }
            
            List<String> lines = Files.readAllLines(datFile);
            if (lines.isEmpty()) {
                System.err.println("Error: selected_repo.dat is empty!");
                return;
            }
            
            String repoId = lines.get(0).trim();
            List<String> cFiles = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    cFiles.add(line);
                }
            }
            
            if (cFiles.isEmpty()) {
                System.err.println("Warning: No C files specified in selected_repo.dat");
            }
            
            System.out.println("Selected repo ID: " + repoId);
            System.out.println("Files to analyze: " + cFiles.size());
            
            // part 3: loading repo from Redis using persistence framework
            System.out.println("\nLoading repo from Redis...");
            RedisDB db = RedisDB.getInstance();
            
            Repo repo = new Repo();
            repo.setId(repoId);
            
            Repo loadedRepo = (Repo) db.load(repo);
            
            if (loadedRepo == null) {
                System.err.println("Error: Could not load repo with ID: " + repoId);
                System.err.println("Make sure the repo exists in Redis!");
                return;
            }
            
            System.out.println("Successfully loaded repo:");
            System.out.println("  Name: " + loadedRepo.getName());
            System.out.println("  Owner: " + loadedRepo.getOwnerLogin());
            System.out.println("  URL: " + loadedRepo.getHtmlUrl());
            System.out.println("  Language: " + loadedRepo.getLanguage());
            System.out.println("  Issues: " + (loadedRepo.getIssues() != null ? loadedRepo.getIssues().size() : 0));
            
            // part 4: cloning the repository
            String cloneUrl = loadedRepo.getHtmlUrl();
            if (!cloneUrl.endsWith(".git")) {
                cloneUrl = cloneUrl + ".git";
            }
            
            String cloneDir = "./cloned_repo";
            
            // removing existing clone directory if it exists
            Path clonePath = Paths.get(cloneDir);
            if (Files.exists(clonePath)) {
                deleteDirectory(clonePath.toFile());
            }
            
            // cloning with depth 1 (shallow clone)
            ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1", cloneUrl, cloneDir);
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // reading output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.err.println("Error: Failed to clone repository (exit code: " + exitCode + ")");
                return;
            }
            
            // Verify .c files exist
            for (String filePath : cFiles) {
                Path fullPath = Paths.get(cloneDir, filePath);
                if (!Files.exists(fullPath)) {
                    System.err.println("Error: C file not found: " + filePath);
                }
            }

            // 5. summarize issues
            List<Issue> issues = loadedRepo.getIssues();
            List <Map<String, Object>> IssueList1 = new ArrayList<>();

            String microserviceAUrl = "http://localhost:8080/summarize_issue";

            for (int i = 0; i < issues.size(); i++) {
                Issue issue = issues.get(i);

                String issueJson = gson.toJson(issue);

                try {
                    // call microservice A
                    String summarizedIssue = callMicroservice(microserviceAUrl, issueJson);

                    // parse response
                    Map<String, Object> summarizedIssueMap = gson.fromJson(summarizedIssue, Map.class);
                    IssueList1.add(summarizedIssueMap);
                } catch (Exception e) {
                    System.err.println("Error calling microservice A: " + e.getMessage());
                }
            }

            // 6. find bugs
            List<Map<String, Object>> IssueList2 = new ArrayList<>();

            String microserviceBUrl = "http://localhost:8080/find_bugs";
            for (int i = 0; i < cFiles.size(); i++) {
                String filePath = cFiles.get(i);
                Path fullPath = Paths.get(cloneDir, filePath);

                try {
                    String cCode = Files.readString(fullPath);

                    // call microservice B
                    String bugsJson = callMicroservice(microserviceBUrl, cCode);

                    // parse response 
                    bugsJson = bugsJson.trim();
                    if (bugsJson.equals("[]")) {
                        continue;
                    } else {
                        // remove brackets []
                        String content = bugsJson.substring(1, bugsJson.length() - 1);

                        // parse each bug object
                        int pos = 0;
                        while (pos < content.length()) {
                            int start = content.indexOf('{', pos);
                            if (start == -1) break;

                            int end = findMatchingBrace(content, start);
                            if (end == -1) break;

                            String bugJson = content.substring(start, end + 1);
                            Map<String, Object> bugMap = gson.fromJson(bugJson, Map.class);
                            
                            bugMap.put("filename", filePath);

                            IssueList2.add(bugMap);
                            pos = end + 1;

                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error calling microservice B: " + e.getMessage());
                }
            }

            // 7. compare IssueList1 and IssueList2 and print set of common issues
            String microserviceCUrl = "http://localhost:8080/check_equivalence";

            try {
                List<List<Map<String, Object>>> reqList = new ArrayList<>();
                reqList.add(IssueList1);
                reqList.add(IssueList2);
                String reqJson = gson.toJson(reqList);

                String commonIssuesJson = callMicroservice(microserviceCUrl, reqJson);

                // parse response as list of issues
                List<Map<String, Object>> commonIssues = gson.fromJson(commonIssuesJson, List.class);
                System.out.println("Common Issues: ");
                for(Map<String, Object> issue: commonIssues) {
                    System.out.println(issue);
                }
            } catch (Exception e) {
                System.err.println("Error calling microservice C: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper method to delete directory recursively
    static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    // helper function to call microservices
    static String callMicroservice(String url, String body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(body));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    // helper function to find matching closing brace, return pos of brace or -1 if not found
    static int findMatchingBrace(String content, int start) {
        int end = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') end++;
            if (c == '}') {
                end--;
                if (end == 0) return i;
            }
        }
        return -1;
    }
}
