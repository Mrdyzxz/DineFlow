package model;

public class OrderItem {

    private FoodItem foodItem;
    private int quantity;

    public OrderItem(FoodItem foodItem, int quantity) {
        if (foodItem == null) {
            throw new IllegalArgumentException("Food item is required.");
        }
        if (!foodItem.isAvailable()) {
            throw new IllegalArgumentException("Food item is unavailable.");
        }
        validateQuantity(quantity);
        this.foodItem = copyFoodItem(foodItem);
        this.quantity = quantity;
    }

    public FoodItem getFoodItem() {
        return copyFoodItem(foodItem);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public double calculateSubtotal() {
        return foodItem.getFoodPrice() * quantity;
    }

    public void displayOrderItem() {
        System.out.println(foodItem.getFoodName()
                + " x " + quantity
                + " = ₱" + calculateSubtotal());
    }

    OrderItem copy() {
        return new OrderItem(foodItem, quantity);
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    private static FoodItem copyFoodItem(FoodItem foodItem) {
        FoodItem copy = new FoodItem(
            foodItem.getFoodId(),
            foodItem.getFoodName(),
            foodItem.getFoodPrice(),
            foodItem.getFoodCategory()
        );
        copy.setAvailable(foodItem.isAvailable());
        return copy;
    }
}