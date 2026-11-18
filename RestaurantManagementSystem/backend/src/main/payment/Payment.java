package payment;

public abstract class Payment {

    private int paymentId;
    private double amount;

    public Payment(int paymentId, double amount) {
        if (paymentId <= 0 || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Payment ID and amount must be positive.");
        }
        this.paymentId = paymentId;
        this.amount = amount;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public double getAmount() {
        return amount;
    }

    public abstract void processPayment();
}