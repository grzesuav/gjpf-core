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
package gov.nasa.jpf.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFListener;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.ObjArray;

/**
 * this is our default report generator, which is heavily configurable
 * via our standard properties. Note this gets instantiated and
 * registered automatically via JPF.addListeners(), so you don't
 * have to add it explicitly
 */

public class Reporter extends ListenerAdapter {

  public static Logger log = JPF.getLogger("gov.nasa.jpf.report");

  protected Config conf;
  protected JPF jpf;
  protected Search search;
  protected JVM vm;

  protected Date started, finished;
  protected Statistics stat; // the object that collects statistics
  protected Publisher[] publishers;

  public Reporter (Config conf, JPF jpf) {
    this.conf = conf;
    this.jpf = jpf;
    search = jpf.getSearch();
    vm = jpf.getVM();
    boolean reportStats = false;

    started = new Date();

    publishers = createPublishers(conf);

    for (Publisher publisher : publishers) {
      if (reportStats || publisher.hasToReportStatistics()) {
        reportStats = true;
      }

      if (publisher instanceof JPFListener) {
        jpf.addListener((JPFListener)publisher);
      }
    }

    if (reportStats){
      stat = conf.getInstance("jpf.report.statistics.class", Statistics.class);
      if (stat == null){
        stat = new Statistics();
      }

      jpf.addListener(stat);
    }
  }

  Publisher[] createPublishers (Config conf) {
    String[] def = { "gov.nasa.jpf.report.ConsolePublisher" };
    ArrayList<Publisher> list = new ArrayList<Publisher>();

    Class<?>[] argTypes = { Config.class, Reporter.class };
    Object[] args = { conf, this };

    for (String id : conf.getStringArray("jpf.report.publisher", def)){
      Publisher p = conf.getInstance("jpf.report." + id + ".class",
                                     Publisher.class, argTypes, args);
      if (p != null){
        list.add(p);
      } else {
        log.warning("could not instantiate publisher class: " + id);
      }
    }

    return list.toArray(new Publisher[list.size()]);
  }

  public void addListener (JPFListener listener) {
    jpf.addListener(listener);
  }

  public Publisher[] getPublishers() {
    return publishers;
  }

  public boolean hasToReportTrace() {
    for (Publisher p : publishers) {
      if (p.hasTopic("trace")) {
        return true;
      }
    }

    return false;
  }

  public boolean hasToReportOutput() {
    for (Publisher p : publishers) {
      if (p.hasTopic("output")) {
        return true;
      }
    }

    return false;
  }


  public <T extends Publisher> boolean addPublisherExtension (Class<T> publisherCls, PublisherExtension e) {
    for (Publisher p : publishers) {
      if (publisherCls.isInstance(p)) {
        p.addExtension(e);
        return true;
      }
    }

    return false;
  }

  public <T extends Publisher> void setPublisherTopics (Class<T> publisherCls,
                                                        int category, String[] topics){
    for (Publisher p : publishers) {
      if (publisherCls.isInstance(p)) {
        p.setTopics(category,topics);
        return;
      }
    }
  }

  boolean contains (String key, String[] list) {
    for (String s : list) {
      if (s.equalsIgnoreCase(key)){
        return true;
      }
    }
    return false;
  }


  //--- the publishing phases
  
  protected void publishStart() {
    for (Publisher publisher : publishers) {
      publisher.openChannel();
      publisher.publishProlog();
      publisher.publishStart();
    }
  }

  protected void publishTransition() {
    for (Publisher publisher : publishers) {
      publisher.publishTransition();
    }
  }

  protected void publishPropertyViolation() {
    for (Publisher publisher : publishers) {
      publisher.publishPropertyViolation();
    }
  }

  protected void publishConstraintHit() {
    for (Publisher publisher : publishers) {
      publisher.publishConstraintHit();
    }
  }

  protected void publishFinished() {
    for (Publisher publisher : publishers) {
      publisher.publishFinished();
      publisher.publishEpilog();
      publisher.closeChannel();
    }
  }

  //--- the listener interface that drives report generation

  public void searchStarted (Search search){
    publishStart();
  }

