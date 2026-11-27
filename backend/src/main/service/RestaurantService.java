
package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.FoodItem;
import model.Order;
import model.Customer;
import model.RestaurantStaff;
import payment.Payment;

public class RestaurantService {

    private ArrayList<FoodItem> foodItems;
    private ArrayList<Order> orders;

    public RestaurantService() {
        foodItems = new ArrayList<>();
        orders = new ArrayList<>();
    }

    // Add food item
    public void addFoodItem(FoodItem foodItem) {
        if (foodItem == null) {
            throw new IllegalArgumentException("Food item is required.");
        }

        foodItems.add(foodItem);
        System.out.println(foodItem.getFoodName() + " added successfully!");
    }

    // Display available food items
    public void displayFoodItems() {
        System.out.println("\n===== FOOD MENU =====");

        for (FoodItem food : foodItems) {
            if (food.isAvailable()) {
                food.displayFoodInfo();
                System.out.println("--------------------");
            }
        }
    }

    // Create order using default dining option
    public Order createOrder(int orderId, Customer customer) {
        return createOrder(orderId, customer, "Dine In");
    }

    // Create order with selected dining option
    public Order createOrder(
            int orderId,
            Customer customer,
            String diningOption
    ) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required.");
        }

        Order order = new Order(orderId, customer, diningOption);
        orders.add(order);

        System.out.println("Order created successfully!");
        System.out.println("Dining Option: " + order.getDiningOption());

        return order;
    }

    // Process order
    // Retained for the RestaurantStaff OOP implementation
    public void processOrder(RestaurantStaff staff, Order order) {
        if (order == null || staff == null) {
            throw new IllegalArgumentException(
                    "Staff and order are required."
            );
        }

        staff.processOrders(order);
        order.updateStatus("Processing");
    }

    public Order updateOrderStatus(int orderId, String orderStatus) {
        Order order = findOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }

        order.updateStatus(orderStatus);
        return order;
    }

    public Order findOrderByTrackingToken(String trackingToken) {
        if (trackingToken == null
                || !trackingToken.matches("[A-Za-z0-9_-]{43}")) {
            return null;
        }

        byte[] requestedToken =
                trackingToken.getBytes(StandardCharsets.US_ASCII);
        Order matchingOrder = null;

        for (Order order : orders) {
            byte[] orderToken = order.getTrackingToken()
                    .getBytes(StandardCharsets.US_ASCII);

            if (MessageDigest.isEqual(orderToken, requestedToken)) {
                matchingOrder = order;
            }
        }

        return matchingOrder;
    }

    // Process payment
    // Call this only after payment confirmation
    public void processPayment(Order order, Payment payment) {
        if (order == null || payment == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "A non-empty order and payment are required."
            );
        }

        if (order.getPayment() != null) {
            throw new IllegalStateException(
                    "This order has already been paid."
            );
        }

        if (payment.getAmount() < order.calculateTotal()) {
            throw new IllegalArgumentException(
                    "Insufficient payment amount."
            );
        }

        order.setPayment(payment);
        payment.processPayment();
    }

    // Display all orders
    public void displayOrders() {
        System.out.println("\n===== ALL ORDERS =====");

        for (Order order : orders) {
            order.displayOrder();
        }
    }

    public List<FoodItem> getFoodItems() {
        return Collections.unmodifiableList(foodItems);
    }

    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public FoodItem getFoodItem(int foodId) {
        for (FoodItem foodItem : foodItems) {
            if (foodItem.getFoodId() == foodId) {
                return foodItem;
            }
        }

        return null;
    }

    private Order findOrder(int orderId) {
        for (Order order : orders) {
            if (order.getOrderId() == orderId) {
                return order;
            }
        }

        return null;
    }
}
