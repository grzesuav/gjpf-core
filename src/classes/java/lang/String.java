//
// Copyright (C) 2007 United States Government as represented by the
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

package java.lang;

import java.io.ObjectStreamField;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**

 * MJI adapter for java.lang.String, based on jkd 1.7.0_07 source.
 * Adapted by frank.
 */

public final class String
implements java.io.Serializable, Comparable<String>, CharSequence {

	/** The value is used for character storage. */
	private final char value[];

	/** Cache the hash code for the string */
	private int hash; // Default to 0

	/** use serialVersionUID from JDK 1.0.2 for interoperability */
	private static final long serialVersionUID = -6849794470754667710L;

	/** from SE6 that jpf depends on */
	private final int offset=0;
	private final int count;


	private static final ObjectStreamField[] serialPersistentFields =
			new ObjectStreamField[0];

	public String() {
		this.value = new char[0];
		count=0;
	}

	public String(String original) {
		this.value = original.value;
		this.hash = original.hash;
		count=original.count;
	}

	public String(char value[]) {
		this.value = Arrays.copyOf(value, value.length);
		count=value.length;
	}

	public String(char value[], int offset, int count) {
		String proxy=init(value,offset,count);
		this.value=proxy.value;
		this.count=proxy.count;
		this.hash=proxy.hash;
	}

	private native String init(char[] value, int offset, int count);

	public String(int[] codePoints, int offset, int count) {
		String proxy=init(codePoints,offset,count);
		this.value=proxy.value;
		this.count=proxy.count;
		this.hash=proxy.hash;
	}

	private native String init(int[] codePoints, int offset, int count);

	@Deprecated
	public String(byte ascii[], int hibyte, int offset, int count) {
		String proxy=init(ascii,hibyte,offset,count);
		this.value=proxy.value;
		this.count=proxy.count;
		this.hash=proxy.hash;
	}

	private native String init(byte ascii[], int hibyte, int offset, int count);

	@Deprecated
	public String(byte ascii[], int hibyte) {
		this(ascii, hibyte, 0, ascii.length);
	}



	public String(byte bytes[], int offset, int length, String charsetName){
		String proxy=init(bytes,offset,length,charsetName);
		this.value=proxy.value;
		this.count=proxy.count;
		this.hash=proxy.hash;
	}

	private native String init(byte bytes[], int offset, int length, String charsetName);


	public String(byte x[], int offset, int length, Charset cset) {
		// no Charset model
		if (cset == null){
			throw new NullPointerException("cset");
		}
		if (length < 0){
			throw new StringIndexOutOfBoundsException(length);
		}
		if (offset < 0){
			throw new StringIndexOutOfBoundsException(offset);
		}
		if (offset > x.length - length){
			throw new StringIndexOutOfBoundsException(offset + length);
		}


		this.value =  StringCoding.decode(cset, x, offset, length);
		this.count=this.value.length;
	}

	private native String init(byte bytes[], int offset, int length, Charset charset);


	public String(byte bytes[], String charsetName)
			throws UnsupportedEncodingException {
		this(bytes, 0, bytes.length, charsetName);
	}

	public String(byte bytes[], Charset charset) {
		this(bytes, 0, bytes.length, charset);
	}

	public String(byte bytes[], int offset, int length) {
		String proxy=init(bytes,offset,length);
		this.value=proxy.value;
		this.count=proxy.count;
		this.hash=proxy.hash;
	}


	private native String init(byte bytes[], int offset, int length);

	public String(byte bytes[]) {
		this(bytes, 0, bytes.length);
	}


	public String(StringBuffer x) {
		// no StringBuffer model
		synchronized(x) {
			this.value = Arrays.copyOf(x.getValue(), x.length());
			this.count=this.value.length;
		}

	}
	private native String init(StringBuffer buffer);


	public String(StringBuilder x) {
		// no StringBuilder model
		this.value = Arrays.copyOf(x.getValue(), x.length());
		this.count=this.value.length;	       
	}


	private native String init(StringBuilder builder);

	@Deprecated
	String(int offset, int count, char[] value) {
		this(value, offset, count);
	}
	public int length() {
		return value.length;
	}
	public boolean isEmpty() {
		return value.length == 0;
	}
	public char charAt(int index) {
		if ((index < 0) || (index >= value.length)) {
			throw new StringIndexOutOfBoundsException(index);
		}
		return value[index];
	}

	native public int codePointAt(int index);
	native public int codePointBefore(int index);
	native public int codePointCount(int beginIndex, int endIndex);
	native public int offsetByCodePoints(int index, int codePointOffset);
	native public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin);

	@Deprecated
	native public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin);
	native public byte[] getBytes(String charsetName)
			throws UnsupportedEncodingException;

	public byte[] getBytes(Charset x){
		// No Charset model.
		if (x == null){
			throw new NullPointerException();
		}
		return StringCoding.encode(x, value, 0, value.length);
	}

	native public byte[] getBytes();
	native public boolean equals(Object anObject);
	public boolean contentEquals(StringBuffer stringBuffer){
		// No StringBuffer model.
		synchronized (stringBuffer) {
			return contentEquals((CharSequence) stringBuffer);
		}
	}

	public boolean contentEquals(CharSequence charSequence){
		// No CharSequence model.
		if (value.length != charSequence.length())
			return false;
		// Case: StringBuffer, StringBuilder
		if (charSequence instanceof AbstractStringBuilder) {
			char a[] = value;
			char b[] = ((AbstractStringBuilder) charSequence).getValue();
			int i = 0;
			int n = value.length;
			while (n-- != 0) {
				if (a[i] != b[i])
					return false;
				i++;
			}
			return true;
		}
		// Case: String
		if (charSequence.equals(this)){
			return true;
		}
		// Case: CharSequence
		char v1[] = value;
		int i = 0;
		int n = value.length;
		while (n-- != 0) {
			if (v1[i] != charSequence.charAt(i))
				return false;
			i++;
		}
		return true;

	}

	native public boolean equalsIgnoreCase(String anotherString);
	native public int compareTo(String anotherString);

	public static final Comparator<String> CASE_INSENSITIVE_ORDER
	= new CaseInsensitiveComparator();
	private static class CaseInsensitiveComparator
	implements Comparator<String>, java.io.Serializable {
		// use serialVersionUID from JDK 1.2.2 for interoperability
		private static final long serialVersionUID = 8575799808933029326L;

		public int compare(String s1, String s2) {
			return MJIcompare(s1,s2);
		}
	}

	native private static int MJIcompare(String s1,String s2);
	public int compareToIgnoreCase(String str) {
		return CASE_INSENSITIVE_ORDER.compare(this, str);
	}

	native public boolean regionMatches(int toffset, String other, int ooffset,
			int len);
	native public boolean regionMatches(boolean ignoreCase, int toffset,
			String other, int ooffset, int len);
	native public boolean startsWith(String prefix, int toffset);
	public boolean startsWith(String prefix) {
		return startsWith(prefix, 0);
	}

	public boolean endsWith(String suffix) {
		return startsWith(suffix, value.length - suffix.value.length);
	}

	native public int hashCode();
	public int indexOf(int ch) {
		return indexOf(ch, 0);
	}
	native public int indexOf(int ch, int fromIndex);
	native public int lastIndexOf(int ch);
	native public int lastIndexOf(int ch, int fromIndex);
	native public int indexOf(String str);
	native public int indexOf(String str, int fromIndex);

	public int lastIndexOf(String str) {
		return lastIndexOf(str, value.length);
	}
	native public int lastIndexOf(String str, int fromIndex);
	native public String substring(int beginIndex);
	native public String substring(int beginIndex, int endIndex);
	public CharSequence subSequence(int beginIndex, int endIndex) {
		return this.substring(beginIndex, endIndex);
	}
	native public String concat(String str);
	native public String replace(char oldChar, char newChar);
	native public boolean matches(String regex);
	public boolean contains(CharSequence charSequence) {
		// No CharSequence model
		return indexOf(charSequence.toString()) > -1;
	}
	native public String replaceFirst(String regex, String replacement);
	native public String replaceAll(String regex, String replacement);
	public String replace(CharSequence target, CharSequence other) {
		// No CharSequence model
		int PATTERN= 0x10;
		Matcher pattern=Pattern.compile(target.toString(), PATTERN).matcher(this);
		return pattern.replaceAll(Matcher.quoteReplacement(other.toString()));
	}
	native public String[] split(String regex, int limit);
	native public String[] split(String regex);
	native public String toLowerCase(Locale locale);
	native public String toLowerCase();
	native public String toUpperCase(Locale locale);
	native public String toUpperCase();
	native public String trim();
	public String toString() {
		return this;
	}
	native public char[] toCharArray();
	native public static String format(String format, Object... args);
	native public static String format(Locale l, String format, Object... args);
	public static String valueOf(Object x){
		// can't translate arbitrary object
		return (x == null) ? "null" : x.toString();
	}
	public static String valueOf(char values[]) {
		return new String(values);
	}
	public static String valueOf(char values[], int offset, int count) {
		return new String(values, offset, count);
	}
	public static String copyValueOf(char values[], int offset, int count) {
		return new String(values, offset, count);
	}
	public static String copyValueOf(char values[]) {
		return new String(values);
	}
	public static String valueOf(boolean bool) {
		return bool ? "true" : "false";
	}
	public static String valueOf(char character) {
		char data[] = {character};
		return new String(data);
	}
	native public static String valueOf(int i);
	native public static String valueOf(long l);
	native public static String valueOf(float f);
	native public static String valueOf(double d);
	public native String intern();

}
