package org.t3.g11.proj2.peer.ui.cmd;

import java.util.Scanner;

public interface CmdPage {
    void show();

    default char getCmd(Scanner sc) {
        String nextLine;

        do {
            nextLine = sc.nextLine().strip();
        } while(nextLine.isEmpty());

        return nextLine.charAt(0);
    }
}
