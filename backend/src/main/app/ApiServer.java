
package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import model.Customer;
import model.FoodItem;
import model.Order;
import model.OrderItem;
import payment.CashPayment;
import payment.EWalletPayment;
import payment.Payment;
import service.RestaurantService;

public class ApiServer {

    private static RestaurantService service;

    public static void main(String[] args) throws IOException {
        service = new RestaurantService();
        addSampleFoodItems();

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        HttpServer server = HttpServer.create(
                new InetSocketAddress(port), 0
        );

        server.createContext("/api/foods", ApiServer::handleFoods);
        server.createContext("/api/orders", ApiServer::handleOrders);

        server.start();

        System.out.println("================================");
        System.out.println("       DINEFLOW API SERVER");
        System.out.println("================================");
        System.out.println("Server running at http://localhost:" + port);
    }

   
private static void addSampleFoodItems() {

    // ==============================
    // BURGERS
    // ==============================

    service.addFoodItem(new FoodItem(1, "Chicken Burger", 99.00, "Burgers"));
    service.addFoodItem(new FoodItem(2, "Cheeseburger", 119.00, "Burgers"));
    service.addFoodItem(new FoodItem(3, "Bacon Burger", 149.00, "Burgers"));

    // ==============================
    // RICE MEALS
    // ==============================

    service.addFoodItem(new FoodItem(4, "Chicken Fillet Meal", 129.00, "Rice Meals"));
    service.addFoodItem(new FoodItem(5, "Beef Tapa Meal", 159.00, "Rice Meals"));
    service.addFoodItem(new FoodItem(6, "Pork Sisig Meal", 139.00, "Rice Meals"));

    // ==============================
    // CHICKEN
    // ==============================

    service.addFoodItem(new FoodItem(7, "1pc Fried Chicken", 99.00, "Chicken"));
    service.addFoodItem(new FoodItem(8, "2pc Fried Chicken", 179.00, "Chicken"));

    // ==============================
    // PIZZA
    // ==============================

    service.addFoodItem(new FoodItem(9, "Cheese Pizza", 150.00, "Pizza"));
    service.addFoodItem(new FoodItem(10, "Pepperoni Pizza", 199.00, "Pizza"));

    // ==============================
    // SIDES
    // ==============================

    service.addFoodItem(new FoodItem(11, "French Fries", 65.00, "Sides"));
    service.addFoodItem(new FoodItem(12, "Onion Rings", 79.00, "Sides"));
    service.addFoodItem(new FoodItem(13, "Chicken Nuggets", 89.00, "Sides"));

    // ==============================
    // DRINKS
    // ==============================

    service.addFoodItem(new FoodItem(14, "Iced Tea", 35.00, "Drinks"));
    service.addFoodItem(new FoodItem(15, "Coca-Cola", 45.00, "Drinks"));
    service.addFoodItem(new FoodItem(16, "Lemonade", 49.00, "Drinks"));

    // ==============================
    // DESSERTS
    // ==============================

    service.addFoodItem(new FoodItem(17, "Vanilla Sundae", 39.00, "Desserts"));
    service.addFoodItem(new FoodItem(18, "Chocolate Sundae", 49.00, "Desserts"));
}

    // ==============================
    // FOOD ENDPOINTS
    // ==============================

