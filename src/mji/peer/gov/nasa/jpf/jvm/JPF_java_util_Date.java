package gov.nasa.jpf.jvm;

import java.util.Date;

public class JPF_java_util_Date {

  static Date getDate (MJIEnv env, int dateRef){
    
    //<2do> that doesn't handle BaseCalendar.Date cdate yet
    long t = env.getLongField(dateRef, "fastTime");
    return new Date(t);
  }
}
