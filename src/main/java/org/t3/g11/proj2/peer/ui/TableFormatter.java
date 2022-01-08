package org.t3.g11.proj2.peer.ui;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class TableFormatter {
    public void printHeader() {
        String headerLine = String.format("|%-16s|%-50s|%-20s|", "Username", "Content", "Publish Date");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < headerLine.length(); i++) builder.append("_");
        builder.append("\n").append(headerLine).append("\n");
        for (int i = 0; i < headerLine.length(); i++) builder.append("=");
        builder.append("\n");

        System.out.print(builder);
    }

    public void printPostRow(String username, String content, String date) {
        boolean firstLine = true;
        String[] strings = content.split("(?<=\\G.{50})"); // splits content string into multiple of size 30
        for (String s : strings) {
            String usrname = "";
            String dt = "";
            if (firstLine) {
                usrname = username;
                Date d = new Date(Long.parseLong(date));
                DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                dt = format.format(d);
            }
            System.out.format("|%-16s|%-50s|%-20s|\n", usrname, s, dt);
            firstLine = false;
        }
        for (int i = 0; i < 90; i++) System.out.print("-");
        System.out.print("\n");
    }
}
