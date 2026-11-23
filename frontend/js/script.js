
document.addEventListener("DOMContentLoaded", () => {

    // ==========================================
    // CONFIGURATION
    // ==========================================

   const API_URL = "https://dineflow-api-g1a3.onrender.com/api";

    // ==========================================
// FOOD IMAGE MAPPING
// ==========================================

const FOOD_IMAGES = {
    "chicken burger": "assets/foods/chicken-burger.png",
    "cheeseburger": "assets/foods/cheese-burger.png",
    "bacon burger": "assets/foods/bacon-burger.png",

    "chicken fillet meal": "assets/foods/chicken-fillet.png",
    "beef tapa meal": "assets/foods/beef-tapa.png",
    "pork sisig meal": "assets/foods/pork-sisig.png",

    "1pc fried chicken": "assets/foods/1pc-fried-chicken.png",
    "2pc fried chicken": "assets/foods/2pc-fried-chicken.png",

    "cheese pizza": "assets/foods/cheese-pizza.png",
    "pepperoni pizza": "assets/foods/pepperoni-pizza.png",

    "french fries": "assets/foods/french-fries.png",
    "onion rings": "assets/foods/onion-rings.png",
    "chicken nuggets": "assets/foods/chicken-nuggets.png",

    "iced tea": "assets/foods/iced-tea.png",
    "coca-cola": "assets/foods/coca-cola.png",
    "lemonade": "assets/foods/lemonade.png",

    "vanilla sundae": "assets/foods/vanilla-sundae.png",
    "chocolate sundae": "assets/foods/chocolate-sundae.png"
};

function getFoodImage(food) {
    return FOOD_IMAGES[food.name.trim().toLowerCase()] || "";
}

    let foodItems = [];
    let cart = [];
    let selectedCategory = "All";
    let diningOption = "Dine In";
    let customerDetails = {};
    let currentOrder = null;
    let paymentInProgress = false;

    const screens = document.querySelectorAll(".screen");

    // ==========================================
    // ELEMENT REFERENCES
    // ==========================================

    const foodGrid = document.getElementById("foodGrid");
    const cartItems = document.getElementById("cartItems");
    const cartCount = document.getElementById("cartCount");
    const cartTotal = document.getElementById("cartTotal");
    const checkoutBtn = document.getElementById("checkoutBtn");

    const customerForm = document.getElementById("customerForm");
    const checkoutItems = document.getElementById("checkoutItems");
    const checkoutTotal = document.getElementById("checkoutTotal");

    const ewalletPanel = document.getElementById("ewalletPanel");
    const paymentMessage = document.getElementById("paymentMessage");

    // ==========================================
    // GENERAL HELPERS
    // ==========================================

    function formatCurrency(amount) {
        return Number(amount).toLocaleString("en-PH", {
            style: "currency",
            currency: "PHP"
        });
    }

    function escapeHTML(value) {
        return String(value).replace(/[&<>"']/g, character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        })[character]);
    }

    function showScreen(screenId) {
        screens.forEach(screen => {
            screen.classList.remove("active");
        });

        const target = document.getElementById(screenId);

        if (target) {
            target.classList.add("active");
            window.scrollTo(0, 0);
        }
    }

    function getFoodEmoji(food) {
        const category = food.category.toLowerCase();
        const name = food.name.toLowerCase();

        if (category.includes("burger")) return "🍔";
        if (category.includes("rice")) return "🍚";
        if (category.includes("chicken")) return "🍗";
        if (category.includes("pizza")) return "🍕";
        if (category.includes("side")) {
            if (name.includes("onion")) return "🧅";
            if (name.includes("nugget")) return "🍗";
            return "🍟";
        }
        if (category.includes("drink")) {
            if (name.includes("coke")) return "🥤";
            if (name.includes("lemonade")) return "🍋";
            return "🧋";
        }
        if (category.includes("dessert")) return "🍦";

        return "🍽️";
    }

    // ==========================================
    // LOAD FOOD MENU FROM JAVA API
    // ==========================================

    async function loadFoodItems() {
        try {
            foodGrid.innerHTML =
                '<p class="loading-message">Loading delicious food...</p>';

            const response = await fetch(`${API_URL}/foods`);

            if (!response.ok) {
                throw new Error("Unable to load food menu.");
            }

            foodItems = await response.json();

            displayFoodItems();

        } catch (error) {
            console.error("Food menu error:", error);

            foodGrid.innerHTML = `
                <p class="loading-message">
                    Unable to connect to DineFlow server.
                    Please make sure the Java API is running.
                </p>
            `;
        }
    }

    // ==========================================
    // DISPLAY FOOD PRODUCTS
    // ==========================================

    function displayFoodItems() {
        const filteredFoods = foodItems.filter(food => {
            const matchesCategory =
                selectedCategory === "All" ||
                food.category === selectedCategory;

            return matchesCategory && food.available;
        });

        if (filteredFoods.length === 0) {
            foodGrid.innerHTML = `
                <p class="loading-message">
                    No available food items in this category.
                </p>
            `;
            return;
        }

        foodGrid.innerHTML = filteredFoods.map(food => `
            <article class="food-card" data-food-id="${food.id}">
                <div class="food-image-container">
    ${
        getFoodImage(food)
            ? `<img
                    src="${getFoodImage(food)}"
                    alt="${escapeHTML(food.name)}"
                    class="food-image"
                    loading="lazy"
                >`
            : `<span class="food-image-fallback">
                    ${getFoodEmoji(food)}
               </span>`
    }
</div>

                <div class="food-info">
                    <h3>${escapeHTML(food.name)}</h3>

                    <p class="food-price">
                        ${formatCurrency(food.price)}
                    </p>

                    <button
                        class="food-add"
                        data-food-id="${food.id}"
                        type="button">
                        + ADD TO ORDER
                    </button>
                </div>
            </article>
        `).join("");
    }

    // ==========================================
    // CATEGORY FILTERING
    // ==========================================

    document.getElementById("categoryList")
        .addEventListener("click", event => {

            const button = event.target.closest(".category-btn");

            if (!button) return;

            document.querySelectorAll(".category-btn")
                .forEach(item => item.classList.remove("active"));

            button.classList.add("active");

            selectedCategory = button.dataset.category;

            displayFoodItems();
        });

    // ==========================================
    // ADD FOOD TO CART
    // ==========================================

    foodGrid.addEventListener("click", event => {

        const button = event.target.closest(".food-add");
        const card = event.target.closest(".food-card");

        if (!button && !card) return;

        const foodId = Number(
            (button || card).dataset.foodId
        );

        addToCart(foodId);
    });

    function addToCart(foodId) {
        const food = foodItems.find(item =>
            Number(item.id) === foodId
        );

        if (!food || !food.available) return;

        const existingItem = cart.find(item =>
            Number(item.food.id) === foodId
        );

        if (existingItem) {
            existingItem.quantity++;
        } else {
            cart.push({
                food: food,
                quantity: 1
            });
        }

        displayCart();
    }

    // ==========================================
    // CART FUNCTIONS
    // ==========================================

    function calculateTotal() {
        return cart.reduce((total, item) => {
            return total +
                Number(item.food.price) * item.quantity;
        }, 0);
    }

    function calculateCartCount() {
        return cart.reduce((total, item) => {
            return total + item.quantity;
        }, 0);
    }

    function displayCart() {
        const total = calculateTotal();
        const count = calculateCartCount();

        cartCount.textContent = count;
        cartTotal.textContent = formatCurrency(total);

        checkoutBtn.disabled = cart.length === 0;

        if (cart.length === 0) {
            cartItems.innerHTML = `
                <div class="empty-cart">
                    <span>🛒</span>
                    <h3>Your cart is empty</h3>
                    <p>Add something delicious!</p>
                </div>
            `;
            return;
        }

        cartItems.innerHTML = cart.map(item => `
            <div class="cart-item">
                <div class="cart-item-info">
                    <h4>${escapeHTML(item.food.name)}</h4>

                    <p>
                        ${formatCurrency(
                            item.food.price * item.quantity
                        )}
                    </p>

                    <div class="quantity-controls">
                        <button
                            type="button"
                            data-action="decrease"
                            data-food-id="${item.food.id}">
                            −
                        </button>

                        <strong>${item.quantity}</strong>

                        <button
                            type="button"
                            data-action="increase"
                            data-food-id="${item.food.id}">
                            +
                        </button>

                        <button
                            type="button"
                            data-action="remove"
                            data-food-id="${item.food.id}">
                            ✕
                        </button>
                    </div>
                </div>
            </div>
        `).join("");
    }

    cartItems.addEventListener("click", event => {

        const button = event.target.closest("button");

        if (!button) return;

        const foodId = Number(button.dataset.foodId);
        const action = button.dataset.action;

        const item = cart.find(item =>
            Number(item.food.id) === foodId
        );

        if (!item) return;

        if (action === "increase") {
            item.quantity++;
        }

        if (action === "decrease") {
            item.quantity--;

            if (item.quantity <= 0) {
                cart = cart.filter(item =>
                    Number(item.food.id) !== foodId
                );
            }
        }

        if (action === "remove") {
            cart = cart.filter(item =>
                Number(item.food.id) !== foodId
            );
        }

        displayCart();
    });

    document.getElementById("clearCartBtn")
        .addEventListener("click", () => {
            cart = [];
            displayCart();
        });

    // ==========================================
    // WELCOME AND DINING NAVIGATION
    // ==========================================

    document.getElementById("beginBtn")
        .addEventListener("click", () => {
            showScreen("diningScreen");
        });

    document.querySelectorAll(".dining-card")
        .forEach(button => {

            button.addEventListener("click", () => {

                diningOption = button.dataset.option;

                document.getElementById("diningBadge")
                    .textContent = diningOption.toUpperCase();

                showScreen("menuScreen");
            });
        });

    document.getElementById("backToDiningBtn")
        .addEventListener("click", () => {
            showScreen("diningScreen");
        });

    // ==========================================
    // CHECKOUT
    // ==========================================

    checkoutBtn.addEventListener("click", () => {

        if (cart.length === 0) return;

        displayCheckoutSummary();

        showScreen("checkoutScreen");
    });

    function displayCheckoutSummary() {

        checkoutItems.innerHTML = cart.map(item => `
            <div class="summary-item">
                <span>
                    ${escapeHTML(item.food.name)}
                    × ${item.quantity}
                </span>

                <strong>
                    ${formatCurrency(
                        item.food.price * item.quantity
                    )}
                </strong>
            </div>
        `).join("");

        checkoutTotal.textContent =
            formatCurrency(calculateTotal());
    }

    document.getElementById("backToMenuBtn")
        .addEventListener("click", () => {
            showScreen("menuScreen");
        });

    // ==========================================
    // CREATE ORDER THROUGH JAVA API
    // ==========================================

    customerForm.addEventListener("submit", async event => {

        event.preventDefault();

        const submitButton = customerForm.querySelector(
            'button[type="submit"]'
        );

        if (cart.length === 0) return;

        customerDetails = {
            name: document.getElementById("customerName")
                .value.trim(),

            contact: document.getElementById("contactInfo")
                .value.trim()
        };

        if (!customerDetails.name || !customerDetails.contact) {
            alert("Please complete your customer information.");
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "CREATING ORDER...";

        try {

            const formData = new URLSearchParams();

            formData.append("customerName", customerDetails.name);
            formData.append("contactInfo", customerDetails.contact);
            formData.append("diningOption", diningOption);

            cart.forEach(item => {
                formData.append(
                    "item",
                    `${item.food.id}:${item.quantity}`
                );
            });

            const response = await fetch(`${API_URL}/orders`, {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },
                body: formData
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(
                    result.message || result.error ||
                    "Unable to create order."
                );
            }

            currentOrder = result;

            paymentMessage.textContent = "";
            ewalletPanel.classList.add("hidden");

            showScreen("paymentScreen");

        } catch (error) {

            console.error("Order creation error:", error);

            alert(error.message);

        } finally {

            submitButton.disabled = false;
            submitButton.textContent = "CONTINUE TO PAYMENT";
        }
    });

    // ==========================================
    // PAYMENT PROCESSING
    // ==========================================

    document.querySelectorAll(".payment-card")
        .forEach(button => {

            button.addEventListener("click", async () => {

                const method = button.dataset.payment;

                if (!currentOrder || paymentInProgress) return;

                if (method === "cash") {
                    await processPayment("cash");
                }

                if (method === "ewallet") {
                    paymentMessage.textContent =
                        "Demo mode: No real payment will be made.";

                    ewalletPanel.classList.remove("hidden");
                }
            });
        });

    document.getElementById("confirmEwalletBtn")
        .addEventListener("click", async () => {
            await processPayment("ewallet");
        });

    async function processPayment(method) {

        if (!currentOrder || paymentInProgress) return;

        paymentInProgress = true;

        const cashButton = document.querySelector(
            '[data-payment="cash"]'
        );

        const ewalletButton = document.getElementById(
            "confirmEwalletBtn"
        );

        cashButton.disabled = true;
        ewalletButton.disabled = true;

        paymentMessage.textContent = "Processing payment...";

        try {

            const formData = new URLSearchParams();

            formData.append("paymentMethod", method);

            if (method === "ewallet") {
                formData.append("walletName", "GCash Demo");
            }

            const response = await fetch(
                `${API_URL}/orders/${currentOrder.id}/payment`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },
                    body: formData
                }
            );

            const result = await response.json();

            if (!response.ok) {
                throw new Error(
                    result.message || result.error ||
                    "Payment could not be processed."
                );
            }

            currentOrder = result;

            displayReceipt();
            showScreen("confirmationScreen");

        } catch (error) {

            console.error("Payment error:", error);

            paymentMessage.textContent = error.message;

            alert(error.message);

        } finally {

            paymentInProgress = false;

            cashButton.disabled = false;
            ewalletButton.disabled = false;
        }
    }

    document.getElementById("backToCheckoutBtn")
        .addEventListener("click", () => {
            showScreen("checkoutScreen");
        });

    // ==========================================
    // ORDER CONFIRMATION
    // ==========================================

    function displayReceipt() {

        document.getElementById("receiptOrderId")
            .textContent = currentOrder.id;

        document.getElementById("receiptDining")
            .textContent = currentOrder.diningOption;

        document.getElementById("receiptTotal")
            .textContent = formatCurrency(currentOrder.total);

        document.getElementById("receiptPayment")
            .textContent = currentOrder.paymentMethod;

        document.getElementById("receiptItems").innerHTML =
            currentOrder.items.map(item => `
                <div class="receipt-item">
                    <span>
                        ${escapeHTML(item.name)}
                        × ${item.quantity}
                    </span>

                    <strong>
                        ${formatCurrency(item.subtotal)}
                    </strong>
                </div>
            `).join("");
    }

    // ==========================================
    // DOWNLOAD PDF RECEIPT
    // ==========================================

    function createPDF(lines) {

        function cleanText(text) {
            return String(text)
                .replace(/₱/g, "PHP ")
                .replace(/[^\x20-\x7E]/g, "");
        }

        function escapePDF(text) {
            return cleanText(text)
                .replace(/\\/g, "\\\\")
                .replace(/\(/g, "\\(")
                .replace(/\)/g, "\\)");
        }

        let content = "BT\n/F1 12 Tf\n50 790 Td\n";

        lines.forEach((line, index) => {

            if (index > 0) {
                content += "0 -20 Td\n";
            }

            content += `(${escapePDF(line)}) Tj\n`;
        });

        content += "ET";

        const objects = [
            "<< /Type /Catalog /Pages 2 0 R >>",

            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",

            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",

            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",

            `<< /Length ${new TextEncoder().encode(content).length} >>\nstream\n${content}\nendstream`
        ];

        let pdf = "%PDF-1.4\n";
        const offsets = [0];

        objects.forEach((object, index) => {

            offsets.push(
                new TextEncoder().encode(pdf).length
            );

            pdf += `${index + 1} 0 obj\n${object}\nendobj\n`;
        });

        const xrefOffset =
            new TextEncoder().encode(pdf).length;

        pdf += `xref\n0 ${objects.length + 1}\n`;
        pdf += "0000000000 65535 f \n";

        offsets.slice(1).forEach(offset => {
            pdf += `${String(offset).padStart(10, "0")} 00000 n \n`;
        });

        pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\n`;
        pdf += `startxref\n${xrefOffset}\n%%EOF`;

        return new Blob([pdf], {
            type: "application/pdf"
        });
    }

    
document.getElementById("downloadReceiptBtn")
    .addEventListener("click", () => {

        if (!currentOrder) return;

        const downloadBtn = document.getElementById("downloadReceiptBtn");

        if (downloadBtn.classList.contains("is-downloading")) return;

        downloadBtn.disabled = true;
        downloadBtn.classList.remove("is-complete");
        downloadBtn.classList.add("is-downloading");

        const lines = [
            "DINEFLOW",
            "SELF-SERVICE FOOD ORDERING",
            "--------------------------------",
            `Order Number: ${currentOrder.id}`,
            `Customer: ${customerDetails.name}`,
            `Contact: ${customerDetails.contact}`,
            `Dining: ${currentOrder.diningOption}`,
            "--------------------------------",
            "ORDER ITEMS"
        ];

        currentOrder.items.forEach(item => {

            lines.push(
                `${item.name} x ${item.quantity}`
            );

            lines.push(
                `Subtotal: ${formatCurrency(item.subtotal)}`
            );
        });

        lines.push("--------------------------------");
        lines.push(`TOTAL: ${formatCurrency(currentOrder.total)}`);
        lines.push(`Payment: ${currentOrder.paymentMethod}`);
        lines.push(`Status: ${currentOrder.paymentStatus}`);
        lines.push("--------------------------------");
        lines.push("Thank you for ordering!");
        lines.push("Please come again.");

        try {

            const pdfBlob = createPDF(lines);
            const downloadURL = URL.createObjectURL(pdfBlob);

            const link = document.createElement("a");

            link.href = downloadURL;
            link.download = `DineFlow-Receipt-${currentOrder.id}.pdf`;

            document.body.appendChild(link);
            link.click();
            link.remove();

            setTimeout(() => {
                URL.revokeObjectURL(downloadURL);
            }, 1000);

            downloadBtn.classList.remove("is-downloading");
            downloadBtn.classList.add("is-complete");

            downloadBtn.disabled = false;

        } catch (error) {

            console.error("Receipt download failed:", error);

            downloadBtn.classList.remove("is-downloading");
            downloadBtn.disabled = false;

            alert("Unable to download receipt. Please try again.");
        }
    });

    // ==========================================
    // START NEW ORDER
    // ==========================================

    document.getElementById("newOrderBtn")
        .addEventListener("click", () => {

            cart = [];
            selectedCategory = "All";
            diningOption = "Dine In";
            customerDetails = {};
            currentOrder = null;

            customerForm.reset();

            document.querySelectorAll(".category-btn")
                .forEach(button => {
                    button.classList.toggle(
                        "active",
                        button.dataset.category === "All"
                    );
                });

            displayCart();

            paymentMessage.textContent = "";
            ewalletPanel.classList.add("hidden");

            showScreen("welcomeScreen");
        });

    // ==========================================
    // INITIALIZE KIOSK
    // ==========================================

    displayCart();
    loadFoodItems();

});