    private static void handleFoods(HttpExchange exchange)
            throws IOException {

        addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
        } else if (method.equals("GET")) {
            getFoods(exchange);
        } else if (method.equals("POST")) {
            addFood(exchange);
        } else {
            sendResponse(exchange, 405,
                    errorJson("Method not allowed"));
        }
    }

    private static void getFoods(HttpExchange exchange)
            throws IOException {

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        synchronized (service) {
            for (FoodItem food : service.getFoodItems()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"id\":").append(food.getFoodId()).append(",")
                    .append("\"name\":\"")
                    .append(escapeJson(food.getFoodName())).append("\",")
                    .append("\"category\":\"")
                    .append(escapeJson(food.getFoodCategory())).append("\",")
                    .append("\"price\":").append(food.getFoodPrice()).append(",")
                    .append("\"available\":").append(food.isAvailable())
                    .append("}");

                first = false;
            }
        }

        sendResponse(exchange, 200, json.append("]").toString());
    }

    private static void addFood(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, List<String>> data = readForm(exchange);

            String name = getValue(data, "name").trim();
            String category = getValue(data, "category").trim();
            String priceText = getValue(data, "price").trim();

            if (name.isEmpty() || category.isEmpty()
                    || priceText.isEmpty()) {
                sendResponse(exchange, 400,
                        errorJson("All fields are required"));
                return;
            }

            double price;

            try {
                price = Double.parseDouble(priceText);
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400,
                        errorJson("Invalid price"));
                return;
            }

            if (!Double.isFinite(price) || price <= 0) {
                sendResponse(exchange, 400,
                        errorJson("Price must be greater than zero"));
                return;
            }

            FoodItem newFood;

            synchronized (service) {
                int nextId = service.getFoodItems().stream()
                    .mapToInt(FoodItem::getFoodId)
                    .max()
                    .orElse(0) + 1;

                newFood = new FoodItem(
                        nextId, name, price, category
                );

                service.addFoodItem(newFood);
            }

            String response = "{"
                + "\"message\":\"Food item added successfully\","
                + "\"id\":" + newFood.getFoodId() + ","
                + "\"name\":\""
                + escapeJson(newFood.getFoodName()) + "\","
                + "\"category\":\""
                + escapeJson(newFood.getFoodCategory()) + "\","
                + "\"price\":" + newFood.getFoodPrice() + ","
                + "\"available\":" + newFood.isAvailable()
                + "}";

            sendResponse(exchange, 201, response);

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400,
                    errorJson(e.getMessage()));
        }
    }

    // ==============================
    // ORDER ENDPOINTS
    // ==============================

    private static void handleOrders(HttpExchange exchange)
            throws IOException {

        addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (path.matches("/api/orders/[0-9]+/payment")) {
            if (method.equals("POST")) {
                confirmPayment(exchange, path);
            } else {
                sendResponse(exchange, 405,
                        errorJson("Method not allowed"));
            }
            return;
        }

        if (!path.equals("/api/orders")
                && !path.equals("/api/orders/")) {
            sendResponse(exchange, 404,
                    errorJson("Order endpoint not found"));
            return;
        }

        if (method.equals("GET")) {
            getOrders(exchange);
        } else if (method.equals("POST")) {
            addOrder(exchange);
        } else {
            sendResponse(exchange, 405,
                    errorJson("Method not allowed"));
        }
    }

    // GET /api/orders
    private static void getOrders(HttpExchange exchange)
            throws IOException {

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        synchronized (service) {
            for (Order order : service.getOrders()) {
                if (!first) {
                    json.append(",");
                }

                appendOrderJson(json, order);
                first = false;
            }
        }

        sendResponse(exchange, 200, json.append("]").toString());
    }

    // POST /api/orders
    // Creates an order without processing payment.
    private static void addOrder(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, List<String>> data = readForm(exchange);

            String customerName =
                    getValue(data, "customerName").trim();

            String contactInfo =
                    getValue(data, "contactInfo").trim();

            String diningOption =
                    getValue(data, "diningOption").trim();

            List<String> requestedItems = data.get("item");

            if (customerName.isEmpty() || contactInfo.isEmpty()) {
                sendResponse(exchange, 400,
                        errorJson("Customer name and contact are required"));
                return;
            }

            if (requestedItems == null || requestedItems.isEmpty()) {
                sendResponse(exchange, 400,
                        errorJson("Select at least one food item"));
                return;
            }

            synchronized (service) {
                List<OrderItemRequest> validatedItems =
                        validateItems(requestedItems);

                int orderId = service.getOrders().stream()
                    .mapToInt(Order::getOrderId)
                    .max()
                    .orElse(0) + 1;

                Customer customer = new Customer(
                        orderId, customerName, contactInfo
                );

                Order order = service.createOrder(
                        orderId, customer, diningOption
                );

                for (OrderItemRequest item : validatedItems) {
                    order.addItem(item.foodItem, item.quantity);
                }

                StringBuilder response = new StringBuilder();
                appendOrderJson(response, order);

                sendResponse(exchange, 201, response.toString());
            }

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400,
                    errorJson(e.getMessage()));
        } catch (IllegalStateException e) {
            sendResponse(exchange, 409,
                    errorJson(e.getMessage()));
        }
    }

    // POST /api/orders/{id}/payment
    // Payment is processed only when the customer confirms.
    private static void confirmPayment(
            HttpExchange exchange,
            String path
    ) throws IOException {

        try {
            String[] pathParts = path.split("/");
            int orderId = Integer.parseInt(pathParts[3]);

            Map<String, List<String>> data = readForm(exchange);

            String paymentMethod = getValue(data, "paymentMethod")
                    .trim().toLowerCase(Locale.ROOT);

            String walletName = getValue(data, "walletName").trim();

            if (!paymentMethod.equals("cash")
                    && !paymentMethod.equals("ewallet")) {
                sendResponse(exchange, 400,
                        errorJson("Choose cash or e-wallet payment"));
                return;
            }

            if (paymentMethod.equals("ewallet")
                    && walletName.isEmpty()) {
                sendResponse(exchange, 400,
                        errorJson("Wallet name is required for e-wallet payment"));
                return;
            }

            synchronized (service) {
                Order order = findOrder(orderId);

                if (order == null) {
                    sendResponse(exchange, 404,
                            errorJson("Order not found"));
                    return;
                }

                if (order.getItems().isEmpty()) {
                    sendResponse(exchange, 400,
                            errorJson("Order has no items"));
                    return;
                }

                if (order.getPayment() != null
                        || order.getOrderStatus().equals("Paid")) {
                    sendResponse(exchange, 409,
                            errorJson("This order has already been paid"));
                    return;
                }

                double total = order.calculateTotal();

                Payment payment;

                if (paymentMethod.equals("cash")) {
                    payment = new CashPayment(orderId, total);
                } else {
                    payment = new EWalletPayment(
                            orderId, total, walletName
                    );
                }

                service.processPayment(order, payment);

                StringBuilder response = new StringBuilder();
                appendOrderJson(response, order);

                sendResponse(exchange, 200, response.toString());
            }

        } catch (NumberFormatException e) {
            sendResponse(exchange, 400,
                    errorJson("Invalid order ID"));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400,
                    errorJson(e.getMessage()));
        } catch (IllegalStateException e) {
            sendResponse(exchange, 409,
                    errorJson(e.getMessage()));
        }
    }

    private static Order findOrder(int orderId) {
        for (Order order : service.getOrders()) {
            if (order.getOrderId() == orderId) {
                return order;
            }
        }

        return null;
    }

    // ==============================
    // ORDER VALIDATION AND JSON
    // ==============================

    private static List<OrderItemRequest> validateItems(
            List<String> requestedItems
    ) {

        List<OrderItemRequest> validatedItems = new ArrayList<>();

        for (String request : requestedItems) {
            String[] parts = request.split(":", -1);

            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid item selection"
                );
            }

            int foodId;
            int quantity;

            try {
                foodId = Integer.parseInt(parts[0]);
                quantity = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Food ID and quantity must be whole numbers"
                );
            }

            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero"
                );
            }

            FoodItem foodItem = service.getFoodItem(foodId);

            if (foodItem == null || !foodItem.isAvailable()) {
                throw new IllegalArgumentException(
                        "Selected food item is unavailable"
                );
            }

            validatedItems.add(
                    new OrderItemRequest(foodItem, quantity)
            );
        }

        return validatedItems;
    }

    private static void appendOrderJson(
            StringBuilder json,
            Order order
    ) {

        json.append("{")
            .append("\"id\":").append(order.getOrderId()).append(",")
            .append("\"customer\":\"")
            .append(escapeJson(order.getCustomer().getName())).append("\",")
            .append("\"date\":\"")
            .append(order.getOrderDate()).append("\",")
            .append("\"diningOption\":\"")
            .append(escapeJson(order.getDiningOption())).append("\",")
            .append("\"total\":").append(order.calculateTotal()).append(",")
            .append("\"status\":\"")
            .append(escapeJson(order.getOrderStatus())).append("\",")
            .append("\"paymentStatus\":\"")
            .append(order.getPayment() == null ? "Pending" : "Paid")
            .append("\",")
            .append("\"paymentMethod\":\"")
            .append(getPaymentMethod(order))
            .append("\",")
            .append("\"items\":[");

        List<OrderItem> items = order.getItems();

        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            FoodItem food = item.getFoodItem();

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                .append("\"foodId\":").append(food.getFoodId()).append(",")
                .append("\"name\":\"")
                .append(escapeJson(food.getFoodName())).append("\",")
                .append("\"quantity\":").append(item.getQuantity()).append(",")
                .append("\"price\":").append(food.getFoodPrice()).append(",")
                .append("\"subtotal\":").append(item.calculateSubtotal())
                .append("}");
        }

        json.append("]}");
    }

    private static String getPaymentMethod(Order order) {
        Payment payment = order.getPayment();

        if (payment == null) {
            return "Not yet paid";
        }

        if (payment instanceof CashPayment) {
            return "Cash";
        }

        if (payment instanceof EWalletPayment) {
            return "E-Wallet";
        }

        return "Other";
    }

    // ==============================
    // FORM AND JSON UTILITIES
    // ==============================

    private static Map<String, List<String>> readForm(
            HttpExchange exchange
    ) throws IOException {

        InputStream input = exchange.getRequestBody();

        String body = new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, List<String>> data = new HashMap<>();

        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }

            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
            );

            String value = parts.length == 2
                    ? URLDecoder.decode(
                            parts[1], StandardCharsets.UTF_8
                    )
                    : "";

            data.computeIfAbsent(
                    key, ignored -> new ArrayList<>()
            ).add(value);
        }

        return data;
    }

    private static String getValue(
            Map<String, List<String>> data,
            String key
    ) {

        List<String> values = data.get(key);

        return values == null || values.isEmpty()
                ? ""
                : values.get(0);
    }

    private static String errorJson(String message) {
        return "{\"error\":\""
                + escapeJson(
                        message == null ? "Invalid request" : message
                )
                + "\"}";
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(
                                String.format(
                                        "\\u%04x",
                                        (int) character
                                )
                        );
                    } else {
                        escaped.append(character);
                    }
            }
        }

        return escaped.toString();
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, OPTIONS"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type"
        );
    }

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String response
    ) throws IOException {

        byte[] data = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    // Helper class for validating requested order items
    private static class OrderItemRequest {

        private final FoodItem foodItem;
        private final int quantity;

        private OrderItemRequest(
                FoodItem foodItem,
                int quantity
        ) {
            this.foodItem = foodItem;
            this.quantity = quantity;
        }
    }
}
