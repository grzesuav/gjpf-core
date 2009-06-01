package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.RawTest;
import gov.nasa.jpf.jvm.abstraction.filter.FilterField;
import gov.nasa.jpf.jvm.untracked.UntrackedField;
import java.util.LinkedList;

import gov.nasa.jpf.jvm.Verify;

/**
 * Tests for the UntrackedField annotation.
 *
 * @author Milos Gligoric (milos.gligoric@gmail.com)
 * @author Tihomir Gvero (tihomir.gvero@gmail.com)
 *
 */
public class TestUntrackedField extends RawTest {

  @UntrackedField
  private static int untracked;
  @UntrackedField
  private static long[] untrackedArray;
  @UntrackedField
  private LinkedList<Integer> untrackedList;
  private LinkedList<Integer> trackedList;

  public static void main (String[] args) {
    TestUntrackedField t = new TestUntrackedField();

    if (!runSelectedTest(args, t)) {
      runAllTests(args, t);
    }
  }

  public void testPrimitiveField () {
    int tracked1 = Verify.getInt(0, 1);
    int tracked2 = Verify.getInt(0, 1);

    untracked++;

    if (tracked1 == 1 && tracked2 == 1) {
      assert (untracked == 4);
    }
  }

  public void testPrimitiveArrayField () {
    untrackedArray = new long[2];
    int tracked1 = Verify.getInt(0, 1);
    int tracked2 = Verify.getInt(0, 1);

    untrackedArray[0]++;
    untrackedArray[1]++;

    if (tracked1 == 1 && tracked2 == 1) {
      assert (untrackedArray[0] == 4 && untrackedArray[1] == 4);
    }
  }

  public void testReferenceField () {
    untrackedList = new LinkedList<Integer>();
    for(int i = 0; i < 10; i++) untrackedList.add(0);

    int tracked1 = Verify.getInt(0, 2);
    int tracked2 = Verify.getInt(0, 2);

    for(int i = 0; i < 10; i++){
      int temp = untrackedList.removeFirst();
      untrackedList.addLast(++temp);
    }

    if (tracked1 == 2 && tracked2 == 2) {
      for(int i = 0; i < 10; i++) assert (untrackedList.getFirst() == 9);
    }
  }

  //FilterSerializer must be switched on
  public void testUntrackedAndFilterFields () {
    class A {
      int tracked;
      @FilterField
      int trackedFiltered;
      @UntrackedField
      @FilterField
      int untrackedFiltered;
    }

    A a = new A();

    a.tracked = Verify.getInt(0, 2);
    a.trackedFiltered = Verify.getInt(0, 2);
    a.untrackedFiltered++;
    Verify.getBoolean();

    // Three cases:
    // [a.tracked == 0, a.trackedFiltered == 0, a.untrackedField == 1]
    // [a.tracked == 1, a.trackedFiltered == 0, a.untrackedField == 4]
    // [a.tracked == 2, a.trackedFiltered == 0, a.untrackedField == 7]
    assert ((a.tracked * 3 + 1) == a.untrackedFiltered &&
            a.trackedFiltered == 0);
  }

  public void testCounters () {
    class C {
      @UntrackedField
      int totalCalls;
      @UntrackedField
      int trueCalls;
      public boolean repOK () {
        int x = Verify.getInt(0, 1);
        int y = Verify.getInt(0, 1);
        return (x == 1) && (y == 1);
      }
    }
    C c = new C();
    boolean b = c.repOK();
    if (b) c.trueCalls++;
    c.totalCalls++;
    if (b) {
      assert ((c.totalCalls == 4) && (c.trueCalls == 1));
    }
  }

  // lists through which this test goes: [0, 1], [0, 2], [0, 3],
  // [0, 3, 4], [0, 3, 4, 5], [0, 3, 4, 5, 6]
  public void testUntrackedTrackedAliasing () {
    trackedList = new LinkedList<Integer>();
    trackedList.addLast(0);

    int value = Verify.getInt(1, 6);
    trackedList.addLast(value);

    if (value == 3) {
      untrackedList = trackedList;
    }

    if (value == 1 || value == 2 || value == 3)
      assert checkOrder(trackedList, 0, value);
    if (value == 4) assert checkOrder(trackedList, 0, 3, 4);
    if (value == 5) assert checkOrder(trackedList, 0, 3, 4, 5);
    if (value == 6) assert checkOrder(trackedList, 0, 3, 4, 5, 6);
  }
  private boolean checkOrder(LinkedList<Integer> list, int... values) {
    if (list.size() != values.length) return false;
    java.util.Iterator<Integer> l = list.iterator();
    for (int i = 0; i < values.length; i++) {
      if (l.next() != values[i]) return false;
    }
    return true;
  }

  public void testCycle () {
    headUntracked = new Node(1);
    headTracked = new Node(2);

    int choice = Verify.getInt(0, 9);
    switch (choice) {
    case 0:
      headUntracked.val = 0;
      headTracked.val = 0;
      break;
    case 1:
      assert (headUntracked.val == 0 && headTracked.val == 2);
      break;
    case 2:
      headTracked.next = headUntracked;
      break;
    case 3:
      assert (headUntracked.next == null && headTracked.next == null);
      break;
    case 4:
      headUntracked.next = headTracked;
      break;
    case 5:
      assert (headUntracked.next == headTracked && headTracked.next == null);
      break;
    case 6:
      headTracked.next = headUntracked;
      headTracked.val = 0;
      break;
    case 7:
      assert (headUntracked.next == headTracked &&
              headTracked.next == headUntracked &&
              headTracked.val == 0);
      break;
    case 8:
      headUntracked.next = null;
      headUntracked = null;
      break;
    case 9:
      assert (headTracked.next == null && headTracked.val == 2);
      break;
    }
  }

