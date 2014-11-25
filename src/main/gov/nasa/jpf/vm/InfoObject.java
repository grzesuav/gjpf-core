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

package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.ObjectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * common root for ClassInfo, MethodInfo, FieldInfo (and maybe more to follow)
 * 
 * so far, it's used to factorize the annotation support, but we can also
 * move the attributes up here
 * 
 * Note this is used for both declaration- and type- annotations since there is
 * a cross-over (type annotations of classes/interfaces are visible through the
 * reflection API that is otherwise just reserved for declaration annotations)
 * 
 * 2do - there are 3 annotation positions that are valid for both type and declaration
 * annotations: class/interface, field and formal method parameters. Of these,
 * only class/interface type annotations can be queried at runtime, i.e. are
 * treated similarly to declaration annotations. It is not clear if this is a
 * Java 8 implementation artifact or intentional
 * 
 * Other than annotating classes/interfaces, type and declaration annotations
 * are kept separate and only the latter ones can be queried from Java, hence
 * we provide a query API that is only accessible from instructions,
 * native peers and listeners. Type annotations are used
 */
public abstract class InfoObject implements Cloneable {

  static AnnotationInfo[] NO_ANNOTATIONS = new AnnotationInfo[0];
  static AbstractTypeAnnotationInfo[] NO_TYPE_ANNOTATIONS = new AbstractTypeAnnotationInfo[0];
  
  // the number of annotations per class/method/field is usually
  // small enough so that simple arrays are more efficient than HashMaps
  protected AnnotationInfo[] annotations = NO_ANNOTATIONS;

  protected AbstractTypeAnnotationInfo[] typeAnnotations = NO_TYPE_ANNOTATIONS;
  
  /** 
   * user defined attribute objects.
   * Note - this is NOT automatically state restored upon backtracking,
   * subclasses have to do this on their own if required
   */
  protected Object attr;
  
  
  public void setAnnotations (AnnotationInfo[] annotations){
    this.annotations = annotations;
  }
  
  public void addAnnotations (AnnotationInfo[] annotations){
    if (annotations == null){
      this.annotations = annotations;
    } else {
      AnnotationInfo[] newAi = new AnnotationInfo[this.annotations.length + annotations.length];
      System.arraycopy(this.annotations,0,newAi, 0, this.annotations.length);
      System.arraycopy(annotations, 0, newAi, this.annotations.length, annotations.length);
      this.annotations = newAi;
    }
  }
  
  public void addAnnotation (AnnotationInfo newAnnotation){
    AnnotationInfo[] ai = annotations;
    if (ai == null){
      ai = new AnnotationInfo[1];
      ai[0] = newAnnotation;

    } else {
      int len = annotations.length;
      ai = new AnnotationInfo[len+1];
      System.arraycopy(annotations, 0, ai, 0, len);
      ai[len] = newAnnotation;
    }

    annotations = ai;
  }

  // to be overridden by ClassInfo because of superclass inhertited annotations
  public boolean hasAnnotations(){
    return (annotations != NO_ANNOTATIONS);
  }
  
  // to be overridden by ClassInfo because of superclass inhertited annotations
  public AnnotationInfo[] getAnnotations() {
    return annotations;
  }
      
  // to be overridden by ClassInfo because of superclass inhertited annotations
  public AnnotationInfo getAnnotation (String name){    
    AnnotationInfo[] ai = annotations;
    if (ai != NO_ANNOTATIONS){
      for (int i=0; i<ai.length; i++){
        if (ai[i].getName().equals(name)){
          return ai[i];
        }
      }
    }
    return null;
  }
  
  public boolean hasAnnotation (String name){
    return getAnnotation(name) != null;    
  }
  
  public AnnotationInfo[] getDeclaredAnnotations(){
    return annotations;
  }

  //--- type annotations
  
  public void setTypeAnnotations (AbstractTypeAnnotationInfo[] typeAnnotations){
    this.typeAnnotations = typeAnnotations;
  }

