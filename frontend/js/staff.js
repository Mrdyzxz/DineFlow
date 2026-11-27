document.addEventListener("DOMContentLoaded", () => {
    const API_URL = "https://dineflow-api-g1a3.onrender.com/api";
    const loginView = document.getElementById("staffLogin");
    const dashboard = document.getElementById("staffDashboard");
    const loginForm = document.getElementById("staffLoginForm");
    const usernameInput = document.getElementById("staffUsername");
    const passwordInput = document.getElementById("staffPassword");
    const loginButton = document.getElementById("staffLoginButton");
    const loginMessage = document.getElementById("staffLoginMessage");
    const orderList = document.getElementById("orderList");
    const orderMessage = document.getElementById("orderMessage");
    const lastUpdated = document.getElementById("lastUpdated");
    const refreshButton = document.getElementById("refreshOrders");
    const logoutButton = document.getElementById("staffLogout");
    const nextStatuses = {
        Pending: "Processing",
        Processing: "Ready",
        Ready: "Completed"
    };
    let staffToken = null;

    function escapeHTML(value) {
        return String(value).replace(/[&<>"']/g, character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#039;"
        })[character]);
    }

    function formatCurrency(amount) {
        return Number(amount).toLocaleString("en-PH", {
            style: "currency",
            currency: "PHP"
        });
    }

    function showLogin(message, isError = false) {
        staffToken = null;
        loginView.hidden = false;
        dashboard.hidden = true;
        refreshButton.hidden = true;
        logoutButton.hidden = true;
        orderList.innerHTML = "";
        lastUpdated.textContent = "";
        loginMessage.textContent = message || "";
        loginMessage.hidden = !message;
        loginMessage.classList.toggle("error", isError);
    }

    function showDashboard() {
        loginView.hidden = true;
        dashboard.hidden = false;
        refreshButton.hidden = false;
        logoutButton.hidden = false;
    }

    function handleUnauthorized(response) {
        if (response.status !== 401 && response.status !== 403) {
            return false;
        }

        showLogin(
            "Your staff session has expired. Please sign in again.",
            true
        );
        return true;
    }

    function staffFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        headers.set("Authorization", `Bearer ${staffToken}`);
        return fetch(url, { ...options, headers });
    }

    async function readResponse(response) {
        try {
            return await response.json();
        } catch (error) {
            throw new Error("The server returned an invalid response.");
        }
    }

    async function loadOrders() {
        if (!staffToken) return;

        refreshButton.disabled = true;
        orderMessage.classList.remove("error");

        try {
            const response = await staffFetch(`${API_URL}/orders`);
            if (handleUnauthorized(response)) return;

            const orders = await readResponse(response);
            if (!response.ok) {
                throw new Error(orders.error || "Unable to load orders.");
            }
            if (!Array.isArray(orders)) {
                throw new Error("The server returned an invalid order list.");
            }

            renderOrders(orders);
            lastUpdated.textContent = `Updated ${new Date().toLocaleTimeString()}`;
            orderMessage.hidden = orders.length > 0;
            orderMessage.textContent = orders.length > 0
                ? ""
                : "No customer orders yet.";
        } catch (error) {
            orderList.innerHTML = "";
            orderMessage.hidden = false;
            orderMessage.classList.add("error");
            orderMessage.textContent =
                `${error.message} Make sure the Java API is running.`;
        } finally {
            refreshButton.disabled = false;
        }
    }

    function renderOrders(orders) {
        orderList.innerHTML = orders.map(order => {
            const status = order.orderStatus || order.status;
            const nextStatus = nextStatuses[status];
            const items = order.items.map(item => `
                <li>
                    <span>${escapeHTML(item.name)} × ${item.quantity}</span>
                    <strong>${formatCurrency(item.subtotal)}</strong>
                </li>
            `).join("");

            return `
                <article class="staff-order">
                    <div class="staff-order-header">
                        <h2>Order #${order.id}</h2>
                        <span class="staff-status">${escapeHTML(status)}</span>
                    </div>
                    <div class="staff-order-meta">
                        <span>${escapeHTML(order.customer)}</span>
                        <span>${escapeHTML(order.diningOption)}</span>
                    </div>
                    <ul class="staff-order-items">${items}</ul>
                    <p class="staff-payment">
                        Payment: <strong>${escapeHTML(order.paymentStatus)}</strong>
                        <span> · ${escapeHTML(order.paymentMethod)}</span>
                    </p>
                    <div class="staff-order-total">
                        <span>Total</span>
                        <strong>${formatCurrency(order.total)}</strong>
                    </div>
                    ${nextStatus ? `
                        <button
                            class="primary-btn staff-order-action"
                            type="button"
                            data-order-id="${order.id}"
                            data-next-status="${nextStatus}">
                            ${nextStatus === "Processing"
                                ? "Start processing"
                                : `Mark ${nextStatus}`}
                        </button>
                    ` : ""}
                </article>
            `;
        }).join("");
    }

    loginForm.addEventListener("submit", async event => {
        event.preventDefault();
        loginButton.disabled = true;
        loginMessage.classList.remove("error");
        loginMessage.textContent = "Signing in...";
        loginMessage.hidden = false;

        const credentials = new URLSearchParams();
        credentials.set("username", usernameInput.value.trim());
        credentials.set("password", passwordInput.value);

        try {
            const response = await fetch(`${API_URL}/staff/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: credentials
            });
            const result = await readResponse(response);

            if (!response.ok) {
                throw new Error(result.error || "Unable to sign in.");
            }
            if (typeof result.token !== "string" || !result.token) {
                throw new Error("The server returned an invalid login response.");
            }

            staffToken = result.token;
            passwordInput.value = "";
            loginMessage.textContent = "";
            loginMessage.hidden = true;
            showDashboard();
            await loadOrders();
        } catch (error) {
            passwordInput.value = "";
            loginMessage.classList.add("error");
            loginMessage.textContent =
                `${error.message} Make sure the Java API is running.`;
            loginMessage.hidden = false;
        } finally {
            loginButton.disabled = false;
        }
    });

    orderList.addEventListener("click", async event => {
        const button = event.target.closest("[data-order-id]");
        if (!button || !staffToken) return;

        button.disabled = true;
        orderMessage.classList.remove("error");
        const formData = new URLSearchParams();
        formData.append("status", button.dataset.nextStatus);

        try {
            const response = await staffFetch(
                `${API_URL}/orders/${button.dataset.orderId}/status`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: formData
                }
            );
            if (handleUnauthorized(response)) return;

            const result = await readResponse(response);
            if (!response.ok) {
                throw new Error(result.error || "Unable to update order.");
            }

            await loadOrders();
        } catch (error) {
            orderMessage.hidden = false;
            orderMessage.classList.add("error");
            orderMessage.textContent = error.message;
            button.disabled = false;
        }
    });

    refreshButton.addEventListener("click", loadOrders);

    logoutButton.addEventListener("click", async () => {
        let message = "You have signed out.";
        let isError = false;

        try {
            const response = await staffFetch(`${API_URL}/staff/logout`, {
                method: "POST"
            });
            if (!response.ok && response.status !== 401) {
                const result = await readResponse(response);
                throw new Error(result.error || "The server could not end the session.");
            }
        } catch (error) {
            message = `${error.message} The token was cleared from this page.`;
            isError = true;
        } finally {
            showLogin(message, isError);
        }
    });
});
