package com.kidtask.models;

/**
 * Parent user class.
 */
public class Parent extends User {
    
    public Parent(String username, String password) {
        super(username, password, UserRole.PARENT);
    }
    
    @Override
    public String toString() {
        return "Parent{" +
                "username='" + username + '\'' +
                '}';
    }
}

