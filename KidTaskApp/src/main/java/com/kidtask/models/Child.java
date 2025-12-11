package com.kidtask.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Child user class with points and level tracking.
 */
public class Child extends User {
    private int points;
    private int level;
    private List<Double> ratings;
    
    public Child(String username, String password) {
        super(username, password, UserRole.CHILD);
        this.points = 0;
        this.level = 1;
        this.ratings = new ArrayList<>();
    }
    
    public Child(String username, String password, int points, int level) {
        super(username, password, UserRole.CHILD);
        this.points = points;
        this.level = level;
        this.ratings = new ArrayList<>();
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }
    
    public void addPoints(int points) {
        this.points += points;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public List<Double> getRatings() {
        return new ArrayList<>(ratings);
    }
    
    public void addRating(double rating) {
        if (rating >= 0 && rating <= 5) {
            ratings.add(rating);
            updateLevel();
        }
    }
    
    private void updateLevel() {
        if (ratings.isEmpty()) {
            return;
        }
        
        double sum = 0.0;
        for (Double rating : ratings) {
            sum += rating;
        }
        double avgRating = sum / ratings.size();
        // Level based on average rating (1-5 rating -> 1-5 level)
        this.level = Math.max(1, Math.min(5, (int) avgRating + 1));
    }
    
    @Override
    public String toString() {
        return "Child{" +
                "username='" + username + '\'' +
                ", points=" + points +
                ", level=" + level +
                '}';
    }
}

