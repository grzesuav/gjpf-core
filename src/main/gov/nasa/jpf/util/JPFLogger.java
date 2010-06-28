//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this is a decorator for java.util.logging.JPFLogger
 *
 * We use this to avoid explicit Logger.isLoggable() checks in the code.
 * The goal is to avoid time & memory overhead if logging is not enabled.
 *
 * We provide a fat interface to avoid Object[] creation for ellipsis method
 * or auto boxing for Object arguments
 *
 * <2do> implement missing public methods
 */
public class JPFLogger extends Logger {

  final static int OFF = Level.OFF.intValue();
  final static int SEVERE = Level.SEVERE.intValue();
  final static int WARNING = Level.WARNING.intValue();
  final static int INFO = Level.INFO.intValue();
  final static int FINE = Level.FINE.intValue();
  final static int FINER = Level.FINER.intValue();
  final static int FINEST = Level.FINEST.intValue();

  // this is what we
  protected Logger logger;

  protected volatile int levelValue; // unfortunately Logger.levelValue is private

  public JPFLogger (Logger logger){
    super(logger.getName(), logger.getResourceBundleName());

    this.logger = logger;
    levelValue = logger.getLevel().intValue();
    if (levelValue == OFF){
       // we map this to negative to save a comparison
      levelValue = Integer.MIN_VALUE;
    }
  }

  public String getName() {
    return logger.getName();
  }

  public boolean isLoggable (Level level){
    return (levelValue <= level.intValue() && levelValue != Integer.MIN_VALUE);
  }

  public void setLevel (Level newLevel){
    logger.setLevel(newLevel);
    levelValue = newLevel.intValue();
    if (levelValue == OFF){
      levelValue = Integer.MIN_VALUE;
    }
  }

  private void log (Level level, Object... args){
    StringBuilder sb = new StringBuilder(256);
    for (Object a : args) {
      sb.append(a);
    }
    logger.log(level, sb.toString());
  }



  //--- since SEVERE is the highest level, there is not much we can save
  public void severe (String msg){
    logger.log(Level.SEVERE, msg);
  }

  //--- the WARNING
  public void warning (String msg){
    // no use to check - it's going to be rechecked anyways
    logger.log(Level.WARNING, msg);
  }
  public void warning (Object s1, Object s2){
    if (levelValue >= WARNING){
      logger.log(Level.WARNING, s1.toString() + s2.toString());
    }
  }
  // this is here to avoid auto boxing
  public void warning (Object s1, int s2){
    if (levelValue >= WARNING){
      logger.log(Level.WARNING, s1.toString() + s2);
    }
  }
  public void warning (Object s1, Object s2, Object s3){
    if (levelValue >= WARNING){
      logger.log(Level.WARNING, s1.toString() + s2.toString() + s3.toString());
    }
  }
  public void warning (Object s1, Object s2, Object s3, Object s4){
    if (levelValue >= WARNING){
      logger.log(Level.WARNING, s1.toString() + s2.toString() + s3.toString() + s4.toString());
    }
  }
  public void warning (Object s1, int s2, Object s3, int s4){
    if (levelValue >= WARNING){
      logger.log(Level.WARNING, s1.toString() + s2 + s3.toString() + s4);
    }
  }
  public void warning (Object... args){
    if (levelValue >= WARNING){
      log(Level.WARNING, args);
    }
  }
  // note this still wraps args into a String array - overhead
  public void fwarning (String format, String... args){
    if (levelValue >= WARNING){
      logger.log(Level.WARNING, String.format(format, (Object)args));
    }
  }

  //--- the INFO
  public void info (String msg){
    logger.log(Level.INFO, msg);
  }
  public void info (Object s1, Object s2){
    if (levelValue >= INFO){
      logger.log(Level.INFO, s1.toString() + s2.toString());
    }
  }
  public void info (Object s1, int s2){
    if (levelValue >= INFO){
      logger.log(Level.INFO, s1.toString() + s2);
    }
  }
  public void info (Object s1, Object s2, Object s3){
    if (levelValue >= INFO){
      logger.log(Level.INFO, s1.toString() + s2.toString() + s3.toString());
    }
  }
  public void info (Object s1, Object s2, Object s3, Object s4){
    if (levelValue >= INFO){
      logger.log(Level.INFO, s1.toString() + s2.toString() + s3.toString() + s4.toString());
    }
  }
  public void info (Object s1, int s2, Object s3, int s4){
    if (levelValue >= INFO){
      logger.log(Level.INFO, s1.toString() + s2 + s3.toString() + s4);
    }
  }
  public void info (Object... args){
    if (levelValue >= INFO){
      log(Level.INFO, args);
    }
  }
  // note this still wraps args into a String array - overhead
  public void finfo (String format, String... args){
    if (levelValue >= INFO){
      logger.log(Level.INFO, String.format(format, (Object)args));
    }
  }

