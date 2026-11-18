package model;

public class FoodItem {
    private int foodId;
    private String foodName;
    private double foodPrice;
    private String foodCategory;
    private boolean available;

    public FoodItem(int foodId, String foodName, double foodPrice, String foodCategory) {
        this.foodId = foodId;
        setFoodName(foodName);
        setFoodPrice(foodPrice);
        this.foodCategory = foodCategory;
        this.available = true;
    }

    public int getFoodId() {
        return foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public double getFoodPrice() {
        return foodPrice;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setFoodName(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            throw new IllegalArgumentException("Food name is required.");
        }
        this.foodName = foodName.trim();
    }

    public void setFoodPrice(double foodPrice) {
        if (!Double.isFinite(foodPrice) || foodPrice <= 0) {
            throw new IllegalArgumentException("Food price must be greater than zero.");
        }
        this.foodPrice = foodPrice;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void displayFoodInfo() {
        System.out.println("Food ID: " + foodId);
        System.out.println("Food Name: " + foodName);
        System.out.println("Price: ₱" + foodPrice);
        System.out.println("Category: " + foodCategory);
        System.out.println("Available: " + available);
    }
}