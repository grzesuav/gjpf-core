[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)]()
[![Build Status](https://travis-ci.org/grzesuav/gjpf-core.svg?branch=develop)](https://travis-ci.org/grzesuav/gjpf-core) 
[![Coverage Status](https://coveralls.io/repos/grzesuav/gjpf-core/badge.svg?branch=develop)](https://coveralls.io/r/grzesuav/gjpf-core) 
[![HuBoard badge](http://img.shields.io/badge/Hu-Board-7965cc.svg)](https://huboard.com/grzesuav/gjpf-core)


This is fork of Java Path Finder project (which is one-to-one mirrored [here](https://github.com/grzesuav/jpf-core)).


Main reasons/motivation:
* desire to contribute in some OS project
* experiment with new features
* maybe contribute some changes to original JPF.

Currently:
* setup of Travis CI build was configured
* develop branch was created
* coverity-scan branch was created and coverity static analysis scan was attached (see branch coverity-scan to see results)
* coveralls.io was configured, coverage result are reported on badge
* gradle is almost done, need to finalize work

#Gradle project layout
It was difficult to balance between old layout, which is required by program way of work (directory layout, files location etc.).
Main change is project modularization, which reflects present of multiple source directories in ant structure. I have temporarily removed
some IDE-related directories (netbeans and eclipse) to keep basic layout simpler, need to consider in which form restore them.
## Build
Gradle is required, project was build on Gradle 2.3, there was some changes in recent Gradle version so for now probably best way is to use same version for building.
To check which tasks are accessible type 'gradle tasks' in root project directory (where 'build.gradle' and 'setting.gradle' are placed).
To build project type 'gradle build' or 'gradle clean build', it will:
* clean (in case when this target was called)
* compile each module, examples and tests
* make jars necessary to properly run tests
* test project with coverage (reports are located in build/reports)
* make distribution packages (tar and zip) placed in build/

Distribution packages are identical as ant-make dist package.

## Repository artifact
This is main goal of gradle-switch. After typing 'gradle install' each of submodule will be deployed in local maven repository and accessible from it.
In other words, if you have some maven/gradle/other project managed by any dependency management system, you can refer to this modules by
groupId:artifactId:version (actual format depends on build system).
Group id is 'gov.nas.jpf.jpf-core', artifacts ids are : 'annotations', 'classes', 'main' and 'peers'. Current version is set to '0.1'.
In future probably release artifacts will be deployed on public maven repo (or something similar), so in order to write extension/software depending
on jpf, there will be no reason to download and build jpf manually (well, except that run it, of course).


## To be done
* discuss with jpf developers project layout/get some feedback
* put info about importing project to eclipse (and ask what IDEs developers are using)



[![Join the chat at https://gitter.im/grzesuav/gjpf-core](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/grzesuav/gjpf-core?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
