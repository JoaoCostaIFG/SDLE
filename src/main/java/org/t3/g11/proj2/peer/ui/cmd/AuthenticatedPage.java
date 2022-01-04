package org.t3.g11.proj2.peer.ui.cmd;

import java.util.Scanner;

public class AuthenticatedPage implements CmdPage {

    final private CmdInterface cmdInterface;

    public AuthenticatedPage(CmdInterface cmdInterface) {
        this.cmdInterface = cmdInterface;
    }

    @Override
    public void show() {
        Scanner sc = this.cmdInterface.getScanner();

        System.out.print("""
                n - New post
                q - quit
                """);
        System.out.flush();

        char cmd = this.getCmd(sc);


        switch (cmd) {
            case 'n', 'N' -> {
                System.out.print("Tweet content: ");
                System.out.flush();
                String content = sc.nextLine();
                if (!cmdInterface.getPeer().newPost(content))
                    System.out.println("Failed to create post.");
                else
                    System.out.println("Post created.");
            }
            case 'q', 'Q' -> cmdInterface.exit();
            default -> System.err.println("Unknown command...");
        }
    }
}
