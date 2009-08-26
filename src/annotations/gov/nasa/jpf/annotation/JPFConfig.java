package gov.nasa.jpf.annotation;

/**
 * annotation that can be used to change JPF config properties
 * from within the SuT
 * using such annotations should NOT make the SuT JPF dependent
 */
public @interface JPFConfig {
  String[] value(); // each element is a "key[+]=val" pair
}
