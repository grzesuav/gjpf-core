package gov.nasa.jpf.jvm;

import java.lang.annotation.*;

/**
 * Indicates a class from JPF that can be used in models and, therefore,
 * loaded as model code by the JPF VM.  Types that do not have this
 * annotation are not intended to be used in models.
 * 
 * TODO: (future) check that any model classes loaded from gov.nasa.jpf.*
 * have this annotation, (unless, perhaps the main class is from gov.nasa.jpf.*)
 *
 * @author peterd
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAPI {}
