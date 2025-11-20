package com.ecs160.hw.model;

import com.ecs160.persistence.annotations.PersistableObject;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.Id;
import java.time.ZonedDateTime;

@PersistableObject
public class Issue {
    @Id
    private String id;

    @PersistableField
    private Integer number;

    @PersistableField
    private String title;

    @PersistableField
    private String body;

    @PersistableField
    private String state;

    @PersistableField
    private ZonedDateTime createdAt;

    @PersistableField
    private ZonedDateTime updatedAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
