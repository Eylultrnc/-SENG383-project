package com.kidtask.gui;

import com.kidtask.data.DataManager;
import com.kidtask.models.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel for displaying progress, points, and level information.
 */
public class ProgressPanel extends JPanel {
    private DataManager dataManager;
    private User currentUser;
    private JLabel pointsLabel;
    private JLabel levelLabel;
    private JProgressBar levelProgressBar;
    private JTextArea ratingsArea;
    private JTextArea tasksSummaryArea;
    
    public ProgressPanel(DataManager dataManager, User currentUser) {
        this.dataManager = dataManager;
        this.currentUser = currentUser;
        initializeComponents();
        setupLayout();
        refresh();
    }
    
    private void initializeComponents() {
        pointsLabel = new JLabel("Points: 0");
        pointsLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        levelLabel = new JLabel("Level: 1");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        levelProgressBar = new JProgressBar(1, 5);
        levelProgressBar.setStringPainted(true);
        levelProgressBar.setPreferredSize(new Dimension(300, 30));
        
        ratingsArea = new JTextArea(10, 30);
        ratingsArea.setEditable(false);
        ratingsArea.setLineWrap(true);
        
        tasksSummaryArea = new JTextArea(10, 30);
        tasksSummaryArea.setEditable(false);
        tasksSummaryArea.setLineWrap(true);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel with points and level
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(pointsLabel, gbc);
        
        gbc.gridx = 1;
        topPanel.add(levelLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        topPanel.add(levelProgressBar, gbc);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center panel with details
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Ratings panel
        JPanel ratingsPanel = new JPanel(new BorderLayout());
        ratingsPanel.setBorder(BorderFactory.createTitledBorder("Ratings History"));
        ratingsPanel.add(new JScrollPane(ratingsArea), BorderLayout.CENTER);
        
        // Tasks summary panel
        JPanel tasksPanel = new JPanel(new BorderLayout());
        tasksPanel.setBorder(BorderFactory.createTitledBorder("Tasks Summary"));
        tasksPanel.add(new JScrollPane(tasksSummaryArea), BorderLayout.CENTER);
        
        centerPanel.add(ratingsPanel);
        centerPanel.add(tasksPanel);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel with refresh button
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        bottomPanel.add(refreshButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void refresh() {
        if (currentUser.getRole() == UserRole.CHILD) {
            Child child = (Child) currentUser;
            
            // Update points and level
            pointsLabel.setText("Points: " + child.getPoints());
            levelLabel.setText("Level: " + child.getLevel());
            levelProgressBar.setValue(child.getLevel());
            levelProgressBar.setString("Level " + child.getLevel() + " / 5");
            
            // Update ratings
            List<Double> ratings = child.getRatings();
            if (ratings.isEmpty()) {
                ratingsArea.setText("No ratings yet.");
            } else {
                StringBuilder ratingsText = new StringBuilder();
                double sum = 0.0;
                for (int i = 0; i < ratings.size(); i++) {
                    double rating = ratings.get(i);
                    sum += rating;
                    ratingsText.append("Rating ").append(i + 1).append(": ")
                              .append(String.format("%.1f", rating)).append("/5.0\n");
                }
                double avg = sum / ratings.size();
                ratingsText.append("\nAverage Rating: ").append(String.format("%.2f", avg)).append("/5.0");
                ratingsArea.setText(ratingsText.toString());
            }
            
            // Update tasks summary
            List<Task> tasks = dataManager.getTasksForChild(child.getUsername());
            int pending = 0, completed = 0, approved = 0, rejected = 0;
            int totalPoints = 0;
            
            for (Task task : tasks) {
                switch (task.getStatus()) {
                    case PENDING:
                        pending++;
                        break;
                    case COMPLETED:
                        completed++;
                        break;
                    case APPROVED:
                        approved++;
                        totalPoints += task.getPoints();
                        break;
                    case REJECTED:
                        rejected++;
                        break;
                }
            }
            
            StringBuilder tasksText = new StringBuilder();
            tasksText.append("Total Tasks: ").append(tasks.size()).append("\n");
            tasksText.append("Pending: ").append(pending).append("\n");
            tasksText.append("Completed: ").append(completed).append("\n");
            tasksText.append("Approved: ").append(approved).append("\n");
            tasksText.append("Rejected: ").append(rejected).append("\n");
            tasksText.append("\nTotal Points Earned: ").append(totalPoints);
            tasksSummaryArea.setText(tasksText.toString());
            
        } else {
            // For parent/teacher, show all children's progress
            pointsLabel.setText("Viewing Progress");
            levelLabel.setText("(Child View Only)");
            levelProgressBar.setValue(0);
            levelProgressBar.setString("N/A");
            
            List<Child> children = dataManager.getAllChildren();
            StringBuilder childrenText = new StringBuilder();
            childrenText.append("Children Progress:\n\n");
            
            for (Child child : children) {
                childrenText.append("Child: ").append(child.getUsername()).append("\n");
                childrenText.append("  Points: ").append(child.getPoints()).append("\n");
                childrenText.append("  Level: ").append(child.getLevel()).append("\n");
                
                List<Double> ratings = child.getRatings();
                if (!ratings.isEmpty()) {
                    double sum = 0.0;
                    for (Double rating : ratings) {
                        sum += rating;
                    }
                    double avg = sum / ratings.size();
                    childrenText.append("  Avg Rating: ").append(String.format("%.2f", avg)).append("/5.0\n");
                }
                childrenText.append("\n");
            }
            
            ratingsArea.setText(childrenText.toString());
            tasksSummaryArea.setText("Select a child to view detailed task summary.");
        }
    }
}

