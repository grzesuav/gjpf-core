package gov.nasa.jpf;

/**
 * @author Nastaran
 *
 * Refereces to the caches for the types Byte, Character, Short, Integer, 
 * Long.
 * 
 * This class is added to the startup class list in JVM.initialize, to be
 * loaded before the valueOf()s are called.
 */
public class BoxObjectCaches {
  // Byte cache
  static Byte byteCache[];

  // Character cache
  static Character charCache[];

  // Short
  static Short shortCache[];

  // Integer cache
  static Integer intCache[];

  // Long cache
  static Long longCache[];
}