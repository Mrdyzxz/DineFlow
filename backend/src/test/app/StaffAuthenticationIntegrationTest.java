package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.Customer;
import model.FoodItem;
import model.Order;
import service.RestaurantService;
import service.StaffAuthenticationService;

public final class StaffAuthenticationIntegrationTest {

    private static final String TEST_USERNAME = "dineflow-test-staff";
    private static final String TEST_PASSWORD = "Local-Test-Password-2026!";

    private StaffAuthenticationIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        RestaurantService service = new RestaurantService();
        service.addFoodItem(new FoodItem(1, "Test meal", 100.0, "Test"));
        Order existingOrder = service.createOrder(
                1, new Customer(1, "Existing Guest", "test")
        );

        char[] password = TEST_PASSWORD.toCharArray();
        String encodedPasswordHash;
        try {
            encodedPasswordHash =
                    StaffAuthenticationService.createPasswordHash(password);
        } finally {
            Arrays.fill(password, '\0');
        }

        setStaticField("service", service);
        setStaticField(
                "staffAuthentication",
                new StaffAuthenticationService(
                        TEST_USERNAME, encodedPasswordHash
                )
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/api/orders", invokeApiHandler("handleOrders"));
        server.createContext("/api/staff", invokeApiHandler("handleStaff"));
        server.createContext("/api/foods", invokeApiHandler("handleFoods"));
        server.start();

        try {
            verifyProtectedStaffRoutes(server, service, existingOrder);
            verifyCustomerRoutesRemainPublic(server);
        } finally {
            server.stop(0);
        }

        System.out.println("Staff authentication integration checks passed.");
    }

    private static void verifyProtectedStaffRoutes(
            HttpServer server,
            RestaurantService service,
            Order existingOrder
    ) throws Exception {
        HttpResponse<String> response = request(
                server, "OPTIONS", "/api/staff/login", null, null
        );
        requireStatus(response, 204, "staff login CORS preflight");
        require(response.headers()
                        .firstValue("Access-Control-Allow-Headers")
                        .orElse("")
                        .contains("Authorization"),
                "CORS must allow bearer authorization headers");

        response = request(
                server, "GET", "/api/orders", null, null
        );
        requireStatus(response, 401, "unauthenticated order listing");

        response = request(
                server,
                "POST",
                "/api/orders/1/status",
                "status=Processing",
                null
        );
        requireStatus(response, 401, "unauthenticated status update");
        require("Pending".equals(existingOrder.getOrderStatus()),
                "unauthenticated status update must not change the order");

        response = request(
                server,
                "POST",
                "/api/foods",
                "name=Blocked&category=Test&price=1",
                null
        );
        requireStatus(response, 401, "unauthenticated food creation");
        require(service.getFoodItems().size() == 1,
                "unauthenticated food creation must not change the menu");

        response = request(
                server,
                "POST",
                "/api/staff/login",
                credentials("wrong-user", TEST_PASSWORD),
                null
        );
        requireStatus(response, 401, "invalid staff credentials");

        response = request(
                server,
                "POST",
                "/api/staff/login",
                credentials(TEST_USERNAME, TEST_PASSWORD),
                null
        );
        requireStatus(response, 200, "valid staff login");
        require(response.headers()
                        .firstValue("Cache-Control")
                        .orElse("")
                        .contains("no-store"),
                "login responses must not be cached");

        Matcher tokenMatcher = Pattern.compile("\"token\":\"([^\"]+)\"")
                .matcher(response.body());
        require(tokenMatcher.find(), "login must return a bearer token");
        String token = tokenMatcher.group(1);

        response = request(
                server, "GET", "/api/orders", null, token
        );
        requireStatus(response, 200, "authenticated order listing");
        require(response.body().contains("Existing Guest"),
                "staff order listing must include existing orders");
        require(!response.body().contains("trackingToken"),
                "staff order listing must not expose tracking tokens");

        response = request(
                server,
                "POST",
                "/api/orders/1/status",
                "status=Processing",
                token
        );
        requireStatus(response, 200, "Pending to Processing transition");

        response = request(
                server,
                "POST",
                "/api/orders/1/status",
                "status=Ready",
                token
        );
        requireStatus(response, 200, "Processing to Ready transition");

        response = request(
                server,
                "POST",
                "/api/orders/1/status",
                "status=Completed",
                token
        );
        requireStatus(response, 200, "Ready to Completed transition");

        response = request(
                server,
                "POST",
                "/api/orders/1/status",
                "status=Processing",
                token
        );
        requireStatus(response, 409, "rejected backward transition");

        response = request(
                server,
                "POST",
                "/api/foods",
                "name=Staff+meal&category=Test&price=12",
                token
        );
        requireStatus(response, 201, "authenticated food creation");

        response = request(
                server, "POST", "/api/staff/logout", null, token
        );
        requireStatus(response, 200, "staff logout");

        response = request(
                server, "GET", "/api/orders", null, token
        );
        requireStatus(response, 401, "revoked session");
    }

