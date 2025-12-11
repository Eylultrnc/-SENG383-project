package com.kidtask.data;

import com.kidtask.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data manager for handling file persistence of users, tasks, and wishes.
 * Uses JSON format for data storage.
 */
public class DataManager {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/Users.txt";
    private static final String TASKS_FILE = DATA_DIR + "/Tasks.txt";
    private static final String WISHES_FILE = DATA_DIR + "/Wishes.txt";

    private Gson gson;
    private Map<String, User> users;
    private List<Task> tasks;
    private List<Wish> wishes;

    public DataManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.users = new HashMap<>();
        this.tasks = new ArrayList<>();
        this.wishes = new ArrayList<>();

        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    /**
     * Load all data from files.
     */
    public void loadData() {
        loadUsers();
        loadTasks();
        loadWishes();
    }

    /**
     * Save all data to files.
     */
    public void saveData() {
        saveUsers();
        saveTasks();
        saveWishes();
    }

    // User management
    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.authenticate(password)) {
            return user;
        }
        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<Child> getAllChildren() {
        List<Child> children = new ArrayList<>();
        for (User user : users.values()) {
            if (user instanceof Child) {
                children.add((Child) user);
            }
        }
        return children;
    }

    // Task management
    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void removeTask(String taskId) {
        tasks.removeIf(task -> task.getTaskId().equals(taskId));
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getTasksForChild(String childUsername) {
        List<Task> childTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getAssignedTo().equals(childUsername)) {
                childTasks.add(task);
            }
        }
        return childTasks;
    }

    public Task getTaskById(String taskId) {
        for (Task task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    // Wish management
    public void addWish(Wish wish) {
        wishes.add(wish);
    }

    public void removeWish(Wish wish) {
        wishes.remove(wish);
    }

    public void removeWish(String wishId) {
        wishes.removeIf(wish -> wish.getWishId().equals(wishId));
    }

    public List<Wish> getAllWishes() {
        return new ArrayList<>(wishes);
    }

    public List<Wish> getWishesForChild(String childUsername, int childLevel) {
        List<Wish> availableWishes = new ArrayList<>();
        for (Wish wish : wishes) {
            if (wish.getRequestedBy().equals(childUsername) &&
                wish.isAvailableForLevel(childLevel)) {
                availableWishes.add(wish);
            }
        }
        return availableWishes;
    }

    public Wish getWishById(String wishId) {
        for (Wish wish : wishes) {
            if (wish.getWishId().equals(wishId)) {
                return wish;
            }
        }
        return null;
    }

    // File I/O methods
    private void loadUsers() {
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            if (json.length() > 0) {
                Type userListType = new TypeToken<List<UserData>>(){}.getType();
                List<UserData> userDataList = gson.fromJson(json.toString(), userListType);

                for (UserData userData : userDataList) {
                    User user = createUserFromData(userData);
                    if (user != null) {
                        users.put(user.getUsername(), user);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try {
            List<UserData> userDataList = new ArrayList<>();
            for (User user : users.values()) {
                UserData userData = createUserDataFromUser(user);
                userDataList.add(userData);
            }

            String json = gson.toJson(userDataList);
            FileWriter writer = new FileWriter(USERS_FILE);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    private void loadTasks() {
        try {
            File file = new File(TASKS_FILE);
            if (!file.exists()) {
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            if (json.length() > 0) {
                Type taskListType = new TypeToken<List<Task>>(){}.getType();
                tasks = gson.fromJson(json.toString(), taskListType);
                if (tasks == null) {
                    tasks = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
    }

    private void saveTasks() {
        try {
            String json = gson.toJson(tasks);
            FileWriter writer = new FileWriter(TASKS_FILE);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving tasks: " + e.getMessage());
        }
    }

    private void loadWishes() {
        try {
            File file = new File(WISHES_FILE);
            if (!file.exists()) {
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            if (json.length() > 0) {
                Type wishListType = new TypeToken<List<Wish>>(){}.getType();
                wishes = gson.fromJson(json.toString(), wishListType);
                if (wishes == null) {
                    wishes = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading wishes: " + e.getMessage());
        }
    }

    private void saveWishes() {
        try {
            String json = gson.toJson(wishes);
            FileWriter writer = new FileWriter(WISHES_FILE);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving wishes: " + e.getMessage());
        }
    }

    // Helper classes for JSON serialization
    private static class UserData {
        private String username;
        private String password;
        private String role;
        private Integer points;
        private Integer level;
        private List<Double> ratings;

        public UserData() {
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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Integer getPoints() {
            return points;
        }

        public void setPoints(Integer points) {
            this.points = points;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public List<Double> getRatings() {
            return ratings;
        }

        public void setRatings(List<Double> ratings) {
            this.ratings = ratings;
        }
    }

    private User createUserFromData(UserData userData) {
        switch (userData.getRole().toUpperCase()) {
            case "CHILD":
                Child child = new Child(userData.getUsername(), userData.getPassword());
                if (userData.getPoints() != null) {
                    child.setPoints(userData.getPoints());
                }
                if (userData.getLevel() != null) {
                    child.setLevel(userData.getLevel());
                }
                if (userData.getRatings() != null) {
                    for (Double rating : userData.getRatings()) {
                        child.addRating(rating);
                    }
                }
                return child;
            case "PARENT":
                return new Parent(userData.getUsername(), userData.getPassword());
            case "TEACHER":
                return new Teacher(userData.getUsername(), userData.getPassword());
            default:
                return null;
        }
    }

    private UserData createUserDataFromUser(User user) {
        UserData userData = new UserData();
        userData.setUsername(user.getUsername());
        userData.setPassword(user.getPassword());
        userData.setRole(user.getRole().name());

        if (user instanceof Child) {
            Child child = (Child) user;
            userData.setPoints(child.getPoints());
            userData.setLevel(child.getLevel());
            userData.setRatings(child.getRatings());
        }

        return userData;
    }
}
