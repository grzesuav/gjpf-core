@echo off

REM genpeer - script to generate NativePeer class

set JPF_HOME=%~dp0..\..

REM this is for GenPeer itself (and the JPF infrastructure it might use)
set CP=%JPF_HOME%\build
set CP=%CP%;%JPF_HOME%\lib\jpf.jar
set CP=%CP%;%JPF_HOME%\lib\bcel.jar

REM this is where we keep our model classes (the potential targets)
set CP=%CP%;%JPF_HOME%\build\env\jpf

java -classpath "%CP%" gov.nasa.arc.ase.jpf.tools.GenPeer %1
