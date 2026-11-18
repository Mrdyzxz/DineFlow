
package app;

import java.util.Scanner;

import model.Customer;
import model.FoodItem;
import model.Order;
import model.RestaurantStaff;

import payment.CashPayment;
import payment.EWalletPayment;
import payment.Payment;

import service.RestaurantService;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        RestaurantService service = new RestaurantService();

        Customer customer = new Customer(
                101, "Juan Dela Cruz", "09123456789"
        );

        RestaurantStaff staff = new RestaurantStaff(
                201, "Maria Santos", "09987654321"
        );

        // Sample food menu
        service.addFoodItem(new FoodItem(
                1, "Chicken Burger", 99.00, "Burger"
        ));

        service.addFoodItem(new FoodItem(
                2, "Cheese Pizza", 150.00, "Pizza"
        ));

        service.addFoodItem(new FoodItem(
                3, "French Fries", 65.00, "Side Dish"
        ));

        service.addFoodItem(new FoodItem(
                4, "Iced Tea", 35.00, "Drinks"
        ));

        int choice;

        do {
            System.out.println("\n================================");
            System.out.println("       DINEFLOW SYSTEM");
            System.out.println("   FOOD ORDERING MANAGEMENT");
            System.out.println("================================");
            System.out.println("1. View Food Menu");
            System.out.println("2. Place Order");
            System.out.println("3. View All Orders");
            System.out.println("4. View Customer Information");
            System.out.println("5. View Staff Information");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            choice = scanner.nextInt();

            switch (choice) {

                case 1:
                    service.displayFoodItems();
                    break;

                case 2:
                    Order order = service.createOrder(
                            service.getOrders().size() + 1,
                            customer
                    );

                    customer.placeOrder(order);

                    int foodChoice;

                    do {
                        service.displayFoodItems();

                        System.out.println("0. Finish Adding Items");
                        System.out.print("Enter Food ID: ");

                        foodChoice = scanner.nextInt();

                        if (foodChoice != 0) {
                            System.out.print("Enter Quantity: ");
                            int quantity = scanner.nextInt();

                            FoodItem selectedFood = null;

                            for (FoodItem food : service.getFoodItems()) {
                                if (food.getFoodId() == foodChoice) {
                                    selectedFood = food;
                                    break;
                                }
                            }

                            if (selectedFood != null) {
                                order.addItem(selectedFood, quantity);
                            } else {
                                System.out.println("Food not found!");
                            }
                        }

                    } while (foodChoice != 0);

                    if (order.getItems().isEmpty()) {
                        System.out.println("Order cancelled: No items selected.");
                        break;
                    }

                    order.displayOrder();

                    service.processOrder(staff, order);

                    System.out.println("\n===== PAYMENT METHOD =====");
                    System.out.println("1. Cash");
                    System.out.println("2. E-Wallet");
                    System.out.print("Choose payment: ");

                    int paymentChoice = scanner.nextInt();

                    double total = order.calculateTotal();

                    Payment payment;

                    if (paymentChoice == 1) {
                        payment = new CashPayment(
                                1001, total
                        );
                    } else if (paymentChoice == 2) {
                        scanner.nextLine();

                        System.out.print("Enter Wallet Name (GCash/Maya): ");
                        String walletName = scanner.nextLine();

                        payment = new EWalletPayment(
                                1002, total, walletName
                        );
                    } else {
                        System.out.println("Invalid payment method.");
                        break;
                    }

                    service.processPayment(order, payment);

                    order.displayOrder();
                    break;

                case 3:
                    service.displayOrders();
                    break;

                case 4:
                    customer.displayInfo();
                    break;

                case 5:
                    staff.displayInfo();
                    break;

                case 0:
                    System.out.println("Thank you for using DineFlow!");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 0);

        scanner.close();
    }
}