  public void fine (String msg){
    logger.log(Level.FINE, msg);
  }
  public void fine (Object s1, Object s2){
    if (levelValue >= FINE){
      logger.log(Level.FINE, s1.toString() + s2.toString());
    }
  }
  public void fine (Object s1, int s2){
    if (levelValue >= FINE){
      logger.log(Level.FINE, s1.toString() + s2);
    }
  }
  public void fine (Object s1, Object s2, Object s3){
    if (levelValue >= FINE){
      logger.log(Level.FINE, s1.toString() + s2.toString() + s3.toString());
    }
  }
  public void fine (Object s1, Object s2, Object s3, Object s4){
    if (levelValue >= FINE){
      logger.log(Level.FINE, s1.toString() + s2.toString() + s3.toString() + s4.toString());
    }
  }
  public void fine (Object s1, int s2, Object s3, int s4){
    if (levelValue >= FINE){
      logger.log(Level.FINE, s1.toString() + s2 + s3.toString() + s4);
    }
  }
  public void fine (Object... args){
    if (levelValue >= FINE){
      log(Level.FINE, args);
    }
  }
  // note this still wraps args into a String array - overhead
  public void ffine (String format, String... args){
    if (levelValue >= FINE){
      logger.log(Level.FINE, String.format(format, (Object)args));
    }
  }

  //--- the FINER
  public void finer (String msg){
    logger.log(Level.FINER, msg);
  }
  public void finer (Object s1, Object s2){
    if (levelValue >= FINER ){
      logger.log(Level.FINER, s1.toString() + s2.toString());
    }
  }
  public void finer (Object s1, int s2){
    if (levelValue >= FINER ){
      logger.log(Level.FINER, s1.toString() + s2);
    }
  }
  public void finer (Object s1, Object s2, Object s3){
    if (levelValue >= FINER ){
      logger.log(Level.FINER, s1.toString() + s2.toString() + s3.toString());
    }
  }
  public void finer (Object s1, Object s2, Object s3, Object s4){
    if (levelValue >= FINER ){
      logger.log(Level.FINER, s1.toString() + s2.toString() + s3.toString() + s4.toString());
    }
  }
  public void finer (Object s1, int s2, Object s3, int s4){
    if (levelValue >= FINER ){
      logger.log(Level.FINER, s1.toString() + s2 + s3.toString() + s4);
    }
  }
  public void finer (Object... args){
    if (levelValue >= FINER){
      log(Level.FINER, args);
    }
  }
  // note this still wraps args into a String array - overhead
  public void ffiner (String format, String... args){
    if (levelValue >= FINER ){
      logger.log(Level.FINER, String.format(format, (Object)args));
    }
  }

  //--- the FINEST
  public void finest (String msg){
    logger.log(Level.FINEST, msg);
  }
  public void finest (Object s1, Object s2){
    if (levelValue >= FINEST ){
      logger.log(Level.FINEST, s1.toString() + s2.toString());
    }
  }
  public void finest (Object s1, int s2){
    if (levelValue >= FINEST ){
      logger.log(Level.FINEST, s1.toString() + s2);
    }
  }
  public void finest (Object s1, Object s2, Object s3){
    if (levelValue >= FINEST ){
      logger.log(Level.FINEST, s1.toString() + s2.toString() + s3.toString());
    }
  }
  public void finest (Object s1, Object s2, Object s3, Object s4){
    if (levelValue >= FINEST ){
      logger.log(Level.FINEST, s1.toString() + s2.toString() + s3.toString() + s4.toString());
    }
  }
  public void finest (Object s1, int s2, Object s3, int s4){
    if (levelValue >= FINEST ){
      logger.log(Level.FINEST, s1.toString() + s2 + s3.toString() + s4);
    }
  }
  public void finest (Object... args){
    if (levelValue >= FINEST){
      log(Level.FINEST, args);
    }
  }
  // note this still wraps args into a String array - overhead
  public void ffinest (String format, String... args){
    if (levelValue >= FINEST ){
      logger.log(Level.FINEST, String.format(format, (Object)args));
    }
  }


}
