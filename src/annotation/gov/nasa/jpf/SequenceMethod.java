package gov.nasa.jpf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface SequenceMethod {
  
  String id();
  
  // BCEL bug workaround (doesn't recognize default attrs)
  //String name()  default "<method>";
  //String args() default "";
  String result() default "";
}
