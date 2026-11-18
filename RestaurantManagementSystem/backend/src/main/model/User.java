package model;

public abstract class User {

    private int userId;
    private String name;
    private String contactInfo;

    public User(int userId, String name, String contactInfo) {
        this.userId = userId;
        setName(name);
        setContactInfo(contactInfo);
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        this.name = name.trim();
    }

    public void setContactInfo(String contactInfo) {
        if (contactInfo == null || contactInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact information is required.");
        }
        this.contactInfo = contactInfo.trim();
    }

    public abstract void displayInfo();
}