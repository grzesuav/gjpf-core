package java.nio;

public abstract class Buffer {
	protected int position;
	protected int capacity;
	protected int limit;

	public final int capacity() {
		return capacity;
	}

	public final Buffer position(int newPosition) {
		if ((newPosition<0)||(newPosition>limit)) {
			throw new IllegalArgumentException("Illegal buffer position exception: "+newPosition);
		}
		this.position = newPosition;
		return this;
	}

	public final int position() {
		return position;
	}

	public final int limit() {
		return this.limit;
	}

	public final Buffer limit(int newLimit) {
		if ((newLimit<0)||(newLimit>capacity)) {
			throw new IllegalArgumentException("Illegal buffer limit exception: "+newLimit);
		}
		this.limit = newLimit;
		return this;
	}

	public final Buffer clear(){
		position = 0;
		limit = capacity;
		return this;
	}

	public final Buffer flip() {
		limit = position;
		position = 0;
		return this;
	}

	public final Buffer rewind() {
		position = 0;
		return this;
	}

	public final int remaining() {
		return limit-position;
	}

	public final boolean hasRemaining() {
		return remaining()>0;
	}

	public abstract boolean hasArray();

	public abstract Object array();
}
