package com.kidtask.gui;

import com.kidtask.data.DataManager;
import com.kidtask.models.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Panel for managing tasks.
 */
public class TaskPanel extends JPanel {
    private DataManager dataManager;
    private User currentUser;
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JTextField dueDateField;
    private JSpinner pointsSpinner;
    private JComboBox<String> childComboBox;
    
    public TaskPanel(DataManager dataManager, User currentUser) {
        this.dataManager = dataManager;
        this.currentUser = currentUser;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refresh();
    }
    
    private void initializeComponents() {
        // Table model
        String[] columnNames = {"ID", "Title", "Description", "Due Date", "Points", 
                                "Assigned To", "Status", "Rating"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        taskTable = new JTable(tableModel);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.getTableHeader().setReorderingAllowed(false);
        
        // Form fields
        titleField = new JTextField(20);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        dueDateField = new JTextField(20);
        dueDateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        pointsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        
        // Child combo box (for parent/teacher)
        childComboBox = new JComboBox<>();
        updateChildComboBox();
    }
    
    private void updateChildComboBox() {
        childComboBox.removeAllItems();
        if (currentUser.getRole() == UserRole.PARENT || 
            currentUser.getRole() == UserRole.TEACHER) {
            List<Child> children = dataManager.getAllChildren();
            for (Child child : children) {
                childComboBox.addItem(child.getUsername());
            }
        } else {
            childComboBox.addItem(currentUser.getUsername());
            childComboBox.setEnabled(false);
        }
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Table panel
        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        add(scrollPane, BorderLayout.CENTER);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        formPanel.add(titleField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(descriptionArea), gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Due Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        formPanel.add(dueDateField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Points:"), gbc);
        gbc.gridx = 1;
        formPanel.add(pointsSpinner, gbc);
        
        if (currentUser.getRole() == UserRole.PARENT || 
            currentUser.getRole() == UserRole.TEACHER) {
            gbc.gridx = 0;
            gbc.gridy = 4;
            formPanel.add(new JLabel("Assign To:"), gbc);
            gbc.gridx = 1;
            formPanel.add(childComboBox, gbc);
        }
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        if (currentUser.getRole() == UserRole.PARENT || 
            currentUser.getRole() == UserRole.TEACHER) {
            JButton addButton = new JButton("Add Task");
            addButton.addActionListener(e -> addTask());
            buttonPanel.add(addButton);
        }
        
        if (currentUser.getRole() == UserRole.CHILD) {
            JButton completeButton = new JButton("Mark Completed");
            completeButton.addActionListener(e -> markTaskCompleted());
            buttonPanel.add(completeButton);
        }
        
        if (currentUser.getRole() == UserRole.PARENT || 
            currentUser.getRole() == UserRole.TEACHER) {
            JButton approveButton = new JButton("Approve & Rate");
            approveButton.addActionListener(e -> approveTask());
            buttonPanel.add(approveButton);
            
            JButton rejectButton = new JButton("Reject");
            rejectButton.addActionListener(e -> rejectTask());
            buttonPanel.add(rejectButton);
        }
        
        JButton deleteButton = new JButton("Delete Task");
        deleteButton.addActionListener(e -> deleteTask());
        buttonPanel.add(deleteButton);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(refreshButton);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);
        
        add(formPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        // Event handlers are set up in setupLayout
    }
    
    public void refresh() {
        tableModel.setRowCount(0);
        List<Task> tasks;
        
        if (currentUser.getRole() == UserRole.CHILD) {
            tasks = dataManager.getTasksForChild(currentUser.getUsername());
        } else {
            tasks = dataManager.getAllTasks();
        }
        
        for (Task task : tasks) {
            Object[] row = {
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPoints(),
                task.getAssignedTo(),
                task.getStatus(),
                task.getRating() != null ? task.getRating() : "N/A"
            };
            tableModel.addRow(row);
        }
        
        updateChildComboBox();
    }
    
    private void addTask() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String dueDate = dueDateField.getText().trim();
        int points = (Integer) pointsSpinner.getValue();
        String assignedTo = (String) childComboBox.getSelectedItem();
        
        if (title.isEmpty() || description.isEmpty() || dueDate.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String taskId = UUID.randomUUID().toString();
        Task task = new Task(taskId, title, description, dueDate, points,
                           assignedTo, currentUser.getUsername());
        
        dataManager.addTask(task);
        dataManager.saveData();
        
        // Clear form
        titleField.setText("");
        descriptionArea.setText("");
        dueDateField.setText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        pointsSpinner.setValue(10);
        
        refresh();
        JOptionPane.showMessageDialog(this,
                "Task added successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void markTaskCompleted() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a task to complete.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String taskId = (String) tableModel.getValueAt(selectedRow, 0);
        Task task = dataManager.getTaskById(taskId);
        
        if (task != null && task.getStatus() == TaskStatus.PENDING) {
            task.markCompleted();
            dataManager.saveData();
            refresh();
            JOptionPane.showMessageDialog(this,
                    "Task marked as completed!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void approveTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a task to approve.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String taskId = (String) tableModel.getValueAt(selectedRow, 0);
        Task task = dataManager.getTaskById(taskId);
        
        if (task != null && task.getStatus() == TaskStatus.COMPLETED) {
            String ratingStr = JOptionPane.showInputDialog(this,
                    "Enter rating (0-5):", "5");
            
            if (ratingStr != null) {
                try {
                    double rating = Double.parseDouble(ratingStr);
                    if (rating < 0 || rating > 5) {
                        JOptionPane.showMessageDialog(this,
                                "Rating must be between 0 and 5.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    task.approve(rating);
                    
                    // Update child's points and rating
                    User childUser = dataManager.getUser(task.getAssignedTo());
                    if (childUser instanceof Child) {
                        Child child = (Child) childUser;
                        child.addPoints(task.getPoints());
                        child.addRating(rating);
                    }
                    
                    dataManager.saveData();
                    refresh();
                    JOptionPane.showMessageDialog(this,
                            "Task approved and rated!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid rating format.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Only completed tasks can be approved.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void rejectTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a task to reject.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String taskId = (String) tableModel.getValueAt(selectedRow, 0);
        Task task = dataManager.getTaskById(taskId);
        
        if (task != null && task.getStatus() == TaskStatus.COMPLETED) {
            task.reject();
            dataManager.saveData();
            refresh();
            JOptionPane.showMessageDialog(this,
                    "Task rejected.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void deleteTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a task to delete.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this task?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String taskId = (String) tableModel.getValueAt(selectedRow, 0);
            dataManager.removeTask(taskId);
            dataManager.saveData();
            refresh();
        }
    }
}

