package org.t3.g11.proj2;

public class Proj2 {
  public static void main(String[] args) {
    if(args.length < 3) {
      System.out.println("Usage: org.t3.g11.proj2.Proj2 <id> <dealerAdd> <routerAdd>");
      return;
    }

    GnuNode node = new GnuNode(args[0], args[1], args[2]);

    node.receive();
  }
}
