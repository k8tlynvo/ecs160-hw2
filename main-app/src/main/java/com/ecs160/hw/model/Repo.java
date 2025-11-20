package com.ecs160.hw.model;

import com.ecs160.persistence.annotations.PersistableObject;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.Id;
import java.util.List;

@PersistableObject
public class Repo {
    @Id
    private String id;
    @PersistableField
    private String name;

    @PersistableField
    private String ownerLogin;

    @PersistableField
    private String htmlUrl;

    @PersistableField
    private String language;

    @PersistableField
    private int stargazersCount;

    @PersistableField
    private int forksCount;

    @PersistableField
    private int openIssuesCount;

    @PersistableField
    private int commitsAfterForkCount;

    @PersistableField
    private List<Issue> issues;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public void setOwnerLogin(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getStargazersCount() {
        return stargazersCount;
    }

    public void setStargazersCount(int stargazersCount) {
        this.stargazersCount = stargazersCount;
    }

    public int getForksCount() {
        return forksCount;
    }

    public void setForksCount(int forksCount) {
        this.forksCount = forksCount;
    }

    public int getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(int openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public int getCommitsAfterForkCount() {
        return commitsAfterForkCount;
    }

    public void setCommitsAfterForkCount(int commitsAfterForkCount) {
        this.commitsAfterForkCount = commitsAfterForkCount;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }
}
