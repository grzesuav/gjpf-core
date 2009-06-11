package gov.nasa.jpf.test.annotation;

import gov.nasa.jpf.SequenceEvent;
import gov.nasa.jpf.SequenceMethod;
import gov.nasa.jpf.SequenceObject;
import gov.nasa.jpf.Sequence;

public class TestSequence {
  
  static class A {
    B b;
    State state;

    abstract class State {
      abstract void confirmed();
    }
    class NoRequest extends State {
      void confirmed() {
        throw new RuntimeException("not prepared to receive confirmation");
      }
    }
    class OpenRequest extends State {
      void confirmed() {
        setState( new ProcessRequest());
        b.processData(A.this);
      }
    }
    class ProcessRequest extends State {
      void confirmed() {
        setState( new CloseRequest());
        b.close(A.this);
      }      
    }
    class CloseRequest extends State {
      void confirmed() {
        b = null;
        setState( new NoRequest());
      }      
    }
    
    
    @SequenceMethod(id="MySequence")
    public void initialize(B b) {
      this.b = b;

      // this should not be logged
      B irrelevantObj = new B();
      irrelevantObj.doWhatever();
      
      state = new OpenRequest();
      b.open(this);
    }
    
    @SequenceMethod(id="MySequence")
    public void confirmed() {
      state.confirmed();
    }
    
    void setState (State newState) {
      state = newState;
    }
  }
  
  static class B {

    @SequenceObject(id="MySequence", object="O3")
    C c;
    
    @SequenceObject(id="MySequence", object="O4")    
    D d;
    
    void initAllComponents() {
      c = new C();
      d = new D();
      
      c.init();
    }
    
    @SequenceMethod(id="MySequence")
    public void open(A client) {
      initAllComponents();
      client.confirmed();
    }
    
    @SequenceMethod(id="MySequence")
    void processData(A client) {
      c.decode();
      doSomething();
      d.integrate();
      client.confirmed();
    }
    
    void doSomething() {
    }
    
    @SequenceMethod(id="MySequence")
    public void close(A client) {
      client.confirmed();
    }
    
    @SequenceMethod(id="MySequence")
    public void doWhatever() { // should not be called on a monitored object
    }
  }
  
  static class C {
    @SequenceMethod(id="MySequence")
    public void init() {
      // whatever
    }
    
    @SequenceMethod(id="MySequence", result="block")
    public byte[] decode() {
      return null;
    }
  }
  
  static class D {
    
    @SequenceMethod(id="MySequence", result="sum")
    public double integrate () {
      return 42.0;
    }
  }
  
  @Sequence(id="MySequence", objects= {"O1=a", "O2=b", "O3", "O4"})
  public static void testSequence (A a) {
    B b = new B();
    
    a.initialize(b);
  }
  
  public static void main (String[] args) {
    testSequence(new A());
  }
}
