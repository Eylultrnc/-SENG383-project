package com.kidtask.gui;

import com.kidtask.data.DataManager;
import com.kidtask.models.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Login frame for user authentication.
 */
public class LoginFrame extends JFrame {
    private DataManager dataManager;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JComboBox<String> roleComboBox;
    
    public LoginFrame(DataManager dataManager) {
        this.dataManager = dataManager;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setTitle("KidTask - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        
        String[] roles = {"CHILD", "PARENT", "TEACHER"};
        roleComboBox = new JComboBox<>(roles);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel titleLabel = new JLabel("KidTask Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(titleLabel, gbc);
        
        // Username
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);
        
        // Role (for registration)
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Role (Register):"), gbc);
        gbc.gridx = 1;
        mainPanel.add(roleComboBox, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Please enter both username and password.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                User user = dataManager.authenticateUser(username, password);
                if (user != null) {
                    // Open dashboard
                    SwingUtilities.invokeLater(() -> {
                        new DashboardFrame(dataManager, user).setVisible(true);
                        dispose();
                    });
                } else {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Invalid username or password.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String role = (String) roleComboBox.getSelectedItem();
                
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Please enter both username and password.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (dataManager.getUser(username) != null) {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Username already exists.",
                            "Registration Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                User newUser = null;
                switch (role) {
                    case "CHILD":
                        newUser = new com.kidtask.models.Child(username, password);
                        break;
                    case "PARENT":
                        newUser = new com.kidtask.models.Parent(username, password);
                        break;
                    case "TEACHER":
                        newUser = new com.kidtask.models.Teacher(username, password);
                        break;
                }
                
                if (newUser != null) {
                    dataManager.addUser(newUser);
                    dataManager.saveData();
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Registration successful! Please login.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    passwordField.setText("");
                }
            }
        });
    }
}

