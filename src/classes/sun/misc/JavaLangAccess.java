package sun.misc;

import sun.reflect.*;
import sun.reflect.annotation.*;
import sun.nio.ch.Interruptible;

/**
 * this is a placeholder for a Java 6 class, which we only have here to
 * support both Java 1.5 and 6 with the same set of env/ classes
 *
 * see sun.msic.SharedSecrets for details
 *
 * <2do> THIS IS GOING AWAY AS SOON AS WE OFFICIALLY SWITCH TO JAVA 6
 */

public interface JavaLangAccess {

    ConstantPool getConstantPool(Class<?> cls);

    void setAnnotationType(Class<?> cls, AnnotationType annotationType);

    AnnotationType getAnnotationType(Class<?> cls);

    <E extends Enum<E>> E[] getEnumConstantsShared(Class<E> cls);

    void blockedOn(Thread t, Interruptible b);

}
