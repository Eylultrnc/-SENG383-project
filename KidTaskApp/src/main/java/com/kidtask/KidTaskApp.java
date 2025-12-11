package com.kidtask;

import com.kidtask.data.DataManager;
import com.kidtask.gui.LoginFrame;

import javax.swing.*;

/**
 * Main application class for KidTask.
 */
public class KidTaskApp {
    
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Initialize data manager and load data
        DataManager dataManager = new DataManager();
        dataManager.loadData();
        
        // Create and show login frame
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(dataManager);
            loginFrame.setVisible(true);
        });
    }
}
