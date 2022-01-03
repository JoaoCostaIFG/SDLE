public class Proj2 {
  public static void main(String[] args) {
    if(args.length < 3) {
      System.out.println("Usage: Proj2 <id> <dealerAdd> <routerAdd>");
      return;
    }

    GnuNode node = new GnuNode(args[0], args[1], args[2]);

    try {
      if (args[0].equals("1")) node.send("Hello");
      else node.receive();
    } catch(Exception e){
      e.printStackTrace();
    }
  }
}
