package gov.nasa.jpf.jvm;

public class BoxObjectCacheManager {
  private static String boxObjectCaches = "gov.nasa.jpf.BoxObjectCaches";

  public static int initIntCache(ThreadInfo ti) {
	int low = -128;
    int high = 127;

	int n = (high-low) + 1;
    int arr = ti.getHeap().newArray("Ljava/lang/Integer", n, ti);
    ti.getHeap().registerPinDown(arr);
    ElementInfo ei = ti.getElementInfo(arr);

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Integer");
    for(int i = 0; i < n; i++) {
      int intObj = ti.getHeap().newObject(ci, ti);
      ti.getElementInfo(intObj).setIntField("value", i + low);
      ei.setReferenceElement(i, intObj);
      ti.getHeap().registerPinDown(intObj);
    }

    ClassInfo cacheClass  = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    cacheClass.getStaticElementInfo().setReferenceField("intCache", arr);
    return arr;
  }

  public static int valueOfInteger(ThreadInfo ti, int i) {
	int low = -128;
	int high = 127;

    if(i >= low && i <= high) {
      ClassInfo cacheClass  = ClassInfo.getResolvedClassInfo(boxObjectCaches);
      int intCache = cacheClass.getStaticElementInfo().getReferenceField("intCache");
      if(intCache == MJIEnv.NULL) {
        intCache = initIntCache(ti);
      }
      return ti.getElementInfo(intCache).getReferenceElement(i - low);
    }

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Integer");
    int intObj = ti.getHeap().newObject(ci, ti);
    ti.getElementInfo(intObj).setIntField("value", i);
    return intObj;
  }
}
