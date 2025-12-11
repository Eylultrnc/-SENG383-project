package com.kidtask.gui;

import com.kidtask.data.DataManager;
import com.kidtask.models.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.UUID;

/**
 * Panel for managing wishes.
 */
public class WishPanel extends JPanel {
    private DataManager dataManager;
    private User currentUser;
    private JTable wishTable;
    private DefaultTableModel tableModel;
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<String> wishTypeComboBox;
    private JSpinner levelSpinner;
    
    public WishPanel(DataManager dataManager, User currentUser) {
        this.dataManager = dataManager;
        this.currentUser = currentUser;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refresh();
    }
    
    private void initializeComponents() {
        // Table model
        String[] columnNames = {"ID", "Title", "Description", "Type", 
                                "Required Level", "Status", "Approved By"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        wishTable = new JTable(tableModel);
        wishTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wishTable.getTableHeader().setReorderingAllowed(false);
        
        // Form fields
        titleField = new JTextField(20);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        
        String[] wishTypes = {"product", "activity"};
        wishTypeComboBox = new JComboBox<>(wishTypes);
        
        levelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Table panel
        JScrollPane scrollPane = new JScrollPane(wishTable);
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
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(wishTypeComboBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Required Level:"), gbc);
        gbc.gridx = 1;
        formPanel.add(levelSpinner, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        if (currentUser.getRole() == UserRole.CHILD) {
            JButton addButton = new JButton("Add Wish");
            addButton.addActionListener(e -> addWish());
            buttonPanel.add(addButton);
        }
        
        if (currentUser.getRole() == UserRole.PARENT) {
            JButton approveButton = new JButton("Approve");
            approveButton.addActionListener(e -> approveWish());
            buttonPanel.add(approveButton);
            
            JButton rejectButton = new JButton("Reject");
            rejectButton.addActionListener(e -> rejectWish());
            buttonPanel.add(rejectButton);
        }
        
        JButton deleteButton = new JButton("Delete Wish");
        deleteButton.addActionListener(e -> deleteWish());
        buttonPanel.add(deleteButton);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(refreshButton);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
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
        List<Wish> wishes;
        
        if (currentUser.getRole() == UserRole.CHILD) {
            Child child = (Child) currentUser;
            wishes = dataManager.getWishesForChild(child.getUsername(), child.getLevel());
        } else if (currentUser.getRole() == UserRole.PARENT) {
            wishes = dataManager.getAllWishes();
        } else {
            wishes = dataManager.getAllWishes();
        }
        
        for (Wish wish : wishes) {
            Object[] row = {
                wish.getWishId(),
                wish.getTitle(),
                wish.getDescription(),
                wish.getWishType(),
                wish.getRequiredLevel(),
                wish.getStatus(),
                wish.getApprovedBy() != null ? wish.getApprovedBy() : "N/A"
            };
            tableModel.addRow(row);
        }
    }
    
    private void addWish() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String wishType = (String) wishTypeComboBox.getSelectedItem();
        int requiredLevel = (Integer) levelSpinner.getValue();
        
        if (title.isEmpty() || description.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String wishId = UUID.randomUUID().toString();
        Wish wish = new Wish(wishId, title, description, wishType,
                            requiredLevel, currentUser.getUsername());
        
        dataManager.addWish(wish);
        dataManager.saveData();
        
        // Clear form
        titleField.setText("");
        descriptionArea.setText("");
        wishTypeComboBox.setSelectedIndex(0);
        levelSpinner.setValue(1);
        
        refresh();
        JOptionPane.showMessageDialog(this,
                "Wish added successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void approveWish() {
        int selectedRow = wishTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a wish to approve.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String wishId = (String) tableModel.getValueAt(selectedRow, 0);
        Wish wish = dataManager.getWishById(wishId);
        
        if (wish != null && wish.getStatus() == WishStatus.PENDING) {
            wish.approve(currentUser.getUsername());
            dataManager.saveData();
            refresh();
            JOptionPane.showMessageDialog(this,
                    "Wish approved!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Only pending wishes can be approved.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void rejectWish() {
        int selectedRow = wishTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a wish to reject.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String wishId = (String) tableModel.getValueAt(selectedRow, 0);
        Wish wish = dataManager.getWishById(wishId);
        
        if (wish != null && wish.getStatus() == WishStatus.PENDING) {
            wish.reject();
            dataManager.saveData();
            refresh();
            JOptionPane.showMessageDialog(this,
                    "Wish rejected.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void deleteWish() {
        int selectedRow = wishTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a wish to delete.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this wish?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String wishId = (String) tableModel.getValueAt(selectedRow, 0);
            dataManager.removeWish(wishId);
            dataManager.saveData();
            refresh();
        }
    }
}