    private static void verifyCustomerRoutesRemainPublic(HttpServer server)
            throws Exception {
        HttpResponse<String> response = request(
                server,
                "GET",
                "/api/foods",
                null,
                null
        );
        requireStatus(response, 200, "public food menu");

        response = request(
                server,
                "POST",
                "/api/orders",
                "customerName=Guest&contactInfo=test&diningOption=Dine+In"
                        + "&item=1%3A1",
                null
        );
        requireStatus(response, 201, "public customer order creation");

        Matcher tokenMatcher = Pattern.compile(
                "\"trackingToken\":\"([^\"]+)\""
        ).matcher(response.body());
        require(tokenMatcher.find(), "created order must return tracking token");
        Matcher orderIdMatcher = Pattern.compile("\"id\":(\\d+)")
                .matcher(response.body());
        require(orderIdMatcher.find(), "created order must return its ID");
        String orderId = orderIdMatcher.group(1);
        String trackingToken = tokenMatcher.group(1);

        response = request(
                server,
                "POST",
                "/api/orders/" + orderId + "/payment",
                "paymentMethod=cash",
                null
        );
        requireStatus(response, 200, "public customer payment");
        require(response.body().contains("\"orderStatus\":\"Pending\""),
                "payment must not advance preparation status");
        require(response.body().contains("\"paymentStatus\":\"Paid\""),
                "payment must remain a separate status");

        response = request(
                server,
                "GET",
                "/api/orders/tracking/" + trackingToken,
                null,
                null
        );
        requireStatus(response, 200, "public token-based order tracking");
        require(response.body().contains("\"paymentStatus\":\"Paid\""),
                "customer tracking must report payment status");
    }

    private static HttpResponse<String> request(
            HttpServer server,
            String method,
            String path,
            String body,
            String token
    ) throws Exception {
        String url = "http://127.0.0.1:"
                + server.getAddress().getPort() + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url));

        if (body != null) {
            builder.header(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
            );
            builder.method(
                    method,
                    HttpRequest.BodyPublishers.ofString(body)
            );
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return HttpClient.newHttpClient().send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String credentials(String username, String password)
            throws Exception {
        return "username=" + URLEncoder.encode(username, "UTF-8")
                + "&password=" + URLEncoder.encode(password, "UTF-8");
    }

    private static HttpHandler invokeApiHandler(String methodName) {
        return exchange -> {
            try {
                Method method = ApiServer.class.getDeclaredMethod(
                        methodName, HttpExchange.class
                );
                method.setAccessible(true);
                method.invoke(null, exchange);
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                throw new IOException("API handler failed.", cause);
            } catch (ReflectiveOperationException error) {
                throw new IOException("Unable to invoke API handler.", error);
            }
        };
    }

    private static void setStaticField(String name, Object value)
            throws Exception {
        Field field = ApiServer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void requireStatus(
            HttpResponse<String> response,
            int expectedStatus,
            String scenario
    ) {
        require(response.statusCode() == expectedStatus,
                scenario + " expected HTTP " + expectedStatus
                        + " but received " + response.statusCode()
                        + ": " + response.body());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
