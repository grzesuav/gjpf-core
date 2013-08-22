package java.nio.channels;

import java.io.IOException;
import java.io.FileDescriptor;

import java.nio.ByteBuffer;

//This class uses the methods from FileDescriptor in order to access files
public class FileChannel {

	public int read(ByteBuffer dst) throws IOException{
		return fd.read(dst.array(),0,dst.array().length);
	}

	public int write(ByteBuffer src) throws IOException{
		fd.write(src.array(),0,src.array().length);
		return src.array().length;
	}

	public void close() throws IOException{
		fd.close();
	}

	public FileChannel(FileDescriptor fd){
		this.fd = fd;
	}

	private FileDescriptor fd = null;

}
