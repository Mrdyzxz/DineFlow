package app;

import java.io.Console;
import java.util.Arrays;
import service.StaffAuthenticationService;

public final class StaffPasswordHash {

    private StaffPasswordHash() {
    }

    public static void main(String[] args) {
        Console console = System.console();

        if (console == null) {
            throw new IllegalStateException(
                    "Run this utility in an interactive terminal."
            );
        }

        char[] password = console.readPassword("New staff password: ");
        char[] confirmation = null;

        try {
            if (password == null) {
                throw new IllegalStateException(
                        "No password was entered."
                );
            }

            confirmation = console.readPassword("Confirm password: ");
            if (confirmation == null) {
                throw new IllegalStateException(
                        "Password confirmation was not entered."
                );
            }

            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException(
                        "Passwords do not match."
                );
            }

            System.out.println(
                    StaffAuthenticationService.createPasswordHash(password)
            );
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
