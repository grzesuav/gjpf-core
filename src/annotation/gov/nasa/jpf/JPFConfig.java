package gov.nasa.jpf;

/**
 * annotation that can be used to change JPF config properties
 * from within the application
 */
public @interface JPFConfig {
  String[] value(); // each element is a "key[+]=val" pair
}
