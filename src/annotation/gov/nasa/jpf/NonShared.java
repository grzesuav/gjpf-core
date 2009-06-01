package gov.nasa.jpf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * annotation for classes that should not have any instances that are
 * referenced from different threads.
 * 
 * The @NonShared attribute is NOT inherited, it only applies
 * to objects of this concrete type (hence it's pointless for
 * abstract classes and interfaces)
 * 
 * This is a more strict form of @NotThreadSafe, which still allows
 * shared references as long as each access is protected by locks. For
 * @NonShared objects, it's already a property violation if the
 * reference escapes.
 * 
 * On the other hand, it's less strict than non-reachable, because
 * it is only checked for explicit references (GETFIELD,PUTFIELD)
 */
@Retention(RetentionPolicy.RUNTIME)

public @interface NonShared {

}
