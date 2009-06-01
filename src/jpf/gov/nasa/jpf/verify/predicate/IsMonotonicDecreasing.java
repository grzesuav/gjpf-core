package gov.nasa.jpf.verify.predicate;

import java.util.ArrayList;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.verify.ContractException;
import gov.nasa.jpf.verify.NativePredicate;

/**
 * example of native predicate for contracts
 *
 * this one checks if test object values are monotonic decreasing
 */
public class IsMonotonicDecreasing implements NativePredicate {

  int    lastStateId; // that was the last previously stored state
  Number lastNumber;

  static class Listener extends ListenerAdapter {
    static Listener singleton;

    ArrayList<IsMonotonicDecreasing> watchList = new ArrayList<IsMonotonicDecreasing>();

    static Listener getSingleton() {
      if (singleton == null) {
        singleton = new Listener();

        // <2do> - that's quirky, how do we register?
        JVM vm = JVM.getVM();
        JPF jpf = vm.getJPF();
        jpf.addListener(singleton);
      }

      return singleton;
    }

    void add (IsMonotonicDecreasing pred) {
      watchList.add(pred);
    }

    public void stateBacktracked (Search search) {
      // if we backtrack past the last state, reset the reference value
      int sid = search.getStateNumber();

      for (IsMonotonicDecreasing pred : watchList) {
        if (pred.lastStateId >= sid) {
          pred.lastNumber = null;
        }
      }
    }
  }

  public IsMonotonicDecreasing() {
    Listener l = Listener.getSingleton();
    l.add(this);
  }

  public String evaluate(Object testObj, Object[] args) {
    // we don't have args

    if (! (testObj instanceof Number)) {
      throw new ContractException("IsMonotonicDecreasing test object not a Number: " + testObj);
    }

    Number num = (Number)testObj;
    JVM vm = JVM.getVM();
    int sid = vm.getStateId();

    if (lastNumber != null) { // could have been reset by the listener upon backtrack
      if (lastNumber.doubleValue() < num.doubleValue()) {
        return ("IsMonotonicDecreasing failed: " + lastNumber + ", " + num);
      }
    }

    lastStateId = sid;
    lastNumber = num;

    return null; // satisfied
  }

}
