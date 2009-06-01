import java.util.Random;

public class Rand {
  public static void main (String[] args) {
    System.out.println("computing c = a/(b+a - 2)..");
    Random random = new Random(42);      // (1)

    int a = random.nextInt(2);           // (2)
    System.out.println("a=" + a);

    //... lots of code here

    int b = random.nextInt(3);           // (3)
    System.out.println("  b=" + b + "       ,a=" + a);

    int c = a/(b+a -2);                  // (4)
    System.out.println("=>  c=" + c + "     ,a=" + a + ",b=" +b);         
  }
}
