//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.util.event;

import gov.nasa.jpf.vm.MJIEnv;

/**
 * a pseudo event that encapsulates a (possibly composed) check
 * 
 * This event type uses 'alt' for disjunction and 'next' for conjunction if
 * they point to CheckEvents
 */
public abstract class CheckEvent extends Event {
  
  protected CheckEvent (String name, Object... arguments){
    super(name, arguments);
  }
  
  public abstract boolean evaluate(MJIEnv env);
  
  public boolean check (MJIEnv env){
    if (!evaluate(env)){
      if (alt != null && alt instanceof CheckEvent){
        return ((CheckEvent)alt).check(env);
      } else {
        return false;
      }
      
    } else {
      if (next != null && next instanceof CheckEvent){
        return ((CheckEvent)next).check(env);
      } else {
        return true;
      }
    }
  }
  
  public CheckEvent or (CheckEvent orCheck){
    addAlternative(orCheck);
    
    return this;
  }
  
  public CheckEvent and (CheckEvent andCheck){
    addNext( andCheck);
    
    return this;
  }
  
  public String getExpression(){
    if (alt == null && !(next instanceof CheckEvent)){
      return toString();
      
    } else {
      StringBuilder sb = new StringBuilder();
      
      sb.append('(');
      sb.append(name);
      
      for (Event e = alt; e != null; e = e.alt){
        sb.append( " || ");
        sb.append(e.name);
      }
      
      for (Event e = next; e instanceof CheckEvent; e = e.next){
        sb.append( " && ");
        sb.append(e.name);        
      }
      
      sb.append(')');
      
      return sb.toString();
    }
  }
}
