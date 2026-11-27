
package model;

import java.time.LocalDate;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import payment.Payment;

public class Order {

    private static final SecureRandom TRACKING_TOKEN_RANDOM =
            new SecureRandom();

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_PROCESSING = "Processing";
    private static final String STATUS_READY = "Ready";
    private static final String STATUS_COMPLETED = "Completed";
    private static final String PAYMENT_UNPAID = "Unpaid";
    private static final String PAYMENT_PAID = "Paid";

    private static final String DINE_IN = "Dine In";
    private static final String TAKE_OUT = "Take Out";

    private int orderId;
    private Customer customer;
    private LocalDate orderDate;
    private double totalAmount;
    private String orderStatus;
    private String diningOption;
    private ArrayList<OrderItem> items;
    private Payment payment;
    private final String trackingToken;

    // Existing constructor for compatibility
    public Order(int orderId, Customer customer) {
        this(orderId, customer, DINE_IN);
    }

    // Constructor with dining option
    public Order(int orderId, Customer customer, String diningOption) {
        this.orderId = orderId;
        this.customer = customer;
        this.orderDate = LocalDate.now();
        this.totalAmount = 0.0;
        this.orderStatus = STATUS_PENDING;
        this.items = new ArrayList<>();
        this.payment = null;
        this.trackingToken = generateTrackingToken();

        setDiningOption(diningOption);
    }

    public int getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getPaymentStatus() {
        return payment == null ? PAYMENT_UNPAID : PAYMENT_PAID;
    }

    public String getTrackingToken() {
        return trackingToken;
    }

    public String getDiningOption() {
        return diningOption;
    }

    public void setDiningOption(String diningOption) {
        if (diningOption == null) {
            throw new IllegalArgumentException("Dining option is required.");
        }

        String option = diningOption.trim();

        if (!DINE_IN.equalsIgnoreCase(option)
                && !TAKE_OUT.equalsIgnoreCase(option)) {
            throw new IllegalArgumentException("Invalid dining option.");
        }

        this.diningOption = DINE_IN.equalsIgnoreCase(option)
                ? DINE_IN
                : TAKE_OUT;
    }

    public List<OrderItem> getItems() {
        List<OrderItem> itemCopies = new ArrayList<>(items.size());

        for (OrderItem item : items) {
            itemCopies.add(item.copy());
        }

        return Collections.unmodifiableList(itemCopies);
    }

    public Payment getPayment() {
        return payment;
    }

    public void addItem(FoodItem foodItem, int quantity) {
        if (payment != null) {
            throw new IllegalStateException(
                    "Items cannot be added after payment."
            );
        }

        if (foodItem != null && foodItem.isAvailable() && quantity > 0) {
            items.add(new OrderItem(foodItem, quantity));
            calculateTotal();
        } else {
            System.out.println(
                    "Invalid item, unavailable food, or quantity."
            );
        }
    }

    public double calculateTotal() {
        totalAmount = 0.0;

        for (OrderItem item : items) {
            totalAmount += item.calculateSubtotal();
        }

        return totalAmount;
    }

    public void updateStatus(String orderStatus) {
        if (orderStatus == null || orderStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Order status is required.");
        }

        String nextStatus = orderStatus.trim();

        if (!STATUS_PENDING.equals(nextStatus)
                && !STATUS_PROCESSING.equals(nextStatus)
                && !STATUS_READY.equals(nextStatus)
                && !STATUS_COMPLETED.equals(nextStatus)) {
            throw new IllegalArgumentException(
                    "Order status is not recognized."
            );
        }

        if (this.orderStatus.equals(nextStatus)) {
            return;
        }

        boolean validTransition =
                (STATUS_PENDING.equals(this.orderStatus)
                        && STATUS_PROCESSING.equals(nextStatus))
                || (STATUS_PROCESSING.equals(this.orderStatus)
                        && STATUS_READY.equals(nextStatus))
                || (STATUS_READY.equals(this.orderStatus)
                        && STATUS_COMPLETED.equals(nextStatus));

        if (!validTransition) {
            throw new IllegalStateException(
                    "Order status can only move to the next preparation stage."
            );
        }

        this.orderStatus = nextStatus;
    }

    public void setPayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required.");
        }

        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "An empty order cannot be paid."
            );
        }

        if (this.payment != null) {
            throw new IllegalStateException(
                    "Payment has already been set for this order."
            );
        }

        if (payment.getAmount() < calculateTotal()) {
            throw new IllegalArgumentException(
                    "Payment amount is less than the order total."
            );
        }

        this.payment = payment;
    }

    public void displayOrder() {
        System.out.println("\n===== ORDER DETAILS =====");
        System.out.println("Order ID: " + orderId);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Date: " + orderDate);
        System.out.println("Dining Option: " + diningOption);
        System.out.println("Order Status: " + orderStatus);
        System.out.println("Payment Status: " + getPaymentStatus());

        for (OrderItem item : items) {
            item.displayOrderItem();
        }

        System.out.println("-------------------------");
        System.out.println("Total: ₱" + calculateTotal());
    }

    private static String generateTrackingToken() {
        byte[] tokenBytes = new byte[32];
        TRACKING_TOKEN_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }
}
