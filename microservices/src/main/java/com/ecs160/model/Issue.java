package com.ecs160.model;

import java.util.Objects;

public class Issue {
    private String bug_type;
    private int line;
    private String description;
    private String filename;
    
    public Issue(String bugType, int line, String description, String filename) {
        this.bug_type = bugType;
        this.line = line;
        this.description = description;
        this.filename = filename;
    }

    public String getBugType() {
        return bug_type;
    }

    public int getLine() {
        return line;
    }
    
    public String getDescription() {
        return description;
    }

    public String getFilename() {
        return filename;
    }

    public void setBugType(String bugType) {
        this.bug_type = bugType;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    // to check if two Issue objects have equal fields
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Issue issue = (Issue) o;
        return line == issue.line &&
            Objects.equals(bug_type, issue.bug_type) &&
            Objects.equals(description, issue.description) &&
            Objects.equals(filename, issue.filename);
    }
}
