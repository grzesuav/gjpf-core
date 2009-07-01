//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.jvm;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.Debug;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;

import gov.nasa.jpf.jvm.bytecode.*;


/**
 * information associated with a method. Each method in JPF
 * is represented by a MethodInfo object
 */
public class MethodInfo extends InfoObject implements Cloneable {

  static final int INIT_MTH_SIZE = 4096;
  private static ArrayList<MethodInfo> mthTable;
  
  // special globalIds
  static final int DIRECT_CALL = -1;
  static final int REFLECTION_CALL = -2;

  static final String[] EMPTY = new String[0];
  
  /**
   * Used to warn about local variable information.
   */
  protected static boolean warnedLocalInfo = false;
  
  //--- various JPF method attributes
  static final int  MJI_NONE    = 0;
  static final int  MJI_NATIVE  = 0x1;
  
  static final int  EXEC_ATOMIC = 0x10000; // method executed atomically
  static final int  EXEC_HIDDEN = 0x20000; // method hidden from path
  static final int  FIREWALL    = 0x40000; // firewall any unhandled exceptions
                                           // (turn into UnhandledException throws)
  static final int  IS_CLINIT   = 0x80000;
  

  public class CodeBuilder {
    ArrayList<Instruction> insns = new ArrayList<Instruction>();

    // running code location
    int off;
    int pos;

    public int append(Instruction insn) {

      insns.add(insn);

      insn.setMethodInfo(MethodInfo.this);
      insn.setLocation(off, pos);

      off++;
      pos += insn.getLength();

      return off;
    }

    public void setCode() {
      code = insns.toArray(new Instruction[insns.size()]);
    }
  }


  /** Name of the method */
  protected String name;

  /** Signature of the method */
  protected String signature;

  /** Class the method belongs to */
  protected ClassInfo ci;

  /** Instructions associated with the method */
  protected Instruction[] code;

  /** JPFConfigException handlers */
  protected ExceptionHandler[] exceptions;

  /** Table used for line numbers */
  protected int[] lineNumbers;

  /** Local variables names */
  protected String[] localVariableNames;

  /** Local variables types */
  protected String[] localVariableTypes;

  /** Maximum number of local variables */
  protected int maxLocals;

  /** Maximum number of elements on the stack */
  protected int maxStack;

  
  //--- a batch of attributes
  
  /** the standard Java modifier attributes */
  int modifiers;
   
  /** a batch of execution related JPF attributes */
  int attrs;
      

  //--- all the stuff we need for native methods
  // <2do> pcm - turn this into a derived class

  /**  the number of stack slots for the arguments (incl. 'this'), lazy eval */
  private int argSize = -1;

  /** number of arguments (excl. 'this'), lazy eval */
  private int nArgs = -1;

  /** what return type do we have (again, lazy evaluated) */
  private byte returnType = -1;

  /** used for native method parameter conversion (lazy evaluated) */
  private byte[] argTypes = null;


  /**
   * this is a lazy evaluated mangled name consisting of the name and
   * arg type signature
   */
  private String uniqueName;
    
  /** a unique int assigned to this method */
  private int globalId = -1;
  
  static InstructionFactory insnFactory;
  
  static boolean init (Config config) {
    insnFactory = config.getEssentialInstance("vm.insn_factory.class", InstructionFactory.class);
    
    mthTable = new ArrayList<MethodInfo>(INIT_MTH_SIZE);
    return true;
  }
  
  /**
   * Creates a new method info.
   */
  protected MethodInfo (Method m, ClassInfo c) {
    name = m.getName();
    signature = m.getSignature();
    ci = c;

    code = loadCode(m);
    exceptions = loadExceptions(m);
    lineNumbers = loadLineNumbers(m);
    maxLocals = getMaxLocals(m);
    maxStack = getMaxStack(m);
    localVariableNames = loadLocalVariableNames(m);
    localVariableTypes = loadLocalVariableTypes(m);
    
    modifiers = m.getModifiers();
        
    // clinits are automatically synchronized on the class object,
    // and they don't let unhandled exceptions through
    if (name.equals("<clinit>")) {
      modifiers |= Modifier.SYNCHRONIZED;
      attrs |= IS_CLINIT | FIREWALL;
    }
    
    // since that's used to store the method in the ClassInfo, and to
    // identify it in tne InvokeInstruction, we can set it here
    uniqueName = getUniqueName(name, signature);
        
    globalId = mthTable.size();
    mthTable.add(this);
    
    loadAnnotations(m.getAnnotationEntries());
  }

