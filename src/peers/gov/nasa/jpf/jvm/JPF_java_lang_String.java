//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.jvm;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * MJI NativePeer class for java.lang.String library abstraction
 */
public class JPF_java_lang_String {

	public static double numObjects;
	public static double numSubstring;
	public static double numConstructorCalls;
	
	public static int init___3CII__Ljava_lang_String_2(MJIEnv env, int objRef,int valueRef, int offset, int count){
		char[]value=env.getCharArrayObject(valueRef);
		String result= new String(value,offset,count);
		return env.newString(result);
	}

	public static int init___3III__Ljava_lang_String_2(MJIEnv env, int objRef,int codePointsRef, int offset, int count){
		int[]codePoints=env.getIntArrayObject(codePointsRef);
		String result= new String(codePoints,offset,count);
		return env.newString(result);
	}

	public static int init___3BIII__Ljava_lang_String_2(MJIEnv env, int objRef,int asciiRef, 
			int hibyte,int offset, int count){
		byte[]ascii=env.getByteArrayObject(asciiRef);
		String result=new String(ascii,hibyte,offset,count);
		return env.newString(result);
	}
	
	
	public static int init___3BIILjava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef,int bytesRef, 
			int offset, int length,int charsetNameRef) throws UnsupportedEncodingException{
		byte[]bytes=env.getByteArrayObject(bytesRef);
		String charsetName=env.getStringObject(charsetNameRef);
		String result=new String(bytes,offset,length,charsetName);
		return env.newString(result);
	}
	
	public static int init___3BIILjava_nio_charset_Charset_2__Ljava_lang_String_2(MJIEnv env, int objRef,int bytesRef, 
			int offset,int length, int charsetRef){
		throw new IllegalStateException("sorry, can't reflect, no Charset model.  You must punch code directly into the String model");
	}

	public static int init___3BII__Ljava_lang_String_2(MJIEnv env, int objRef,int bytesRef, 
			int offset, int length){
		byte[]bytes=env.getByteArrayObject(bytesRef);
		String result=new String(bytes,offset,length);
		return env.newString(result);
	}
	
	public static int init___Ljava_lang_StringBuffer_2__Ljava_lang_String_2(MJIEnv env, int objRef,int bufferRef){
		throw new IllegalStateException("sorry, can't reflect, no StringBuffer model.  You must punch code directly into the String model");
	}

	public static int init___Ljava_lang_StringBuilder_2__Ljava_lang_String_2(MJIEnv env, int objRef,int bufferRef){
		throw new IllegalStateException("sorry, can't reflect, no StringBuilder model.  You must punch code directly into the String model");
	}



	static public int codePointAt__I__I(MJIEnv env, int objRef, int index){
		String obj = env.getStringObject(objRef);
		return obj.codePointAt(index);
	}
	
	static public int codePointBefore__I__I(MJIEnv env, int objRef, int index){
		String obj = env.getStringObject(objRef);
		return obj.codePointBefore(index);
	}

	static public int codePointCount__II__I(MJIEnv env, int objRef,int beginIndex, int endIndex){
		String obj = env.getStringObject(objRef);
		return obj.codePointCount(beginIndex,endIndex);
	}

	static public int offsetByCodePoints__II__I(MJIEnv env, int objRef,int index, int codePointOffset){
		String obj = env.getStringObject(objRef);
		return obj.offsetByCodePoints(index,codePointOffset);
	}
	
	static public void getChars__II_3CI__V(MJIEnv env,int objRef,int srcBegin, int srcEnd, int dstRef, int dstBegin){
		String obj = env.getStringObject(objRef);
		char[]dst=env.getCharArrayObject(dstRef);
		obj.getChars(srcBegin,srcEnd,dst,dstBegin);
	}

	static public void getBytes__II_3BI__V(MJIEnv env,int objRef,int srcBegin, int srcEnd, int dstRef, int dstBegin){
		String obj = env.getStringObject(objRef);
		byte[]dst=env.getByteArrayObject(dstRef);
		obj.getBytes(srcBegin,srcEnd,dst,dstBegin);
	}

	public static int getBytes__Ljava_lang_String_2___3B(MJIEnv env, int objRef, int charSetRef){
		String string = env.getStringObject(objRef);
		String charset = env.getStringObject(charSetRef);

		try {
			byte[] b=string.getBytes(charset);
			return env.newByteArray(b);

		} catch (UnsupportedEncodingException uex){
			env.throwException(uex.getClass().getName(), uex.getMessage());
			return MJIEnv.NULL;
		}
	}

	static public int getBytes_____3B(MJIEnv env,int objRef){
		String obj = env.getStringObject(objRef);
		byte[]bytes=obj.getBytes();
		return env.newByteArray(bytes);
	}

	public static boolean equals__Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int argRef) {
		if (argRef == MJIEnv.NULL){
			return false;

		}

		Heap   heap = env.getHeap();
		ElementInfo   s1 = heap.get(objRef);
		ElementInfo   s2 = heap.get(argRef);

		if (!env.isInstanceOf(argRef,"java.lang.String")) {
			return false;
		}

		Fields f1 = heap.get( s1.getReferenceField("value")).getFields();
		int o1 = s1.getIntField("offset");
		int l1 = s1.getIntField("count");

		Fields f2 = heap.get( s2.getReferenceField("value")).getFields();
		int o2 = s2.getIntField("offset");
		int l2 = s2.getIntField("count");

		if (l1 != l2) {
			return false;
		}

		char[] c1 = ((CharArrayFields)f1).asCharArray();
		char[] c2 = ((CharArrayFields)f2).asCharArray();

		for (int j=o1, k=o2, max=o1+l1; j<max; j++, k++){
			if (c1[j] != c2[k]){
				return false;
			}
		}

		return true;
	}


	
	public static boolean equalsIgnoreCase__Ljava_lang_String_2__Z(MJIEnv env, int objref, int anotherString) {
		String thisString = env.getStringObject(objref);
		if (anotherString != MJIEnv.NULL){
			return thisString.equalsIgnoreCase(env.getStringObject(anotherString));
		} else {
			return false;
		}
	}

	
	public static int compareTo__Ljava_lang_String_2__I(MJIEnv env,int objRef,int anotherStringRef){
		String obj = env.getStringObject(objRef);
		String anotherString = env.getStringObject(anotherStringRef);
		return obj.compareTo(anotherString);
	}
	

	static public int MJIcompare__Ljava_lang_String_2Ljava_lang_String_2__I(MJIEnv env,int clsRef,
			int s1Ref,int s2Ref){
		// Is there a way to reflect? Punching in from jkd1.7.0_07
		String s1=env.getStringObject(s1Ref);
		String s2=env.getStringObject(s2Ref);
        int n1 = s1.length();
        int n2 = s2.length();
        int min = Math.min(n1, n2);
        for (int i = 0; i < min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        // No overflow because of numeric promotion
                        return c1 - c2;
                    }
                }
            }
        }
        return n1 - n2;	
	}
	
	public static boolean regionMatches__ILjava_lang_String_2II__Z(MJIEnv env,int objRef,
			int toffset,int otherRef, int ooffset, int len){
		String obj = env.getStringObject(objRef);
		String other = env.getStringObject(otherRef);
		return obj.regionMatches(toffset,other,ooffset,len);
		
	}


	public static boolean regionMatches__ZILjava_lang_String_2II__Z(MJIEnv env,int objRef,
			boolean ignoreCase, int toffset,
			int otherRef, int ooffset, int len){
		String obj = env.getStringObject(objRef);
		String other = env.getStringObject(otherRef);
		return obj.regionMatches(ignoreCase,toffset,other,ooffset,len);
		
	}

	static public boolean startsWith__Ljava_lang_String_2I__Z(MJIEnv env, int objRef, int prefixRef, int toffset){
		String thisStr = env.getStringObject(objRef);
		String prefix = env.getStringObject(prefixRef);
		return thisStr.startsWith(prefix,toffset);
	}


	static public boolean startsWith__Ljava_lang_String_2__Z(MJIEnv env, int objRef, int prefixRef){
		String thisStr = env.getStringObject(objRef);
		String prefix = env.getStringObject(prefixRef);
		return thisStr.startsWith(prefix);
	}

	public static int hashCode____I (MJIEnv env, int objref) {
		int h = env.getIntField(objref, "hash");

		if (h == 0){
			int vref = env.getReferenceField(objref, "value");
			int off = env.getIntField(objref, "offset");
			int len = env.getIntField(objref, "count");

			// now get the char array data, but be aware they are stored as ints
			ElementInfo ei = env.getElementInfo(vref);
			char[] values = ((CharArrayFields)ei.getFields()).asCharArray();

			for (int max=off+len; off<max; off++) {
				h = 31*h + values[off];
			}
			env.setIntField(objref, "hash", h);

		}    

		return h;
	}

	public static int indexOf__I__I (MJIEnv env, int objref, int c) {
		return indexOf__II__I(env,objref,c,0);
	}


	public static int indexOf__II__I (MJIEnv env, int objref, int c, int fromIndex) {
		int vref = env.getReferenceField(objref, "value");
		int off = env.getIntField(objref, "offset");
		int len = env.getIntField(objref, "count");

		if (fromIndex >= len){
			return -1;
		}
		if (fromIndex < 0){
			fromIndex = 0;
		}

		ElementInfo ei = env.getElementInfo(vref);
		char[] values = ((CharArrayFields)ei.getFields()).asCharArray();

		for (int i=fromIndex; i<len; i++){
			if (values[i+off] == c){
				return i;
			}
		}

		return -1;
	}

	public static int lastIndexOf__I__I (MJIEnv env, int objref, int c) {
		return lastIndexOf__II__I(env,objref,c,Integer.MAX_VALUE);
	}

	public static int lastIndexOf__II__I (MJIEnv env, int objref, int c, int fromIndex) {
		int vref = env.getReferenceField(objref, "value");
		int off = env.getIntField(objref, "offset");
		int len = env.getIntField(objref, "count");

		if (fromIndex < 0){
			return -1;
		}
		if (fromIndex > len-1){
			fromIndex = len-1;
		}

		ElementInfo ei = env.getElementInfo(vref);
		char[] values = ((CharArrayFields)ei.getFields()).asCharArray();

		for (int i=fromIndex; i>0; i--){
			if (values[i+off] == c){
				return i;
			}
		}

		return -1;
	}




	public static int indexOf__Ljava_lang_String_2__I(MJIEnv env, int objref, int str) {
		String thisStr = env.getStringObject(objref);
		String indexStr = env.getStringObject(str);

		return thisStr.indexOf(indexStr);
	}


	public static int indexOf__Ljava_lang_String_2I__I(MJIEnv env, int objref, int str, int fromIndex) {
		String thisStr = env.getStringObject(objref);
		String indexStr = env.getStringObject(str);

		return thisStr.indexOf(indexStr, fromIndex);
	}

	public static int lastIndexOf__Ljava_lang_String_2I__I(MJIEnv env, int objref, int str, int fromIndex) {
		String thisStr = env.getStringObject(objref);
		String indexStr = env.getStringObject(str);

		return thisStr.lastIndexOf(indexStr, fromIndex);
	}

	
	public static int substring__I__Ljava_lang_String_2(MJIEnv env,int objRef,int beginIndex){
		String obj = env.getStringObject(objRef);
		String result=obj.substring(beginIndex);
		return env.newString(result);
		
	}
	public static int substring__II__Ljava_lang_String_2(MJIEnv env,int objRef,int beginIndex,
			int endIndex){
		String obj = env.getStringObject(objRef);
		String result=obj.substring(beginIndex,endIndex);
		return env.newString(result);
		
	}

	public static int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int strRef) {
		Heap heap = env.getHeap();

		ElementInfo thisStr = heap.get(objRef);
		ElementInfo otherStr = heap.get(strRef);

		int thisLength = thisStr.getIntField("count");
		int otherLength = otherStr.getIntField("count");

		if (otherLength == 0)
			return objRef;

		int thisOffset = thisStr.getIntField("offset");
		CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
		char thisChars[] = thisFields.asCharArray();

		int otherOffset = otherStr.getIntField("offset");
		CharArrayFields otherFields = (CharArrayFields) heap.get(otherStr.getReferenceField("value")).getFields();
		char otherChars[] = otherFields.asCharArray();

		char resultChars[] = new char[thisLength + otherLength];
		System.arraycopy(thisChars, thisOffset, resultChars, 0, thisLength);
		System.arraycopy(otherChars, otherOffset, resultChars, thisLength, otherLength);

		return env.newString(new String(resultChars));
	}
	
	//--- the various replaces

	public static int replace__CC__Ljava_lang_String_2(MJIEnv env, int objRef, char oldChar, char newChar) {

		if (oldChar == newChar) { // nothing to replace
			return objRef;
		}

		int vref = env.getReferenceField(objRef, "value");
		int off = env.getIntField(objRef, "offset");
		int len = env.getIntField(objRef, "count");

		ElementInfo ei = env.getElementInfo(vref);
		char[] values = ((CharArrayFields) ei.getFields()).asCharArray();
		char[] newValues = null;

		for (int i = off, j = 0; j<len; i++, j++) {
			char c = values[i];
			if (c == oldChar) {
				if (newValues == null) {
					newValues = new char[len];
					if (j>0){
						System.arraycopy(values, off, newValues, 0, j);
					}
				}
				newValues[j] = newChar;
			} else {
				if (newValues != null) {
					newValues[j] = c;
				}
			}
		}

		if (newValues != null) {
			String s = new String(newValues);
			return env.newString(s);

		} else { // oldChar not found, return the original string
				return objRef;
		}
	}
	

	public static boolean matches__Ljava_lang_String_2__Z (MJIEnv env, int objRef, int regexRef){
		String s = env.getStringObject(objRef);
		String r = env.getStringObject(regexRef);

		return s.matches(r);
	}


	public static int replaceFirst__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int regexRef, int replacementRef) {
		String thisStr = env.getStringObject(objRef);
		String regexStr = env.getStringObject(regexRef);
		String replacementStr = env.getStringObject(replacementRef);

		String result = thisStr.replaceFirst(regexStr, replacementStr);
		return (result != thisStr) ? env.newString(result) : objRef;
	}

	public static int replaceAll__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int regexRef, int replacementRef) {
		String thisStr = env.getStringObject(objRef);
		String regexStr = env.getStringObject(regexRef);
		String replacementStr = env.getStringObject(replacementRef);

		String result = thisStr.replaceAll(regexStr, replacementStr);
		return (result != thisStr) ? env.newString(result) : objRef;
	}


	public static int split__Ljava_lang_String_2I___3Ljava_lang_String_2(MJIEnv env, int clsObjRef, int strRef, int limit) {
		String s = env.getStringObject(strRef);
		String obj = env.getStringObject(clsObjRef);

		String[] result = obj.split(s, limit);

		return env.newStringArray(result);
	}

	public static int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env,int clsObjRef,int strRef){
		String s=env.getStringObject(strRef);
		String obj=env.getStringObject(clsObjRef);

		String[] result=obj.split(s);

		return env.newStringArray(result);
	}

	public static int toLowerCase__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int locRef){
		String s = env.getStringObject(objRef);
		Locale loc = JPF_java_util_Locale.getLocale(env, locRef);

		String lower = s.toLowerCase(loc);

		return (s == lower) ? objRef : env.newString(lower);
	}

	public static int toLowerCase____Ljava_lang_String_2 (MJIEnv env, int objRef){
		String s = env.getStringObject(objRef);
		String lower = s.toLowerCase();

		return (s == lower) ? objRef : env.newString(lower);
	}

	public static int toUpperCase__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int locRef){
		String s = env.getStringObject(objRef);
		Locale loc = JPF_java_util_Locale.getLocale(env, locRef);

		String upper = s.toUpperCase(loc);

		return (s == upper) ? objRef : env.newString(upper);
	}


	public static int toUpperCase____Ljava_lang_String_2 (MJIEnv env, int objRef){
		String s = env.getStringObject(objRef);
		String upper = s.toUpperCase();

		return (s == upper) ? objRef : env.newString(upper);
	}

	public static int trim____Ljava_lang_String_2(MJIEnv env,int objRef) {
		Heap heap = env.getHeap();
		ElementInfo thisStr = heap.get(objRef);

		CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
		int thisLength = thisStr.getIntField("count");
		int thisOffset = thisStr.getIntField("offset");
		char thisChars[] = thisFields.asCharArray();

		int start = thisOffset;
		int end = thisOffset + thisLength;

		while ((start < end) && (thisChars[start] <= ' ')){
			start++;
		}

		while ((start < end) && (thisChars[end - 1] <= ' ')){
			end--;
		}

		if (start == thisOffset && end == thisOffset + thisLength){
			// if there was no white space, return the string itself
			return objRef;
		}

		String result = new String(thisChars, start, end - start);
		return env.newString(result);
	}


	public static int toCharArray_____3C (MJIEnv env, int objref){
		int vref = env.getReferenceField(objref, "value");
		int off = env.getIntField(objref, "offset");
		int len = env.getIntField(objref, "count");

		int cref = env.newCharArray(len);

		for (int i=0, j=off; i<len; i++, j++){
			env.setCharArrayElement(cref, i, env.getCharArrayElement(vref, j));
		}

		return cref;
	}

	public static int format__Ljava_lang_String_2_3Ljava_lang_Object_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
			int fmtRef, int argRef){
		return env.newString(env.format(fmtRef,argRef));
	}

	public static int format__Ljava_util_Locale_2Ljava_lang_String_2_3Ljava_lang_Object_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
			int locRef,int fmtRef, int argRef){
		Locale loc = JPF_java_util_Locale.getLocale(env, locRef);
		return env.newString(env.format(loc,fmtRef,argRef));
	}


	public static int intern____Ljava_lang_String_2 (MJIEnv env, int robj) {
		// <2do> Replace this with a JPF space HashSet once we have a String model
		Heap   heap = env.getHeap();

		String s = env.getStringObject(robj);
		robj = heap.newInternString(s, env.getThreadInfo());

		return robj;
	}

	public static int  valueOf__I__Ljava_lang_String_2(MJIEnv env, int clsref,int i){
		String result=String.valueOf(i);
		return env.newString(result);
	}

	public static int  valueOf__J__Ljava_lang_String_2(MJIEnv env, int clsref,long l){
		String result=String.valueOf(l);
		return env.newString(result);
	}

	public static int  valueOf__F__Ljava_lang_String_2(MJIEnv env, int clsref,float f){
		String result=String.valueOf(f);
		return env.newString(result);
	}
	
	public static int  valueOf__D__Ljava_lang_String_2(MJIEnv env, int clsref,double d){
		String result=String.valueOf(d);
		return env.newString(result);
	}


}
