package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.logging.Logger;


/**
 * This is the simple version of IdleFilter. This one simply breaks all back-edges
 * encountered to make sure JPF's partial-order reduction doesn't add meaningless
 * transitions forever. This is our dual of the cycle-proviso in classic po-reduction theory.
 *
 *  One can set how many back-edges to consider before breaking, but by default it is 1
 *
 */
public class SimpleIdleFilter extends ListenerAdapter {

	  static Logger log = JPF.getLogger("gov.nasa.jpf.listener.SimpleIdleFilter");

	  static class ThreadStat {
	    String tname;

	    int backJumps;

	    int loopStartPc;

	    int loopEndPc;

	    int loopStackDepth;

	    ThreadStat(String tname) {
	      this.tname = tname;
	    }
	  }

	  ObjVector<ThreadStat> threadStats = new ObjVector<ThreadStat>();

	  ThreadStat ts;

	  int maxBackJumps;

	  // ----------------------------------------------------- SearchListener
	  // interface

	  public SimpleIdleFilter(Config config) {
	    maxBackJumps = config.getInt("idle.max_backjumps", 1);
	  }

	  public void stateAdvanced(Search search) {
	    ts.backJumps = 0;
	    ts.loopStackDepth = 0;
	    ts.loopStartPc = ts.loopEndPc = 0;
	  }

	  public void stateBacktracked(Search search) {
	    ts.backJumps = 0;
	    ts.loopStackDepth = 0;
	    ts.loopStartPc = ts.loopEndPc = 0;
	  }

	  // ----------------------------------------------------- VMListener interface
	  public void instructionExecuted(VM vm) {
	    Instruction insn = vm.getLastInstruction();

       if (!insn.isBackJump()) {     // Put this test first for a performance optimization.
         return;
       }

	    ThreadInfo ti = vm.getLastThreadInfo();

	    int tid = ti.getId();
	    ts = threadStats.get(tid);
	    if (ts == null) {
	      ts = new ThreadStat(ti.getName());
	      threadStats.set(tid, ts);
	    }

       ts.backJumps++;

       int loopStackDepth = ti.getStackDepth();
       int loopPc = vm.getNextInstruction().getPosition();

       if ((loopStackDepth != ts.loopStackDepth) || (loopPc != ts.loopStartPc)) {
         // new loop, reset
         ts.loopStackDepth = loopStackDepth;
         ts.loopStartPc = loopPc;
         ts.loopEndPc = insn.getPosition();
         ts.backJumps = 0;
       } else {
         if (ts.backJumps > maxBackJumps) {
           ti.reschedule(true); // this breaks the executePorStep loop
         }
       }
	  }
}
