package org.t3.g11.proj2.peer.ui.cmd;

import org.t3.g11.proj2.peer.ui.TableFormatter;
import org.t3.g11.proj2.peer.Peer;

import java.nio.charset.StandardCharsets;
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
                p - List stored posts
                f - Follow someone
                u - Unfollow someone
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

                if (content.getBytes(StandardCharsets.UTF_8).length > 500) {
                    System.out.println("Content over character limit (500).");
                    break;
                }

                if (!peer.newPost(content))
                    System.out.println("Failed to create post.");
                else
                    System.out.println("Post created.");
            }
            case 'p', 'P' -> {
                try {
                    List<HashMap<String, String>> posts = peer.getPosts();
                    if (posts == null || posts.isEmpty()) {
                        System.out.println("No Posts To Show");
                        return;
                    }

                    TableFormatter tf = new TableFormatter();
                    tf.printHeader();

                    for (HashMap<String, String> post : posts) {
                        tf.printPostRow(post.get("author"), post.get("content"), post.get("timestamp"));
                    }
                } catch (Exception e) {
                    System.err.println("Error loading posts");
                }
            }
            case 'f', 'F' -> {
                System.out.print("User to follow: ");
                System.out.flush();
                String content = sc.nextLine();

                try {
                    peer.subscribe(content);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            case 'u', 'U' -> {
                System.out.print("User to unfollow: ");
                System.out.flush();
                String content = sc.nextLine();

                try {
                    peer.unsubscribe(content);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
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
