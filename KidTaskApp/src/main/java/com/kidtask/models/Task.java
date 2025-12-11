package com.kidtask.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Task class representing a task that can be assigned to a child.
 */
public class Task {
    private String taskId;
    private String title;
    private String description;
    private String dueDate;
    private int points;
    private String assignedTo;  // Child username
    private String createdBy;   // Parent/Teacher username
    private TaskStatus status;
    private Double rating;  // Rating given by parent/teacher
    private String completedDate;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public Task(String taskId, String title, String description, String dueDate, 
                int points, String assignedTo, String createdBy) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.points = points;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.status = TaskStatus.PENDING;
        this.rating = null;
        this.completedDate = null;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public String getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    public String getCompletedDate() {
        return completedDate;
    }
    
    public void setCompletedDate(String completedDate) {
        this.completedDate = completedDate;
    }
    
    public void markCompleted() {
        if (this.status == TaskStatus.PENDING) {
            this.status = TaskStatus.COMPLETED;
            this.completedDate = LocalDateTime.now().format(FORMATTER);
        }
    }
    
    public void approve(Double rating) {
        if (this.status == TaskStatus.COMPLETED) {
            this.status = TaskStatus.APPROVED;
            if (rating != null) {
                this.rating = rating;
            }
        }
    }
    
    public void reject() {
        if (this.status == TaskStatus.COMPLETED) {
            this.status = TaskStatus.REJECTED;
        }
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", points=" + points +
                '}';
    }
}

