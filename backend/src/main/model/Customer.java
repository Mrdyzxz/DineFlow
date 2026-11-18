package model;

public class Customer extends User {

    public Customer(int customerId, String name, String contactInfo) {
        super(customerId, name, contactInfo);
    }

    @Override
    public void displayInfo() {
        System.out.println("\n===== CUSTOMER INFORMATION =====");
        System.out.println("Customer ID: " + getUserId());
        System.out.println("Name: " + getName());
        System.out.println("Contact Info: " + getContactInfo());
    }

    public void placeOrder(Order order) {
        if (order != null) {
            System.out.println(getName()
                    + " placed Order #" + order.getOrderId());
        }
    }
}