  static class CycleNode {
    int val;
    CycleNode next;
  }
  @UntrackedField
  CycleNode obj1;
  @UntrackedField
  CycleNode obj2;
  @UntrackedField
  CycleNode obj3;
  public void testUntrackedCycle () {
    obj1 = new CycleNode();
    obj2 = new CycleNode();
    obj3 = new CycleNode();

    int choice = Verify.getInt(1, 4);
    switch (choice) {
    case 1:
      obj1.val = choice; // [N1]
      obj1.next = obj2;  // [N1] - [N2] -|
      break;
    case 2:
      obj2.val = choice;
      obj2.next = obj3;  // [N1] -> [N2] -> [N3] -|
      break;
    case 3:
      obj3.val = choice; // [N1] -> [N2] -> [N3]
      obj3.next = obj1;  //   \______<______/
      break;
    }

    if (choice == 2)
      assert (obj1.val == 1 && obj1.next == obj2);
    if (choice == 3)
      assert (obj1.val == 1 && obj1.next == obj2 &&
          obj2.val == 2 && obj2.next == obj3);
    if (choice == 4)
      assert (obj1.val == 1 && obj1.next == obj2 &&
          obj2.val == 2 && obj2.next == obj3 &&
          obj3.val == 3 && obj3.next == obj1);
  }

  @UntrackedField
  private static int staticUntracked;
  @UntrackedField
  private int nonstaticUntracked;
  public void testStaticNonStatic () {
    int verify1 = Verify.getInt(3, 4);
    int verify2 = Verify.getInt(1, 2);

    staticUntracked++;
    nonstaticUntracked++;

    if (verify1 == 2 && verify2 == 2) {
      assert (staticUntracked == 4 && nonstaticUntracked == 4);
    }
  }

  public void testInitializedObjectFirst () {
    class C {
      @UntrackedField
      int counter;
    }
    C c1 = new C();
    C c2 = new C();
    int verify = Verify.getInt(0, 3);
    c1.counter++;
    if (verify == 3) assert (c1.counter == 4) : "c1.counter expected 4, is: " +c1.counter;
  }

  public void testInitializedObjects () {
    class C {
      @UntrackedField
      int counter;
    }
    C c1 = new C();
    C c2 = new C();
    c2.counter = 5;  // it does not work if set counter to 0
    int verify = Verify.getInt(0, 3);
    c1.counter++;
    if (verify == 3) assert (c1.counter == 4);
  }

  public void testInitializedObjectLast () {
    class C {
      @UntrackedField
      int counter;
    }
    C c2 = new C();
    C c1 = new C();
    int verify = Verify.getInt(0, 3);
    c1.counter++;
    if (verify == 3) assert (c1.counter == 4);
  }

  public void testInheritedUntrackedFields () {
    class A {
      @UntrackedField
      int counterA;
      @UntrackedField
      String logA = "";
    }
    class B extends A {
      @UntrackedField
      int counterB;
      @UntrackedField
      String logB = "";
    }

    B b = new B();

    int verify = Verify.getInt(1, 4);
    b.counterA++;
    b.counterB--;

    b.logA += b.counterA;
    b.logB += b.counterB;

    if (verify == 4)
      assert (b.counterA == 4 && b.counterB == -4 &&
          b.logA.equals("1234") && b.logB.equals("-1-2-3-4"));
  }

  public void testUsingUntrackedFieldInSameClass () {
    class A {
      @UntrackedField
      LinkedList<Integer> info = new LinkedList<Integer>();

      void add(int i) {
        info.add(i);
      }

      int get(int index) throws IndexOutOfBoundsException {
        return info.get(index);
      }
    }
    A a = new A();

    int verify = Verify.getInt(10, 15);
    a.add(verify);

    if (verify == 15)
      assert (a.get(0) == 10 && a.get(1) == 11 && a.get(2) == 12 &&
          a.get(3) == 13 && a.get(4) == 14 && a.get(5) == 15);
  }

  // TODO: add more tests, e.g., for aliasing between tracked and untracked objects
  @UntrackedField
  private Node headUntracked;
  private Node headTracked;
  static class Node {
    public int val;
    public Node next;

    public Node(int val) {
      this.val = val;
    }

    public void addLast(Node node) {
      Node tmp = this;
      while (tmp.next != null) {
        tmp = tmp.next;
      }
      tmp.next = node;
    }

    public boolean checkOrder(int... values) {
      if (size() != values.length) return false;
      Node tmp = this;
      for (int i = 0; tmp.next != null; i++, tmp = tmp.next) {
        if (tmp.val != values[i]) return false;
      }
      return true;
    }

    public int size() {
      int size = 1;
      Node tmp = this;
      while (tmp.next != null) {
        tmp = tmp.next;
        size++;
      }
      return size;
    }

    public String toString() {
      String s = "N:" + val;
      Node tmp = this;
      while (tmp.next != null) {
        tmp = tmp.next;
        s += " N:" + tmp.val;
      }
      return s;
    }
  }
}
