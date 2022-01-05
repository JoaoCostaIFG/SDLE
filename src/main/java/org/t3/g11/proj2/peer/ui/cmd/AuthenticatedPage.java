package org.t3.g11.proj2.peer.ui.cmd;

import org.t3.g11.proj2.TableFormatter;
import org.t3.g11.proj2.peer.Peer;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class AuthenticatedPage implements CmdPage {

    final private CmdInterface cmdInterface;

    public AuthenticatedPage(CmdInterface cmdInterface) {
        this.cmdInterface = cmdInterface;
    }

    @Override
    public void show() {
        Scanner sc = this.cmdInterface.getScanner();
        Peer peer = cmdInterface.getPeer();

        System.out.print("""
                n - New post
                s - List stored posts
                q - quit
                """);
        System.out.flush();

        char cmd = this.getCmd(sc);


        switch (cmd) {
            case 'n', 'N' -> {
                System.out.print("Tweet content: ");
                System.out.flush();
                String content = sc.nextLine();
                if (!peer.newPost(content))
                    System.out.println("Failed to create post.");
                else
                    System.out.println("Post created.");
            }
            case 's', 'S' -> {
                List<HashMap<String, String>> selfPosts = peer.getSelfPeerPosts();
                if(selfPosts == null || selfPosts.isEmpty()) {
                    System.out.println("No Posts To Show");
                    return;
                }

                TableFormatter tf = new TableFormatter();
                tf.printHeader();

                for(HashMap<String, String> post : selfPosts){
                    tf.printPostRow(post.get("author"), post.get("content"), post.get("timestamp"));
                }
            }
            case 'q', 'Q' -> cmdInterface.exit();
            default -> System.err.println("Unknown command...");
        }
    }
}
