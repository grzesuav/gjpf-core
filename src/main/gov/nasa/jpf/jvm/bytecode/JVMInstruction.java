package gov.nasa.jpf.jvm.bytecode;

public interface JVMInstruction {
	public void accept(JVMInstructionVisitor insVisitor);
}