package gov.nasa.jpf.jvm.bytecode;

public interface InstructionVisit {
	public void accept(InstructionVisitor insVisitor);
}