  // for explicit construction only (direct calls)
  protected MethodInfo (int id) {
    globalId = id;
    // we don't want direct call methods in the mthTable (would be a memory leak)
  }
  
  public MethodInfo (ClassInfo ci, String name, int maxLocals, int maxStack, int modifiers){
    this.ci = ci;
    this.name = name;
    this.uniqueName = name;
    this.signature = "()V";
    this.maxLocals = maxLocals;
    this.maxStack = maxStack;
    this.localVariableNames = EMPTY;
    this.localVariableTypes = EMPTY;
    this.modifiers = modifiers;

    this.lineNumbers = null;
    this.exceptions = null;
        
    this.globalId = mthTable.size();
    mthTable.add(this);
  }
  
  public static MethodInfo getMethodInfo (int globalId){
    if (globalId >=0 && globalId <mthTable.size()){
      return mthTable.get(globalId);
    } else {
      return null;
    }
  }
  
  public static InstructionFactory getInstructionFactory() {
    return insnFactory;
  }
  
  public static void setInstructionFactory (InstructionFactory newFactory){
    insnFactory = newFactory;
  }

  public CodeBuilder getCodeBuilder() {
    return new CodeBuilder();
  }

  public void setClassInfo (ClassInfo ci){
    this.ci = ci;
  }
  
  public void setCode (Instruction[] code){
    for (int i=0; i<code.length; i++){
      code[i].setMethodInfo(this);
    }
    this.code = code;
  }
  
  public static int getNumberOfLoadedMethods () {
    return mthTable.size();
  }
    
  void setAtomic (boolean isAtomic) {
    if (isAtomic) {
      attrs |= EXEC_ATOMIC;
    } else {
      attrs &= ~EXEC_ATOMIC;
    }
  }
  public boolean isAtomic () {
    return ((attrs & EXEC_ATOMIC) != 0);
  }
  
  void setHidden (boolean isHidden) {
    if (isHidden) {
      attrs |= EXEC_HIDDEN;
    } else {
      attrs &= ~EXEC_HIDDEN;
    }
  }
  public boolean isHidden () {
    return ((attrs & EXEC_HIDDEN) != 0);    
  }
  
