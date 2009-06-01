@echo off
REM
REM very simplistic ant batch file for Windows (why would you use this?)
REM

REM Set the JPF_HOME directory
set JPF_HOME=%~dp0..\..

REM where to find javac
set CP=%JAVA_HOME%\lib\tools.jar

REM this is the common ant stuff

set CP=%CP%;%JPF_HOME%\build-tools\lib\ant.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-launcher.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-junit.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-jantlr.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-jai.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-jmf.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-trax.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\ant-nodeps.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\xml-apis.jar
set CP=%CP%;%JPF_HOME%\build-tools\lib\junit-4.1.jar


REM If we have class files (e.g., with a source distribution), we probably
REM want to use those first
set CP=%CP%;%JPF_HOME%\build\jpf
set CP=%CP%;%JPF_HOME%\build\test

REM Otherwise, we look for the jar (binary distributions)
set CP=%CP%;%JPF_HOME%\lib\jpf.jar

REM And these are the external libs we use at runtime
REM (include the CLASSPATH first, in case somebody wants to use specific versions)
set CP=%CP%;%CLASSPATH%

set CP=%CP%;%JPF_HOME%\lib\bcel.jar

REM our standard native peer environment
REM * For our source distribution.
set CP=%CP%;%JPF_HOME%\build\env\jvm

REM * For our binary dirtribution
set CP=%CP%;%JPF_HOME%\lib\env_jvm.jar

REM Examples
set CP=%CP%;%JPF_HOME%\examples
set CP=%CP%;%JPF_HOME%\build\examples

set JVM_FLAGS=-Xmx1536m
java %JVM_FLAGS% -classpath "%CP%" org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9
