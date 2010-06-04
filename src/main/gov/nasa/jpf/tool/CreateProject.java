//
// Copyright (C) 2009 United States Government as represented by the
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

package gov.nasa.jpf.tool;

import gov.nasa.jpf.util.Printable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * tool to create a JPF project directory with subdirs and standard files
 */
public class CreateProject {

  public static class Exception extends RuntimeException {
    Exception (String details){
      super(details);
    }
  }

  static final String[] defaultSrcDirs = { "main", "peers", "annotations", "classes", "tests", "examples" };

  File coreDir;

  String projectName;
  File projectDir;

  File srcDir;
  ArrayList<File> srcDirs = new ArrayList<File>();    // what we keep within srcDir

  File libDir;
  File toolsDir;
  File binDir;

  // optional dirs
  File nbDir;
  File eclipseDir;

  //--- the public API


  public static void main (String[] args){
    if (args.length < 2){
      System.out.println("usage: java gov.nasa.jpf.tools.CreateProject <jpf-core-path> <new-project-path>");
      return;
    }

    String corePath = args[0];
    String projectPath = args[1];

    CreateProject creator = new CreateProject(corePath, projectPath);

    if (args.length > 2){
      for (int i=2; i<args.length; i++){
        creator.addSrcDir(args[i]);
      }
    }

    creator.createProject();
  }

  public CreateProject (String corePath, String projectPath){
    coreDir = new File(corePath);
    projectDir = new File(coreDir, projectPath);

    projectName = projectDir.getName();

    srcDir = new File(projectDir, "src");
    libDir = new File(projectDir, "lib");
    toolsDir = new File(projectDir, "tools");
    binDir = new File(projectDir, "bin");

    eclipseDir = new File(projectDir, "eclipse");
    nbDir = new File(projectDir, "nbproject");
  }

  public void setCoreDir (String coreDirName){
    coreDir = new File(coreDirName);
  }

  public void addSrcDirs (String[] srcDirNames){
    for (String srcDirName : srcDirNames){
      addSrcDir(srcDirName);
    }
  }

  public void addSrcDir (String srcDirName){
    File sd = new File(srcDir, srcDirName);
    srcDirs.add(sd);
  }

  public void addEclipseDir () {
    eclipseDir = new File(projectDir, "eclipse");
  }

  public void addNetBeansDir (){
    nbDir = new File(projectDir, "nbproject");
  }

  public void createProject () throws CreateProject.Exception {

    // does the project dir already exist
    //if (projectDir.isDirectory()){
    //  throw new Exception("project directory already exists: " + projectDir.getPath());
    //}

    // if we didn't set them explicitly, use the defaults
    if (srcDirs.isEmpty()){
      addSrcDirs(defaultSrcDirs);
    }

    createDirectories();
    createAntScript();
    createProjectProperties();
    createScripts();
    createEclipseFiles();
    createNbFiles();
    copyTools();
  }

  //--- our internal creators/initializers

  void createDirectories (){
    projectDir.mkdirs();

    if (projectDir.isDirectory()){
      // create the source directories
      createDir(srcDir);
      for (File sd : srcDirs){
        createDir(sd);
      }

      // create the utility dirs
      createDir(libDir);
      createDir(toolsDir);
      createDir(binDir);

      //create and initialize the optional IDE dirs
      createDir(nbDir);
      createDir(eclipseDir);

    } else {
      throw new Exception("failed to create project dir: " + projectDir.getPath());
    }
  }


  void copyTools (){
    if (coreDir.isDirectory()){
      File buildDir = new File(coreDir, "build");
      if (buildDir.isDirectory()){
        copyFile( new File(buildDir, "RunJPF.jar"), toolsDir);
        copyFile( new File(buildDir, "RunAnt.jar"), toolsDir);
      }
    }
  }


  void createAntScript() {
    String templateContents = getResourceFileContents("templates/build.xml");
    String contents = templateContents.replaceAll("@PROJECT", projectName);

    File antScript = new File(projectDir, "build.xml");
    writeToNewFile(antScript, contents);
  }


  void createProjectProperties() {
    String templateContents = getResourceFileContents("templates/jpf.properties");
    String contents = templateContents.replaceAll("@PROJECT", projectName);

    File newFile = new File(projectDir, "jpf.properties");
    writeToNewFile(newFile, contents);
  }

  void createScripts() {
    String[] scripts = { "jpf", "jpf.bat", "ant", "ant.bat" };

    for (String fname : scripts){
      String contents = getResourceFileContents("templates/" + fname);
      File script = new File(binDir, fname);
      writeToNewFile(script, contents);
    }
  }