  /**
   * turn unhandled exceptions at the JPF execution level
   * into UnhandledException throws at the host VM level
   * this is useful to implement firewalls for direct calls
   * which should not let exceptions permeate into bytecode/
   * application code
   */
  public void setFirewall (boolean isFirewalled) {
    if (isFirewalled) {
      attrs |= FIREWALL;
    } else {
      attrs &= ~FIREWALL;
    }
  }
  public boolean isFirewall () {
    return ((attrs & FIREWALL) != 0);    
  }
  
  
  
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException cnx) {
      return null;
    }
  }
  
  public int getGlobalId() {
    return globalId;
  }
  
  protected MethodInfo createCallStub (String originator, int id){
    MethodInfo mi = new MethodInfo(id);
    String cname = ci.getName();
    Instruction insn;

    mi.name = originator + name; // + cname; // could maybe also include the called method, but keep it fast
    mi.signature = "()V";
    mi.maxLocals = isStatic() ? 0 : 1;
    mi.maxStack = getNumberOfCallerStackSlots();  // <2do> cache for optimization
    mi.localVariableNames = EMPTY;
    mi.localVariableTypes = EMPTY;
    mi.lineNumbers = null;
    mi.exceptions = null;
    mi.uniqueName = mi.name;

    // create the code
    CodeBuilder cb = mi.getCodeBuilder();

    if (isStatic()){
      mi.modifiers |= Modifier.STATIC;
      
      if (isClinit()) {
        insn = insnFactory.create(null, "INVOKECLINIT");
      } else {
        insn = insnFactory.create(null, "INVOKESTATIC");
      }
    } else if (name.equals("<init>")){
      insn = insnFactory.create(null, "INVOKESPECIAL");
    } else {
      insn = insnFactory.create(null, "INVOKEVIRTUAL");
    }
    ((InvokeInstruction)insn).setInvokedMethod(cname, name, signature);
    cb.append(insn);
    
    insn = insnFactory.create(null, "RETURN");
    cb.append(insn);

    cb.setCode();

    return mi;
  }
  
  /**
   * NOTE - this only works in conjunction with a special StackFrame
   */
  public MethodInfo createDirectCallStub (String originator) {
    return createCallStub(originator, DIRECT_CALL);
  }
  public boolean isDirectCallStub() {
    return (globalId == DIRECT_CALL);
  }

  public MethodInfo createReflectionCallStub() {
    return createCallStub("[reflection]", REFLECTION_CALL);
  }
  boolean isReflectionCallStub() {
    return (globalId == REFLECTION_CALL);
  }


  public boolean isSyncRelevant () {
    return (name.charAt(0) != '<');
  }
  
  public boolean isClinit () {
    return ((attrs & IS_CLINIT) != 0);
  }

  public boolean isClinit (ClassInfo ci) {
    return (((attrs & IS_CLINIT) != 0) && (this.ci == ci));
  }

  /**
   * yet another name - this time with a non-mangled, but abbreviated signature
   * and without return type (e.g. like "main(String[])"
   */
  public String getLongName () {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    
    sb.append('(');
    String[] argTypeNames = getArgumentTypeNames();
    for (int i=0; i<argTypeNames.length; i++) {
      String a = argTypeNames[i];
      int idx = a.lastIndexOf('.');
      if (idx > 0) {
        a = a.substring(idx+1);
      }
      if (i>0) {
        sb.append(',');
      }
      sb.append(a);
    }
    sb.append(')');
    
    return sb.toString();
  }
  
  /**
   * return the minimal name that has to be unique for overloading
   * used as a lookup key
   * NOTE: with the silent introduction of covariant return types
   * in Java 5.0, we have to use the full signature to be unique
   */
  public static String getUniqueName (String mname, String signature) {
    return (mname + signature);
  }

  public byte[] getArgumentTypes () {
    if (argTypes == null) {
      argTypes = Types.getArgumentTypes(signature);
      nArgs = argTypes.length;
    }

    return argTypes;
  }

  public String[] getArgumentTypeNames () {
    return Types.getArgumentTypeNames(signature);
  }
  
  public int getArgumentsSize () {
    if (argSize < 0) {
      argSize = Types.getArgumentsSize(signature);

      if (!isStatic()) {
        argSize++;
      }
    }

    return argSize;
  }

  public String getReturnTypeName () {
    return Types.getReturnTypeName(signature);
  }
  
  public String getSourceFileName () {
    if (ci != null) {
      return ci.getSourceFileName();
    } else {
      return "[VM]";
    }
  }

  public String getClassName () {
    if (ci != null) {
      return ci.getName();
    } else {
      return "[VM]";
    }
  }
  
  /**
   * Returns the class the method belongs to.
   */
  public ClassInfo getClassInfo () {
    return ci;
  }

  /**
   * Return the complete name of the method, including the class name.
   */
  public String getCompleteName () {
    return getClassName() + '.' + name + signature;
  }

  /**
   * return classname.name (but w/o signature)
   */
  public String getBaseName() {
    return getClassName() + '.' + name;
  }
  
  public boolean isExecutable (ThreadInfo ti) {
    // <2do> well, that doesn't take into account if executability depends on values
    // but 'isExecutable' is going away anyways
    return canEnter(ti);
  }

    
  public boolean isCtor () {
    return (name.equals("<init>"));
  }
  
  public boolean isInternalMethod () {
    // <2do> pcm - should turn this into an attribute for efficiency reasons
    return (name.equals("<clinit>") || uniqueName.equals("finalize()V"));
  }
  
  public boolean isThreadEntry (ThreadInfo ti) {
    return (uniqueName.equals("run()V") && (ti.countStackFrames() == 1));
  }
  
  /**
   * Returns the full name of the method, name and signature.
   */
  public String getFullName () {
    if (ci != null) {
      return ci.getName() + '.' + getUniqueName();
    } else {
      return (name + signature);
    }
  }

  /**
   * return number of instructions
   */
  public int getNumberOfInstructions() {
    return code.length;
  }
  
  /**
   * Returns a specific instruction.
   */
  public Instruction getInstruction (int i) {
    if (code == null) {
      return null;
    }

    if ((i < 0) || (i >= code.length)) {
      return null;
    }

    return code[i];
  }

  /**
   * Returns the instruction at a certain position.
   */
  public Instruction getInstructionAt (int position) {
    if (code == null) {
      return null;
    }

    for (int i = 0, l = code.length; i < l; i++) {
      if ((code[i] != null) && (code[i].getPosition() == position)) {
        return code[i];
      }
    }

    throw new JPFException("instruction not found");
  }

  /**
   * Returns the instructions of the method.
   */
  public Instruction[] getInstructions () {
    return code;
  }

  /**
   * Returns the line number for a given position.
   */
  public int getLineNumber (Instruction pc) {
    if (lineNumbers == null) {
      if (pc == null)
        return -1;
      else
        return pc.getPosition();
    }
    int idx = pc.getOffset();
    if (idx < 0) idx = 0;
    return lineNumbers[idx];
  }

  /**
   * Returns a table to translate positions into line numbers.
   */
  public int[] getLineNumbers () {
    return lineNumbers;
  }

  public boolean isMJI () {
    return ((attrs & MJI_NATIVE) != 0);
  }

  public int getMaxLocals () {
    return maxLocals;
  }

  public static int getMaxLocals (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return 0;
    }

    return c.getMaxLocals();
  }

  public int getMaxStack () {
    return maxStack;
  }

  public static int getMaxStack (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return 0;
    }

    return c.getMaxStack();
  }

  public ExceptionHandler[] getExceptions () {
    return exceptions;
  }

  public String[] getLocalVariableNames () {
    return localVariableNames;
  }

  public String[] getLocalVariableTypes () {
    return localVariableTypes;
  }

  public MethodInfo getOverriddenMethodInfo(){
    MethodInfo smi = null;
    
    if (ci != null) {
      ClassInfo sci = ci.getSuperClass();
      if (sci != null){
        smi = sci.getMethod(getUniqueName(), true);
      }
    }
    
    return smi;
  }
  
  /**
   * Returns the name of the method.
   */
  public String getName () {
    return name;
  }

  public String getJNIName () {
    return Types.getJNIMangledMethodName(null, name, signature);
  }
  
  public int getModifiers () {
    return modifiers;
  }
  
  /**
   * Returns true if the method is native
   */
  public boolean isNative () {
    return ((modifiers & Modifier.NATIVE) != 0);
  }

  public boolean isAbstract () {
    return ((modifiers & Modifier.ABSTRACT) != 0);
  }
  
  public int getNumberOfArguments () {
    if (nArgs < 0) {
      nArgs = Types.getNumberOfArguments(signature);
    }

    return nArgs;
  }

  /**
   * Returns the size of the arguments.
   * This returns the number of parameters passed on the stack, incl. 'this'
   */
  public int getNumberOfStackArguments () {
    int n = getNumberOfArguments();

    return isStatic() ? n : n + 1;
  }

  public int getNumberOfCallerStackSlots () {
    return Types.getNumberOfStackSlots(signature, isStatic()); // includes return type
  }
  
  /**
   * do we return Object references?
   */
  public boolean isReferenceReturnType () {
    int r = getReturnType();

    return ((r == Types.T_REFERENCE) || (r == Types.T_ARRAY));
  }

  public byte getReturnType () {
    if (returnType < 0) {
      returnType = Types.getReturnType(signature);
    }

    return returnType;
  }

  /**
   * Returns the signature of the method.
   */
  public String getSignature () {
    return signature;
  }

  /**
   * Returns true if the method is static.
   */
  public boolean isStatic () {
    return ((modifiers & Modifier.STATIC) != 0);
  }

  /**
   * is this a public method
   */
  public boolean isPublic() {
    return ((modifiers & Modifier.PUBLIC) != 0);
  }
  
  public boolean isPrivate() {
    return ((modifiers & Modifier.PRIVATE) != 0);
  }
  
  public boolean isProtected() {
    return ((modifiers & Modifier.PROTECTED) != 0);
  }
  
  
  /**
   * Returns true if the method is synchronized.
   */
  public boolean isSynchronized () {
    return ((modifiers & Modifier.SYNCHRONIZED) != 0);
  }
  
  public String getUniqueName () {
    return uniqueName;
  }

  public boolean canEnter (ThreadInfo th) {
    if (isSynchronized()) {
      ElementInfo ei = getBlockedObject(th, true);

      // <?> pcm - the other way round would be intuitive
      return ei.canLock(th);
    }

    return true;
  }

  public ElementInfo getBlockedObject (ThreadInfo th, boolean isBeforeCall) {
    int         objref;
    ElementInfo ei = null;

    if (isSynchronized()) {
      if (isStatic()) {
        objref = ci.getClassObjectRef();
      } else {
        // NOTE 'inMethod' doesn't work for natives, because th.getThis()
        // pulls 'this' from the stack frame, which we don't have (and don't need)
        // for natives
        objref = isBeforeCall ? th.getCalleeThis(this) : th.getThis();
      }

      DynamicArea da = JVM.getVM().getDynamicArea();
      ei = da.get(objref);

      assert (ei != null) : ("inconsistent stack, no object or class ref: " +
                               getCompleteName() + " (" + objref +")");
    }

    return ei;
  }

  public void enter (ThreadInfo ti) {
    if (isSynchronized()) {
      ElementInfo ei = getBlockedObject(ti, false);
      ei.lock(ti);
      
      if (isStatic() && isClinit()) {
        ci.setInitializing(ti);
      }
    }
  }

  public void leave (ThreadInfo ti) {
    
    // <2do> - that's not really enough, we might have suspicious bytecode that fails
    // to release locks acquired by monitor_enter (e.g. by not having a handler that
    // monitor_exits & re-throws). That's probably shifted into the bytecode verifier
    // in the future (i.e. outside JPF), but maybe we should add an explicit test here
    // and report an error if the code does asymmetric locking (according to the specs,
    // VMs are allowed to silently fix this, so it might run on some and fail on others)
    
    if (isSynchronized()) {
      ElementInfo ei = getBlockedObject(ti, false);
      ei.unlock(ti);
      
      if (isStatic() && isClinit()) {
        // we just released the lock on the class object, returning from a clinit
        // now we can consider this class to be initialized.
        // NOTE this is still part of the RETURN insn of clinit, so ClassInfo.isInitialized
        // is protected
        ci.setInitialized();
      }
    }
  }
  
  /**
   * execute this method, which might be either bytecode or native.
   */
  public Instruction execute (ThreadInfo ti) {

    if (((attrs & MJI_NATIVE) != 0) || isNative()) {
      NativePeer nativePeer = ci.getNativePeer();
      if (nativePeer != null) {
        return  nativePeer.executeMethod(ti, this);
      } else {
        return ti.createAndThrowException("java.lang.UnsatisfiedLinkError",
                                          ci.getName() + '.' + getUniqueName() + " (no peer)");
      }
      
    } else {
      ti.pushFrame( new StackFrame(this, ti.getTopFrame()));
      enter(ti);

      return ti.getPC();
    }
  }

  /**
   * Loads the code of the method.
   */
  protected Instruction[] loadCode (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return null;
    }

    InstructionList     il = new InstructionList(c.getCode());

    InstructionHandle[] hs = il.getInstructionHandles();
    
    int                 length = hs.length;

    Instruction[]       is = new Instruction[length];

    for (int i = 0; i < length; i++) {
      is[i] = insnFactory.create(hs[i], i, this, m.getConstantPool());

      if (c.getLineNumberTable() != null) {
        // annoying bug when BCEL don't seem to find linenumber - pos match
        // also sometimes linenumber tables are not available
        is[i].setContext(ci.getName(), name,
                         c.getLineNumberTable()
                          .getSourceLine(is[i].getPosition()),
                         is[i].getPosition());
      }
    }

    return is;
  }

  /**
   * Returns the exceptions of the method.
   */
  protected ExceptionHandler[] loadExceptions (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return null;
    }

    CodeException[] ce = c.getExceptionTable();

    if (ce.length == 0) {
      return null;
    }

    int                length = ce.length;
    ExceptionHandler[] eh = new ExceptionHandler[length];

    ConstantPool       cp = m.getConstantPool();

    for (int i = 0; i < length; i++) {
      int ct = ce[i].getCatchType();
      eh[i] = new ExceptionHandler(((ct == 0)
                                    ? null
                                    : cp.getConstantString(ct,
                                                           Constants.CONSTANT_Class)
                                        .replace('/', '.')), ce[i].getStartPC(),
                                   ce[i].getEndPC(), ce[i].getHandlerPC());
    }

    return eh;
  }

  /**
   * Loads the line numbers for the method.
   */
  protected int[] loadLineNumbers (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return null;
    }

    LineNumberTable lnt = c.getLineNumberTable();

    int             length = code.length;
    int[]           ln = new int[length];

    if (lnt == null) {
      // no line information
      return null;
    } else {
      for (int i = 0; i < length; i++) {
        try { //annoying bug when BCEL don't seem to find linenumber - pos match
          ln[i] = lnt.getSourceLine(code[i].getPosition());
        } catch (RuntimeException e) {
          System.out.print("^");
        }
      }
    }

    return ln;
  }

  /**
   * Loads the names of the local variables.
   *
   * NOTE: BCEL only gives us a list of all *named* locals, which might not
   * include all local vars (temporaries, like StringBuilder). Note that we have
   * to fill this with "?" in order to make the returned array correspond with
   * slot numbers
   */
  protected String[] loadLocalVariableNames (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return null;
    }

    LocalVariableTable lvt = c.getLocalVariableTable();

    if (lvt == null) {
      if (!warnedLocalInfo && !ci.isSystemClass()) {
        Debug.println(Debug.WARNING);
        Debug.println(Debug.WARNING, "No local variable information available");
        Debug.println(Debug.WARNING, "for " + getCompleteName());
        Debug.println(Debug.WARNING,
                      "Recompile with -g to include this information");
        Debug.println(Debug.WARNING);
        warnedLocalInfo = true;
      }

      return null;
    }

    LocalVariable[] lv = lvt.getLocalVariableTable();
    int             length = lv.length;
    String[]        v = new String[c.getMaxLocals()];

    for (int i = 0; i < length; i++) {
      v[lv[i].getIndex()] = lv[i].getName();
    }

    for (int i=0; i<v.length; i++) {
      if (v[i] == null) {
        v[i] = "?";
      }
    }

    return v;
  }

  /**
   * Loads the types of the local variables.
   * see loadLocalVariableNames for the problem with temporaries and
   * why we can't copy the types 1:1
   */
  protected String[] loadLocalVariableTypes (Method m) {
    Code c = m.getCode();

    if (c == null) {
      return null;
    }

    LocalVariableTable lvt = c.getLocalVariableTable();

    if (lvt == null) {
      if (!warnedLocalInfo && !ci.isSystemClass()) {
        Debug.println(Debug.WARNING, "No local variable information available");
        Debug.println(Debug.WARNING, "for " + getCompleteName());
        Debug.println(Debug.WARNING,
                      "Recompile with -g to include this information");
        Debug.println(Debug.WARNING);
        warnedLocalInfo = true;
      }

      return null;
    }

    LocalVariable[] lv = lvt.getLocalVariableTable();
    int             length = lv.length;
    String[]        v = new String[c.getMaxLocals()];

    for (int i = 0; i < length; i++) {
      v[lv[i].getIndex()] = lv[i].getSignature();
    }

    for (int i=0; i<v.length; i++) {
      if (v[i] == null) {
        v[i] = "?";
      }
    }

    return v;
  }

  void setMJI (boolean isMJI) {
    if (isMJI) {
      attrs |= MJI_NATIVE;
    } else {
      attrs &= ~MJI_NATIVE;
    }
  }
  
  public String toString() {
    return "MethodInfo[" + getFullName() + ']';
  }
}
