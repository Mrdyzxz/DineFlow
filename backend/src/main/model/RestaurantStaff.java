package model;

public class RestaurantStaff extends User {

    public RestaurantStaff(int staffId, String staffName, String contactInfo) {
        super(staffId, staffName, contactInfo);
    }

    @Override
    public void displayInfo() {
        System.out.println("\n===== RESTAURANT STAFF =====");
        System.out.println("Staff ID: " + getUserId());
        System.out.println("Staff Name: " + getName());
        System.out.println("Contact Info: " + getContactInfo());
    }

    public void manageFoodItems(FoodItem foodItem) {
        if (foodItem != null) {
            System.out.println(getName() + " is managing: "
                    + foodItem.getFoodName());
        }
    }

    public void processOrders(Order order) {
        if (order != null) {
            System.out.println(getName() + " is processing Order #"
                    + order.getOrderId());
        }
    }
}