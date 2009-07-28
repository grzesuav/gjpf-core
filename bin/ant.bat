@echo off
REM
REM very simplistic ant batch file for Windows (just for bootstrapping purposes)
REM

REM Set the JPF_HOME directory
set JPF_HOME=%~dp0..\..

REM where to find javac
set CP=%JAVA_HOME%\lib\tools.jar

REM this is the common ant stuff
set CP=%CP%;%JPF_HOME%\lib\ant.jar
set CP=%CP%;%JPF_HOME%\lib\ant-launcher.jar
set CP=%CP%;%JPF_HOME%\lib\ant-junit.jar
set CP=%CP%;%JPF_HOME%\lib\xml-apis.jar

REM other libraries
set CP=%CP%;%JPF_HOME%\lib\junit-4.6.jar
set CP=%CP%;%JPF_HOME%\lib\bcel.jar

REM JPF class dirs
REM want to use those first
set CP=%CP%;%JPF_HOME%\build\main
set CP=%CP%;%JPF_HOME%\build\peers


set JVM_FLAGS=-Xmx1536m
java %JVM_FLAGS% -classpath "%CP%" org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9