  public void stateAdvanced (Search search) {
    publishTransition();
  }

  public void searchConstraintHit(Search search) {
    publishConstraintHit();
  }


  public void propertyViolated (Search search) {
    publishPropertyViolation();
  }

  public void searchFinished (Search search){
    finished = new Date();

    publishFinished();
  }


  //--- various getters
  
  public Date getStartDate() {
    return started;
  }

  public Date getFinishedDate () {
    return finished;
  }
    
  public JVM getVM() {
    return vm;
  }

  public Search getSearch() {
    return search;
  }

  public List<Error> getErrors () {
    return search.getErrors();
  }

  public Error getLastError () {
    return search.getLastError();
  }

  public String getLastSearchConstraint () {
    return search.getLastSearchContraint();
  }

  public String getLastErrorId () {
    Error e = getLastError();
    if (e != null) {
      return "#" + e.getId();
    } else {
      return "";
    }
  }

  public int getNumberOfErrors() {
    return search.getErrors().size();
  }

  public Statistics getStatistics() {
    return stat;
  }

  public Statistics getStatisticsSnapshot () {
    return stat.clone();
  }
  
  /**
   * in ms
   */
  public long getElapsedTime () {
    Date d = (finished != null) ? finished : new Date();
    long t = d.getTime() - started.getTime();
    return t;
  }

  public Path getPath (){
    return vm.getClonedPath();
  }

  public String getJPFBanner () {
    String s = "JavaPathfinder v" + JPF.VERSION + " - (C) 1999-2007 RIACS/NASA Ames Research Center";
    
    if (conf.getBoolean("jpf.report.show_repository", false)) {
      String repInfo =  getRepositoryInfo();
      if (repInfo != null) {
        s += repInfo;
      }
    }
    
    return s;
  }

  /**
   * get SVN repository info from file system
   */
  String getRepositoryInfo () {
    String dir = conf.getString("jpf.basedir");
    if (dir != null) {
      dir += File.separatorChar + ".svn";
    } else {
      dir = ".svn";
    }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    try {
      File f = new File(dir, "entries");
      if (f.exists()) {
        FileInputStream fis = new FileInputStream(f);
        BufferedReader br = new BufferedReader( new InputStreamReader(fis));
        for (String line = br.readLine(); line != null; line=br.readLine()) {
          if (line.equals("dir")) {
            if ((line = br.readLine()) != null) {
              pw.println();
              pw.print("(r");
              pw.print(line);
              
              if ((line = br.readLine()) != null) {
                String repository = line;

                // repository root & blank lines
                br.readLine();
                while ((line = br.readLine()) != null && line.equals(""));

                if (line.matches("\\d\\d\\d\\d-\\d\\d-\\d\\dT.*")) {
                  String dtg = line.substring(0,19);
                  pw.print(" ");
                  pw.print(dtg);
                  pw.println(')');
                  pw.print(repository);
                }
              } else {
                pw.print(')');
              }
              break;
            }
          }
        }
        
        return sw.toString();
      }  
    } catch (IOException iox) {
      // nothing - we just don't know
    }
    
    return null;
  }
  
  public String getHostName () {
    try {
      InetAddress in = InetAddress.getLocalHost();
      String hostName = in.getHostName();
      return hostName;
    } catch (Throwable t) {
      return "localhost";
    }
  }

  public String getUser() {
    return System.getProperty("user.name");
  }

  public String getSuT() {
    // it would be better to know from where we loaded the class file, but BCEL doesn't tell us
    String mainCls = vm.getMainClassName();
    ClassInfo ciMain = ClassInfo.getClassInfo(mainCls);
    return ciMain.getSourceFileName();
  }
  
  public String getJava (){
    String vendor = System.getProperty("java.vendor");
    String version = System.getProperty("java.version");
    return vendor + "/" + version;
  }

  public String getArch () {
    String arch = System.getProperty("os.arch");
    Runtime rt = Runtime.getRuntime();
    String type = arch + "/" + rt.availableProcessors();

    return type;
  }

  public String getOS () {
    String name = System.getProperty("os.name");
    String version = System.getProperty("os.version");
    return name + "/" + version;
  }

}
