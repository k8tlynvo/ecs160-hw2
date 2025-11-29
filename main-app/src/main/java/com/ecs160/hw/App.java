package com.ecs160.hw;

import com.ecs160.persistence.RedisDB;
import com.ecs160.hw.model.Repo;
import java.io.*;
import java.nio.file.*;
import java.util.*;

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
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper method to delete directory recursively
    private static void deleteDirectory(File directory) throws IOException {
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
}
