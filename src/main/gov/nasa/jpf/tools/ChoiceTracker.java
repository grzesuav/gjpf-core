package gov.nasa.jpf.tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;

/**
 * generic choice tracker tool, to produce a list of choice values that
 * can be used to create readable replay scripts etc.
 */
public class ChoiceTracker extends ListenerAdapter implements PublisherExtension {

  Config config;
  JVM vm;
  Search search;
  
  protected PrintWriter pw;
  Class<?>[] cgClasses;
  boolean isReportExtension; 
  
  String[] excludes;
  
  public ChoiceTracker (JPF jpf, String traceFileName, Class<?> cgClass){
    config = jpf.getConfig();
    vm = jpf.getVM();
    search = jpf.getSearch();
    
    cgClasses = new Class<?>[1];
    cgClasses[0] = cgClass;
    
    try {
      pw = new PrintWriter(traceFileName);
    } catch (FileNotFoundException fnfx) {
      System.err.println("cannot write choice trace to file: " + traceFileName);
      pw = new PrintWriter(System.out);
    }
  }

  public ChoiceTracker (Config config, JPF jpf) {
    this.config = config;
    vm = jpf.getVM();
    search = jpf.getSearch();
    
    String fname = config.getString("choice.trace");
    if (fname == null) {
      isReportExtension = true;
      jpf.addPublisherExtension(ConsolePublisher.class, this);
      // pw is going to be set later
    } else {
      try {
        pw = new PrintWriter(fname);
      } catch (FileNotFoundException fnfx) {
        System.err.println("cannot write choice trace to file: " + fname);
        pw = new PrintWriter(System.out);
      }
    }
    
    excludes = config.getStringArray("choice.exclude");
    cgClasses = config.getClasses("choice.class");
  }

  public void setExcludes (String... ex) {
    excludes=ex;
  }
  
  boolean isRelevantCG (ChoiceGenerator cg){
    if (cgClasses == null){
      return true;
    } else {
      for (Class<?> cls : cgClasses){
        if (cls.isAssignableFrom(cg.getClass())){
          return true;
        }
      }
      
      return false;
    }
  }

  public void propertyViolated (Search search) {
        
    if (!isReportExtension) {

      pw.print("// application: ");
      pw.print(config.getTarget());
      for (String s : config.getTargetArgs()) {
        pw.print(s);
        pw.print(' ');
      }
      pw.println();

      if (cgClasses == null) {
        pw.println("// trace over all CG classes");
      } else {
        pw.print("// trace over CG types: ");
        for (Class<?> cls : cgClasses){
          pw.print(cls.getName());
          pw.print(' ');
        }
        pw.println();
      }

      pw.println("//------------------------- choice trace");
      printChoices();
      
      pw.println("//------------------------- end choice trace");
      pw.flush();
    }
  }

  void printChoices () {
    SystemState ss = vm.getSystemState();
    ChoiceGenerator[] cgStack = ss.getChoiceGenerators();

    nextChoice:
    for (ChoiceGenerator cg : cgStack) {
      if (isRelevantCG(cg) && !cg.isDone()){
        Object o = cg.getNextChoice();
        if (o != null) {
          Object choice = cg.getNextChoice();
          
          if (excludes != null){
            for (String e : excludes) {
              if (choice.toString().startsWith(e)) continue nextChoice;
            }
          }
          
          pw.println(choice);
        }
      }
    }
  }

  //--- the PublisherExtension interface

  public void publishPropertyViolation (Publisher publisher) {
    pw = publisher.getOut();
    publisher.publishTopicStart("choice trace " + publisher.getLastErrorId());
    printChoices();
  }

}
