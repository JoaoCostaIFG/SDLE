package org.t3.g11.proj2.peer.ui.cmd;

import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class GuestPage implements CmdPage {

    private final CmdInterface cmdInterface;

    public GuestPage(CmdInterface cmdInterface) {
        this.cmdInterface = cmdInterface;
    }

    @Override
    public void show() {
        Scanner sc = this.cmdInterface.getScanner();

        System.out.print("""
                r - Register
                l - Login
                q - quit
                """);
        System.out.flush();

        char cmd = this.getCmd(sc);

        // TODO this username is random each time!!!!!!!
        switch (cmd) {
            case 'r', 'R' -> {
                System.out.println("Username:");
                String username = sc.nextLine();
//                byte[] buf = new byte[12];
//                new Random().nextBytes(buf);
//                String username = new String(Base64.getEncoder().encode(buf)).replaceAll("/", "");
                if (!this.cmdInterface.getPeer().register(username)) {
                    System.out.println("Register failed");
                } else {
                    System.out.println("Authenticated as " + username);
                    this.cmdInterface.pushPage(new AuthenticatedPage(this.cmdInterface));
                }
            }
            case 'l', 'L' -> {
                String username = sc.nextLine();
                if (!this.cmdInterface.getPeer().authenticate(username)) {
                    System.out.println("Login failed");
                } else {
                    System.out.println("Logged in as " + username);
                    this.cmdInterface.pushPage(new AuthenticatedPage(this.cmdInterface));
                }
            }
            case 'q', 'Q' -> this.cmdInterface.exit();
            default -> System.err.println("Unknown command...");
        }
    }
}