  void createEclipseFiles() {
    createProjectTemplateInstance("templates/dot-project", projectDir, ".project");
    createProjectTemplateInstance("templates/dot-classpath", projectDir, ".classpath");
    createProjectTemplateInstance("templates/run-jpf", eclipseDir, "run-" + projectName + ".launch");

    File etbDir = new File(projectDir, ".externalToolBuilders");
    etbDir.mkdir();
    createProjectTemplateInstance("templates/antbuilder", etbDir, "AntBuilder.launch");
  }

  /** for now we assume there are all srcdirs present
  void createDotClasspath() {
    // Eclipse seems to choke if it doesn't find the configured source roots,
    // so we have to create the file contents explicitly instead from a template
    printFile(new File(projectDir, ".classpath"), new Printable() {

      public void printOn( PrintWriter out){
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<classpath>");
        
        for (File d : srcDirs){
          String dname = d.getName();
          out.println("\t<classpathentry kind=\"src\" output=\"build/" + dname + "\" path=\"src/" + dname + "\"/>");
        }
        out.println("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>");
        out.println("\t<classpathentry kind=\"output\" path=\"build/main\"/>");

        out.println("</classpath>");
      }
    });
  }
  **/


  void createNbFiles() {
    createProjectTemplateInstance("templates/nb-project.xml", nbDir, "project.xml");
    createProjectTemplateInstance("templates/ide-file-targets.xml", nbDir, "ide-file-targets.xml");
  }

  //--- various utility methods

  void createDir (File dir){
    dir.mkdir();
    if (!dir.isDirectory()){
      throw new Exception("failed to create: " + dir.getAbsolutePath());
    }
  }

  void createProjectTemplateInstance( String resourceName, File dir, String fname){
    String templateContents = getResourceFileContents( resourceName);
    String contents = templateContents.replaceAll("@PROJECT", projectName);

    File newFile = new File(dir, fname);
    writeToNewFile(newFile, contents);
  }

  void printFile(File f, Printable printer) {
    PrintWriter out = null;

    try {
      f.createNewFile();
      FileWriter fw = new FileWriter(f);
      out = new PrintWriter(fw);

      printer.printOn(out);

    } catch (IOException iox){
      throw new Exception("error writing: " + f.getAbsolutePath());

    } finally {
      if (out != null){
        out.close();
      }
    }
  }

  boolean copyFile (File src, File toDir){
    if (src.isFile()){
      File tgt = new File(toDir, src.getName());
      try {
        if (tgt.createNewFile()){
          String contents = getFileContents(src);
          writeToNewFile(tgt, contents);
          return true;
        }
      } catch (IOException iox) {
        throw new Exception("failed to create file: " + tgt.getAbsolutePath());
      }
    }

    return false;
  }

  String getFileContents (File file){
    FileInputStream is = null;

    try {
      is = new FileInputStream(file);
      int size = is.available();
      if (size > 0){
        byte[] buf = new byte[size];
        is.read(buf);

        return new String(buf);
      } else {
        return "";
      }

    } catch (FileNotFoundException fnfx) {
      return null;

    } catch (IOException iox) {
      throw new Exception("error reading file: " + file.getAbsolutePath());

    } finally {
      if (is != null){
        try {
          is.close();
        } catch (IOException iox){
          throw new Exception("failed to close file input stream: " + file.getAbsolutePath());
        }
      }
    }
  }

  String getResourceFileContents (String fileName){
    InputStream is = getClass().getResourceAsStream(fileName);

    if (is == null){
      throw new Exception("resource not found: " + fileName);
    }

    try {
      int size = is.available();
      if (size > 0){
        byte[] buf = new byte[size];
        is.read(buf);
        return new String(buf);
      } else {
        throw new Exception("resource empty: " + fileName);
      }

    } catch (IOException iox){
      throw new Exception("error reading resource contents: " + fileName);

    } finally {
      try {
        is.close();
      } catch (IOException iox){
        throw new Exception("failed to close input resource stream: " + fileName);
      }
    }
  }

  void writeToNewFile (File file, String contents){
    FileWriter fw = null;

    try {
      file.createNewFile();

      if (contents != null && contents.length() > 0){
        fw = new FileWriter(file);
        fw.write(contents, 0, contents.length());
      }

    } catch (IOException iox) {
      throw new Exception("failed to write: " + file.getAbsolutePath());

    } finally {
      if (fw != null){
        try {
          fw.close();
        } catch (IOException iox){
          throw new Exception("failed to close output file: " + file.getAbsolutePath());
        }
      }
    }
  }

}
