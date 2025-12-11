package com.kidtask.gui;

import com.kidtask.data.DataManager;
import com.kidtask.models.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Main dashboard frame that contains all panels.
 */
public class DashboardFrame extends JFrame {
    private DataManager dataManager;
    private User currentUser;
    private JTabbedPane tabbedPane;
    private TaskPanel taskPanel;
    private WishPanel wishPanel;
    private ProgressPanel progressPanel;
    
    public DashboardFrame(DataManager dataManager, User user) {
        this.dataManager = dataManager;
        this.currentUser = user;
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        setTitle("KidTask - Dashboard (" + currentUser.getUsername() + " - " + 
                 currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        tabbedPane = new JTabbedPane();
        
        // Create panels based on user role
        taskPanel = new TaskPanel(dataManager, currentUser);
        wishPanel = new WishPanel(dataManager, currentUser);
        progressPanel = new ProgressPanel(dataManager, currentUser);
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Tasks", taskPanel);
        tabbedPane.addTab("Wishes", wishPanel);
        tabbedPane.addTab("Progress", progressPanel);
        
        // Add logout button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataManager.saveData();
                SwingUtilities.invokeLater(() -> {
                    new LoginFrame(dataManager).setVisible(true);
                    dispose();
                });
            }
        });
        topPanel.add(logoutButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add window listener to save on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                dataManager.saveData();
            }
        });
    }
    
    private void setupLayout() {
        // Layout is already set in initializeComponents
    }
    
    public void refreshAllPanels() {
        taskPanel.refresh();
        wishPanel.refresh();
        progressPanel.refresh();
    }
}

