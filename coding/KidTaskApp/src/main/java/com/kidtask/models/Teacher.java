package com.kidtask.models;

/**
 * Teacher user class.
 */
public class Teacher extends User {
    
    public Teacher(String username, String password) {
        super(username, password, UserRole.TEACHER);
    }
    
    @Override
    public String toString() {
        return "Teacher{" +
                "username='" + username + '\'' +
                '}';
    }
}

