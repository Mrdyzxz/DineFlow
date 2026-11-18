# DineFlow class diagram

The diagram reflects the Java classes under `backend/src/main`. In Mermaid,
`$` marks static members and `*` marks abstract methods.

```mermaid
classDiagram
    class User {
        <<abstract>>
        -int userId
        -String name
        -String contactInfo
        +User(int, String, String)
        +getUserId() int
        +getName() String
        +getContactInfo() String
        +setName(String) void
        +setContactInfo(String) void
        +displayInfo()*
    }

    class Customer {
        +Customer(int, String, String)
        +displayInfo() void
        +placeOrder(Order) void
    }

    class RestaurantStaff {
        +RestaurantStaff(int, String, String)
        +displayInfo() void
        +manageFoodItems(FoodItem) void
        +processOrders(Order) void
    }

    class FoodItem {
        -int foodId
        -String foodName
        -double foodPrice
        -String foodCategory
        -boolean available
        +FoodItem(int, String, double, String)
        +getFoodId() int
        +getFoodName() String
        +getFoodPrice() double
        +getFoodCategory() String
        +isAvailable() boolean
        +setFoodName(String) void
        +setFoodPrice(double) void
        +setFoodCategory(String) void
        +setAvailable(boolean) void
        +displayFoodInfo() void
    }

    class OrderItem {
        -FoodItem foodItem
        -int quantity
        +OrderItem(FoodItem, int)
        +getFoodItem() FoodItem
        +getQuantity() int
        +setQuantity(int) void
        +calculateSubtotal() double
        +displayOrderItem() void
        ~copy() OrderItem
        -validateQuantity(int)$ void
        -copyFoodItem(FoodItem)$ FoodItem
    }

    class Order {
        -String STATUS_PENDING$
        -String STATUS_PROCESSING$
        -String STATUS_PAID$
        -int orderId
        -Customer customer
        -LocalDate orderDate
        -double totalAmount
        -String orderStatus
        -ArrayList~OrderItem~ items
        -Payment payment
        +Order(int, Customer)
        +getOrderId() int
        +getCustomer() Customer
        +getOrderDate() LocalDate
        +getTotalAmount() double
        +getOrderStatus() String
        +getItems() List~OrderItem~
        +getPayment() Payment
        +addItem(FoodItem, int) void
        +calculateTotal() double
        +updateStatus(String) void
        +setPayment(Payment) void
        +displayOrder() void
    }

    class Payment {
        <<abstract>>
        -int paymentId
        -double amount
        +Payment(int, double)
        +getPaymentId() int
        +getAmount() double
        +processPayment()*
    }

    class CashPayment {
        +CashPayment(int, double)
        +processPayment() void
    }

    class EWalletPayment {
        -String walletName
        +EWalletPayment(int, double, String)
        +getWalletName() String
        +setWalletName(String) void
        +processPayment() void
    }

    class RestaurantService {
        -ArrayList~FoodItem~ foodItems
        -ArrayList~Order~ orders
        +RestaurantService()
        +addFoodItem(FoodItem) void
        +displayFoodItems() void
        +createOrder(int, Customer) Order
        +processOrder(RestaurantStaff, Order) void
        +processPayment(Order, Payment) void
        +displayOrders() void
        +getFoodItems() List~FoodItem~
        +getOrders() List~Order~
        +getFoodItem(int) FoodItem
    }

    class ApiServer {
        -RestaurantService service$
        +ApiServer()
        +main(String[])$ void
        -addSampleFoodItems()$ void
        -handleFoods(HttpExchange)$ void
        -handleOrders(HttpExchange)$ void
        -addCorsHeaders(HttpExchange)$ void
        -getFoods(HttpExchange)$ void
        -addFood(HttpExchange)$ void
        -getOrders(HttpExchange)$ void
        -addOrder(HttpExchange)$ void
        -validateItems(List~String~)$ List~OrderItemRequest~
        -appendOrderJson(StringBuilder, Order)$ void
        -readForm(HttpExchange)$ Map~String, List~String~~
        -getValue(Map~String, List~String~~, String)$ String
        -errorJson(String)$ String
        -escapeJson(String)$ String
        -sendResponse(HttpExchange, int, String)$ void
    }

    class OrderItemRequest {
        <<private static nested>>
        -FoodItem foodItem
        -int quantity
        -OrderItemRequest(FoodItem, int)
    }

    class Main {
        +Main()
        +main(String[])$ void
    }

    User <|-- Customer
    User <|-- RestaurantStaff
    Payment <|-- CashPayment
    Payment <|-- EWalletPayment
    Order "1" *-- "0..*" OrderItem : items
    OrderItem "1" *-- "1" FoodItem : private food snapshot
    Order "*" --> "0..1" Customer : customer
    Order "0..*" --> "0..1" Payment : payment reference
    RestaurantService "0..*" o-- "0..*" FoodItem : menu references
    RestaurantService "1" o-- "0..*" Order : order history
    RestaurantService ..> Customer : createOrder parameter
    RestaurantService ..> RestaurantStaff : processOrder parameter
    RestaurantService ..> Payment : processPayment parameter
    ApiServer --> RestaurantService : static service field
    ApiServer ..> OrderItemRequest : creates and validates
    OrderItemRequest "*" --> "1" FoodItem : foodItem
    ApiServer ..> Customer : creates
    ApiServer ..> FoodItem : creates and reads
    ApiServer ..> Order : creates and reads
    ApiServer ..> RestaurantStaff : creates
    ApiServer ..> Payment : creates and processes
    ApiServer ..> CashPayment : creates
    ApiServer ..> EWalletPayment : creates
    Main ..> RestaurantService : local variable
    Main ..> Customer : creates and uses
    Main ..> RestaurantStaff : creates and uses
    Main ..> FoodItem : creates and reads
    Main ..> Order : creates and uses
    Main ..> Payment : selects and processes
    Main ..> CashPayment : creates
    Main ..> EWalletPayment : creates
```

`Order` contains zero or more `OrderItem` snapshots. Each order has zero or one
payment reference; the code does not prevent the same `Payment` object from
being assigned to multiple orders, hence the `0..*` multiplicity at the
`Order` end. An `Order` can also be constructed with a null customer, so the
customer multiplicity is optional in the diagram. `OrderItemRequest` is a
private static nested helper in `ApiServer`; its fields are `final` in Java.
The status constants in `Order` are also `static final`.
`ApiServer()` and `Main()` are compiler-generated public default constructors;
they are shown to reflect the classes' Java-visible constructors.
