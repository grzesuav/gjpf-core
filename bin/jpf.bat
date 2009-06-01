REM
REM overly simplified batch file to start JPF from a command prompt
REM

@echo off

REM Set the JPF_HOME directory
set JPF_HOME=%~dp0..

REM where to find javac
set CP=%JAVA_HOME%\lib\tools.jar

REM this is the common ant stuff

REM If we have class files (e.g., with a source distribution), we probably
REM want to use those first
set CP=%CP%;%JPF_HOME%\build\jpf
set CP=%CP%;%JPF_HOME%\build\test

REM Otherwise, we look for the jar (binary distributions)
set CP=%CP%;%JPF_HOME%\lib\jpf.jar

REM And these are the external libs we use at runtime
REM (include the CLASSPATH first, in case somebody wants to use specific versions)
set CP=%CLASSPATH%;%CP%

set CP=%CP%;%JPF_HOME%\lib\bcel.jar

REM our standard native peer environment
REM * For our source distribution.
set CP=%CP%;%JPF_HOME%\build\env\jvm

REM * For our binary dirtribution
set CP=%CP%;%JPF_HOME%\lib\env_jvm.jar

REM Examples
set CP=%CP%;%JPF_HOME%\examples
set CP=%CP%;%JPF_HOME%\build\examples

set JVM_FLAGS=-agentlib:yjpagent=tracing -Xmx1536m

java %JVM_FLAGS% -classpath "%CP%" gov.nasa.jpf.JPF +jpf.basedir="%JPF_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9
