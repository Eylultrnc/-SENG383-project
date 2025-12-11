package com.kidtask.models;

/**
 * Wish class representing a product or activity wish.
 */
public class Wish {
    private String wishId;
    private String title;
    private String description;
    private String wishType;  // "product" or "activity"
    private int requiredLevel;
    private String requestedBy;  // Child username
    private WishStatus status;
    private String approvedBy;  // Parent username
    
    public Wish(String wishId, String title, String description, String wishType,
                int requiredLevel, String requestedBy) {
        this.wishId = wishId;
        this.title = title;
        this.description = description;
        this.wishType = wishType;
        this.requiredLevel = requiredLevel;
        this.requestedBy = requestedBy;
        this.status = WishStatus.PENDING;
        this.approvedBy = null;
    }
    
    public String getWishId() {
        return wishId;
    }
    
    public void setWishId(String wishId) {
        this.wishId = wishId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getWishType() {
        return wishType;
    }
    
    public void setWishType(String wishType) {
        this.wishType = wishType;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }
    
    public String getRequestedBy() {
        return requestedBy;
    }
    
    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
    
    public WishStatus getStatus() {
        return status;
    }
    
    public void setStatus(WishStatus status) {
        this.status = status;
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public void approve(String approvedBy) {
        if (this.status == WishStatus.PENDING) {
            this.status = WishStatus.APPROVED;
            this.approvedBy = approvedBy;
        }
    }
    
    public void reject() {
        if (this.status == WishStatus.PENDING) {
            this.status = WishStatus.REJECTED;
        }
    }
    
    public boolean isAvailableForLevel(int childLevel) {
        return childLevel >= requiredLevel;
    }
    
    @Override
    public String toString() {
        return "Wish{" +
                "wishId='" + wishId + '\'' +
                ", title='" + title + '\'' +
                ", wishType='" + wishType + '\'' +
                ", requiredLevel=" + requiredLevel +
                ", status=" + status +
                '}';
    }
}

