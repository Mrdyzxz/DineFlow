package payment;

public class CashPayment extends Payment {

    public CashPayment(int paymentId, double amount) {
        super(paymentId, amount);
    }

    @Override
    public void processPayment() {
        System.out.println("\n===== CASH PAYMENT =====");
        System.out.println("Payment ID: " + getPaymentId());
        System.out.println("Amount: ₱" + getAmount());
        System.out.println("Payment successful!");
    }
}