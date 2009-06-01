package gov.nasa.jpf.test;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Random;

import gov.nasa.jpf.Ensures;
import gov.nasa.jpf.Local;
import gov.nasa.jpf.Predicate;
import gov.nasa.jpf.Requires;
import gov.nasa.jpf.Invariant;
import gov.nasa.jpf.jvm.Verify;


class TestContractsBase {

  @Requires("a > 0")
  int foo (int a){
    return a;
  }

  @Ensures("Result < 0")
  int baz (int a){
    return a;
  }
}

@Invariant({"d within 40 +- 5",
            "a > 0"})
public class TestContracts extends TestContractsBase {

  static String w = "WHAT?";
  double d = 42.1;
  int a = 42;

  @Requires("a > 0 && a < 50")
  int foo (int a){
    return 42/a;
  }

  @Requires("a within 10,20")
  @Ensures("old(d) >= d")
  int baz (int a){
    d += a;
    return a + 10;
  }

  @Requires("d <= e")
  static void bar (double d, String x, double e){
    // nothing
  }

  // only here for the invariant
  void faz () {
    d += 100;
  }

  @Ensures("Result != null")
  Object fizz() {
    return null;
  }

//example of model-space predicate
  @Local
  static class IsValidDate implements Predicate {
    // should be side effect free
    public String evaluate (Object testObj, Object[] args) {

      Date test=null, begin=null, end=null;
      boolean result = false;

      if (testObj != null) {
        test = (Date)testObj;

        if (args.length == 2 && args[0] != null && args[1] != null) {
          try {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
            begin = df.parse(args[0].toString());
            end = df.parse(args[1].toString());

            if ((test.compareTo(begin) < 0) || (test.compareTo(end) > 0)){
              return "IsValidDate " + test + " outside " + begin + " and " + end;
            }

          } catch (ParseException px) {
            return "IsValidDate argument did not parse: " + px.getMessage();
          }
        }
      }
      return null;
    }
  }


  // example of model predicate
  @Ensures("Result satisfies IsValidDate('01/01/1999', '12/31/2010')")
  Date computeDate (int year, int month, int day) {
    Date d = new Date(year,month,day);
    return d;
  }


  // example of native predicate
  @Ensures("Result satisfies gov.nasa.jpf.test.predicate.IsMonotonicDecreasing")
  int computeSomething (int i, int j) {
    if (i * (5-j) > 10) {
      return 10;
    } else {
      return j;
    }
  }

  static {
    Verify.setProperties("cg.enumerate_random=true");
  }
  public static void main (String[] args){
    TestContracts t = new TestContracts();
    //t.foo(123);   // super precond holds, so it's Ok
    //t.baz(15);
    //bar(42.0, "BARR", 41.0);

    //t.computeDate(98, 10, 10);

    t.faz(); // invariant violation

    Random random = new Random(42);
    int n = random.nextInt(4); // note we have to run this with cg.enumerate_random=true to catch the error

    System.out.println("--- round: " + n);
    for (int i=5; i>=0; i--) {
      int m = t.computeSomething(n, i);
      System.out.println("  " + m);
    }


    //t.fizz();
  }

}
