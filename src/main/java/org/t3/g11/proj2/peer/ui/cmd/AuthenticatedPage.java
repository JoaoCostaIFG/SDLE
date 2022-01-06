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
                f - Follow someone
                l - Lookup content
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
                if (selfPosts == null || selfPosts.isEmpty()) {
                    System.out.println("No Posts To Show");
                    return;
                }

                TableFormatter tf = new TableFormatter();
                tf.printHeader();

                for (HashMap<String, String> post : selfPosts) {
                    tf.printPostRow(post.get("author"), post.get("content"), post.get("timestamp"));
                }
            }
            case 'f', 'F' -> {
                System.out.print("User to follow: ");
                System.out.flush();
                String content = sc.nextLine();

                if (!peer.subscribe(content))
                    System.out.println("Already subscribed.");
                else
                    System.out.println("Subscribed successfully.");
            }
            case 'l', 'L' -> {
                System.out.print("User to show: ");
                System.out.flush();
                String username = sc.nextLine();

                List<HashMap<String, String>> posts = peer.getUserPosts(username);
                if (posts == null || posts.isEmpty()) {
                    System.out.println("No Posts To Show");
                    return;
                }

                TableFormatter tf = new TableFormatter();
                tf.printHeader();

                for (HashMap<String, String> post : posts) {
                    tf.printPostRow(post.get("author"), post.get("content"), post.get("timestamp"));
                }
            }
            case 'q', 'Q' -> cmdInterface.exit();
            default -> System.err.println("Unknown command...");
        }
    }
}
