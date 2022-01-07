package org.t3.g11.proj2;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class TableFormatter {
    public void printHeader() {
        for (int i = 0; i < 90; i++) System.out.print("_");
        System.out.print("\n");
        System.out.format("|%-17s|%-50s|%-19s|\n", "Username", "Content", "Publish Date");
        for (int i = 0; i < 90; i++) System.out.print("=");
        System.out.print("\n");
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
            System.out.format("|%-17s|%-50s|%-19s|\n", usrname, s, dt);
            firstLine = false;
        }
        for (int i = 0; i < 90; i++) System.out.print("_");
        System.out.print("\n");
    }
}
