package com.kidtask.models;

/**
 * Base user class for all users in the KidTask application.
 */
public abstract class User {
    protected String username;
    protected String password;
    protected UserRole role;
    
    public User(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public boolean authenticate(String password) {
        return this.password.equals(password);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}

