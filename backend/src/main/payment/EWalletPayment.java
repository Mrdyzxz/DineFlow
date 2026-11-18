package payment;

public class EWalletPayment extends Payment {

    private String walletName;

    public EWalletPayment(int paymentId, double amount, String walletName) {
        super(paymentId, amount);
        setWalletName(walletName);
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        if (walletName == null || walletName.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet name is required.");
        }
        this.walletName = walletName;
    }

    @Override
    public void processPayment() {
        System.out.println("\n===== E-WALLET PAYMENT =====");
        System.out.println("Payment ID: " + getPaymentId());
        System.out.println("Wallet: " + walletName);
        System.out.println("Amount: ₱" + getAmount());
        System.out.println("Payment successful!");
    }
}
