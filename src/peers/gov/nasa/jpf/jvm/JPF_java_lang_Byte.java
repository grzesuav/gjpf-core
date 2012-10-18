package gov.nasa.jpf.jvm;

public class JPF_java_lang_Byte extends NativePeer {
  // <2do> at this point we deliberately do not override clinit

  public int valueOf__B__Ljava_lang_Byte_2 (MJIEnv env, int clsRef, byte val) {
    return env.valueOfByte(val);
  }
}
