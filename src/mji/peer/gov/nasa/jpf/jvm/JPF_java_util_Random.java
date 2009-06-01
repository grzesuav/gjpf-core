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

import java.util.Random;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ConfigChangeListener;

/**
 * MJI NativePeer class for java.util.Random library abstraction
 *
 * (we need to cut off the static class init since it pulls in a lot
 * of classes requiring native methods)
 *
 * <2do> - we should add a property if the nextXX'es should be turned
 * into choice points, and if yes, how. In general, it is probably preferable
 * to leave them single-valued, but deterministic
 */
public class JPF_java_util_Random {

  static final long multiplier = 0x5DEECE66DL;
  static final long addend     = 0xBL;
  static final long mask       = (1L << 48) - 1;

  static class ConfigListener implements ConfigChangeListener {

    public void propertyChanged(Config conf, String key, String oldValue, String newValue) {
      if ("cg.enumerate_random".equals(key)) {
        setEnumerateRandom(conf);
      }
    }
  }

  static boolean enumerateRandom;

  public static void init (Config conf) {
    setEnumerateRandom(conf);
    conf.addChangeListener(new ConfigListener());
  }

  static void setEnumerateRandom (Config conf) {
    enumerateRandom = conf.getBoolean("cg.enumerate_random", false);

    if (enumerateRandom){
      JPF_gov_nasa_jpf_jvm_Verify.init(conf);
    }    
  }
  
  public static void $clinit____V (MJIEnv env, int rcls) {
    // don't let this one pass, it pulls the ObjectStreams
  }


  // internal helpers to simulate Random operations, Unfortunately we
  // can't just delegate to a real Random object because there is no decent
  // way to get the updated seed value back

  static long nextSeed (long seed){
    return (seed * multiplier + addend) & mask;
  }

  static int next (long seed, int bits) {
    return (int)(seed >>> (48 - bits));
  }

  // that's a more convenient wrapper if we don't have repeated calls
  static int next (MJIEnv env, int objref, int bits) {
    int atomicLongRef = env.getReferenceField(objref, "seed");
    long old = env.getLongField(atomicLongRef, "value");

    long next = (old * multiplier + addend) & mask;

    env.setLongField(atomicLongRef, "value", next);

    return (int)(next >>> (48 - bits));
  }

  static long getSeed (MJIEnv env, int objref){
    int atomicLongRef = env.getReferenceField(objref, "seed");
    return env.getLongField(atomicLongRef, "value");
  }

  static void setSeed (MJIEnv env, int objref, long seed){
    int atomicLongRef = env.getReferenceField(objref, "seed");
    env.setLongField(atomicLongRef, "value", seed);
  }

  public static int nextInt____I (MJIEnv env, int objref) {
    int r = 0;
    if (enumerateRandom) {
      // <2do>
    } else {
      r = next(env,objref,32);
    }

    return r;
  }

  public static double nextDouble____D (MJIEnv env, int objref) {
    double r = 0;
    if (enumerateRandom) {
      // <2do>
    } else {
      r = (((long)(next(env,objref, 26)) << 27)
                   + next(env,objref, 27)) / (double)(1L << 53);
    }

    return r;
  }

  public static double nextGaussian____D (MJIEnv env, int objref) {
    double r = 0;
    if (enumerateRandom) {
      // <2do>

    } else {
      // not sure if we really want to keep this fidelity, just in case the
      // fields are used anywhere. After all, those fields are private

      if (env.getBooleanField(objref, "haveNextNextGaussian")) {
        env.setBooleanField(objref, "haveNextNextGaussian", false);
        r = env.getDoubleField(objref, "nextNextGaussian");
      } else {
        double a,b,c;

        do {
          a = 2*nextDouble____D(env,objref) - 1; // <2do> that's inefficient!
          b = 2*nextDouble____D(env,objref) - 1;
          c = a*a + b*b;
        } while (a >= 1 || c == 0);

        r = StrictMath.sqrt( -2 * StrictMath.log(c) / c);
        env.setDoubleField(objref, "nextNextGaussian", r * b);
        env.setBooleanField(objref, "haveNextNextGaussian", true);

        r *= a;
      }
    }

    return r;
  }



  public static int nextInt__I__I (MJIEnv env, int objref, int n) {
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_jvm_Verify.getInt__II__I(env,-1,0,n-1);

    } else {
      long seed = getSeed(env, objref);

      if ((n & -n) == n) {
        seed = nextSeed(seed);
        setSeed(env, objref, seed);
        return (int)((n * (long)next(seed,31)) >> 31);

      } else {

        int bits, v;
        do {
          seed = nextSeed(seed);
          bits = next(seed, 31);
          v = bits % n;
        } while(bits - v + (n-1) < 0);

        setSeed(env, objref, seed);
        return v;
      }
    }
  }

  public static boolean nextBoolean____Z (MJIEnv env, int objref){
    if (enumerateRandom){
      return JPF_gov_nasa_jpf_jvm_Verify.getBoolean____Z(env,-1);

    } else {
      long seed = getSeed(env, objref);
      seed = nextSeed(seed);
      setSeed(env,objref,seed);
      return (next(seed,1) != 0);
    }
  }

}