  public void addTypeAnnotations (AbstractTypeAnnotationInfo[] tas){
    if (typeAnnotations == NO_TYPE_ANNOTATIONS){
      typeAnnotations = tas;
      
    } else {
      int oldLen = typeAnnotations.length;
      AbstractTypeAnnotationInfo[] newTA = new AbstractTypeAnnotationInfo[oldLen + tas.length];
      System.arraycopy(typeAnnotations, 0, newTA, 0, oldLen);
      System.arraycopy(tas, 0, newTA, oldLen, tas.length);
      typeAnnotations = newTA;
    }
  }
  
  public void addTypeAnnotation (AbstractTypeAnnotationInfo newAnnotation){
    AbstractTypeAnnotationInfo[] ai = typeAnnotations;
    if (ai == null){
      ai = new AbstractTypeAnnotationInfo[1];
      ai[0] = newAnnotation;

    } else {
      int len = annotations.length;
      ai = new AbstractTypeAnnotationInfo[len+1];
      System.arraycopy(annotations, 0, ai, 0, len);
      ai[len] = newAnnotation;
    }

    typeAnnotations = ai;
  }

  
  public AbstractTypeAnnotationInfo[] getTypeAnnotations() {
    return typeAnnotations;
  }
  
  public boolean hasTypeAnnotations(){
    return (typeAnnotations != NO_TYPE_ANNOTATIONS);    
  }
  
  public boolean hasTypeAnnotation (String name){
    return getTypeAnnotation(name) != null;    
  }
  
  public AbstractTypeAnnotationInfo getTypeAnnotation (String annoClsName){    
    AbstractTypeAnnotationInfo[] ai = typeAnnotations;
    if (ai != NO_TYPE_ANNOTATIONS){
      for (int i=0; i<ai.length; i++){
        if (ai[i].getName().equals(annoClsName)){
          return ai[i];
        }
      }
    }
    return null;
  }

  public <T extends AbstractTypeAnnotationInfo> List<T> getTargetTypeAnnotations (Class<T> targetType){
    List<T> list = null;
    
    AbstractTypeAnnotationInfo[] ais = typeAnnotations;
    if (ais != NO_TYPE_ANNOTATIONS){
      for (AbstractTypeAnnotationInfo ai : ais){
        if (targetType.isAssignableFrom(ai.getClass())){
          if (list == null){
            list = new ArrayList<>();
          }
          list.add((T)ai);
        }
      }
    }
    
    if (list != null){
      return list;
    } else {
      return Collections.emptyList();
    }
  }
  
  //--- the generic attribute API

  public boolean hasAttr () {
    return (attr != null);
  }

  public boolean hasAttr (Class<?> attrType){
    return ObjectList.containsType(attr, attrType);
  }

  public boolean hasAttrValue (Object a){
    return ObjectList.contains(attr, a);
  }
  
  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   */
  public Object getAttr(){
    return attr;
  }

  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   */
  public void setAttr (Object a){
    attr = a;    
  }

  public void addAttr (Object a){
    attr = ObjectList.add(attr, a);
  }

  public void removeAttr (Object a){
    attr = ObjectList.remove(attr, a);
  }

  public void replaceAttr (Object oldAttr, Object newAttr){
    attr = ObjectList.replace(attr, oldAttr, newAttr);
  }

  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getAttr (Class<T> attrType) {
    return ObjectList.getFirst(attr, attrType);
  }

  public <T> T getNextAttr (Class<T> attrType, Object prev) {
    return ObjectList.getNext(attr, attrType, prev);
  }

  public ObjectList.Iterator attrIterator(){
    return ObjectList.iterator(attr);
  }
  
  public <T> ObjectList.TypedIterator<T> attrIterator(Class<T> attrType){
    return ObjectList.typedIterator(attr, attrType);
  }
}
