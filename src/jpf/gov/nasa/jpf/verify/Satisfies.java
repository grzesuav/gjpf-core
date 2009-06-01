/**
 *
 */
package gov.nasa.jpf.verify;

import gov.nasa.jpf.Predicate;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DirectCallStackFrame;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticArea;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;

import java.util.ArrayList;
import java.util.HashMap;

public class Satisfies extends Contract {

  static HashMap<ClassInfo,ForwardingPredicate> predicates = new HashMap<ClassInfo,ForwardingPredicate>();

  class ForwardingPredicate implements Predicate {

    MethodInfo miPred;
    int        predRef; // the predicate object reference

    ForwardingPredicate (ClassInfo ciPred){
      // ciPred is guaranteed to be a Predicate implementor
      MethodInfo  mi = ciPred.getMethod("evaluate(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/String;", true);
      if (mi != null) {
        miPred = mi.createDirectCallStub("[predicate]");
        predRef = newInstance(ciPred);
      }
    }

    void execClinit (ClassInfo ci) {
      if (!ci.isInitialized()) {
        ClassInfo sci = ci.getSuperClass();

        if (sci != null) {
          execClinit(sci);
        } else {
          ThreadInfo ti = ctx.getThreadInfo();
          StaticArea sa = ti.getVM().getStaticArea();
          sa.addClass(ci, ti);

          MethodInfo clinit = ci.getMethod("<clinit>()V", false);
          if (clinit != null) {
            MethodInfo miStub = clinit.createDirectCallStub("[predicate class init]");
            DirectCallStackFrame frame = new DirectCallStackFrame(miStub);
            ti.executeMethodAtomic(frame);
          }
        }
      }
    }

    void execCtor (ClassInfo ci, int ref) {
      ClassInfo sci = ci.getSuperClass();

      if (sci != null) {
        execCtor(sci, ref);
      } else {
        MethodInfo miCtor = ci.getMethod("init()V", false);
        if (miCtor != null) {
          // <2do> needs to traverse superclasses
          MethodInfo miStub = miCtor.createDirectCallStub("[predicate init]");
          DirectCallStackFrame frame = new DirectCallStackFrame(miStub);
          frame.pushRef(ref);
          ctx.getThreadInfo().executeMethodAtomic(frame);
        }
      }
    }

    /**
     * beware, this only execs clinit and init synchronously. Any blocking
     * and it will die
     */
    public int newInstance (ClassInfo ci) {
      ThreadInfo ti = ctx.getThreadInfo();
      JVM vm = ti.getVM();
      DynamicArea da = vm.getDynamicArea();

      if (!ci.isInitialized()) {
        execClinit(ci);
      }

      int r = da.newObject(ci, ti);
      da.get(r).pinDown(true); // there are no references in the app

      // notice we do ad hoc init here, which is not allowed to do any blocking
      execCtor(ci,r);

      return r;
    }


    int getRef (ThreadInfo ti, Object o) {
      if (o instanceof ElementInfo) {
        return ((ElementInfo)o).getIndex();

      } else {
        // here it gets braindead - we convert JPF into native, to convert back into JPF

        MJIEnv env = ti.getEnv();

        if (o instanceof Integer) {
          return env.newInteger((Integer)o);
        } else if (o instanceof Long) {
          return env.newLong((Long)o);
        } else if (o instanceof Boolean) {
          return env.newBoolean((Boolean)o);
        } else if (o instanceof Double) {
          return env.newDouble((Double)o);
        } else if (o instanceof Float) {
          return env.newFloat((Float)o);

        } else {
          return env.newString(o.toString());
        }
      }
    }

    int getArrayRef (ThreadInfo ti, Object[] args) {
      if (args != null) {
        MJIEnv env = ti.getEnv();
        int ref = env.newObjectArray("Ljava/lang/Object;", args.length);

        for (int i=0; i<args.length; i++) {
          env.setReferenceArrayElement(ref, i, getRef(ti,args[i]));
        }
        return ref;

      } else {
        return MJIEnv.NULL;
      }
    }

    public String evaluate (Object testObj, Object[] args) {
      if (predRef != -1) {
        ThreadInfo ti = ctx.getThreadInfo();
        MJIEnv env = ti.getEnv();
        DirectCallStackFrame frame = new DirectCallStackFrame(miPred);

        frame.pushRef( predRef);
        frame.pushRef( getRef(ti, testObj));
        frame.pushRef( getArrayRef(ti, args));

        ctx.getThreadInfo().executeMethodAtomic(frame);
        int errRef = frame.pop();

        if (errRef != MJIEnv.NULL) {
          return env.getStringObject(errRef);
        }
      } else {
        // <2do> not sure what's the best default reaction
      }

      return null;
    }

  }

  ContractContext ctx;

  Operand testObj;
  Operand[] args;

  Predicate pred;
  String violation;

  public Satisfies (ContractContext ctx, String id, Operand testObj, ArrayList<Operand> args) {
    this.testObj = testObj;
    this.ctx = ctx;

    if (args != null) {
      int n = args.size();
      if (n > 0) {
        this.args = new Operand[n];
        args.toArray(this.args);
      }
    }

    pred = getPredicate(id);
  }

  Predicate getPredicate(String id) {
    // the tricky part is when we have the CLASSPATH in the JPF classpath, i.e.
    // ClassInfo would find all the native predicate classes. We use the
    // NativePredicate interface to check for natives, and since native class loading
    // is faster than creating ClassInfos, we start with that

    try {
      Class<?> cls = Class.forName(id);
      if (NativePredicate.class.isAssignableFrom(cls)) { // it's native
        return (Predicate) cls.newInstance();
      }
    } catch (IllegalAccessException iax) {
      log.warning("no public constructor: " + id);
    } catch (InstantiationException iex) {
      log.warning("error intantiating Predicate object: " + id);
    } catch (ClassNotFoundException cfnx) {
      // can't be native, try model
    }

    // check if we can find a model predicate
    Predicate p = getModelPredicate(id);

    if (p == null) {
      log.warning("could not find predicate: " + id);
      return null;
    } else {
      return p;
    }
  }

  Predicate getModelPredicate (String id) {
    ClassInfo ci = ctx.resolveClassInfo(id);

    if (ci != null) {
      if (!ci.isInstanceOf("gov.nasa.jpf.Predicate")) {
        log.warning("not a predicate instance: " + id);
        return null;
      }

      ForwardingPredicate p = predicates.get(ci);
      if (p == null) {
        p = new ForwardingPredicate(ci);
        predicates.put(ci,p);
      }
      return p;

    } else {
      return null;
    }
  }

  public boolean holds (VarLookup lookup) {
    if (pred != null) {
      Object testObjVal = testObj.getValue(lookup);
      Object[] argVals = null;

      if (args != null) {
        argVals = new Object[args.length];
        for (int i=0; i<args.length; i++) {
          argVals[i] = args[i].getValue(lookup);
        }
      }

      violation = pred.evaluate(testObjVal, argVals);

    }

    return (violation == null);
  }

  protected void saveOldOperandValues(VarLookup lookup) {
    if (pred != null) {
      testObj.saveOldOperandValue(lookup);

      if (args != null) {
        for (int i=0; i<args.length; i++) {
          args[i].saveOldOperandValue(lookup);
        }
      }
    }
  }

  public String toString() {
    if (violation != null) {
      return violation;
    } else {
      return testObj.toString() + " satisfies " + pred;
    }
  }
}