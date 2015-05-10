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
* **(new)** documentation was modified, so now its fully navigable from github, click [here](doc/index.md)
* **(new)** merged latest Peter changes from babelfish repo (up to 25. commit, from 22 april 15)
* **(in progress)** preparing example maven/distribution repo on bintray as POC for gradle build

#Gradle project layout
It was difficult to balance between old layout, which is required by program way of work (directory layout, files location etc.).
Main change is project modularization, which reflects present of multiple source directories in ant structure. I have temporarily removed
some IDE-related directories (netbeans and eclipse) to keep basic layout simpler, need to consider in which form restore them.
## Build
Gradle is required, project was build on Gradle 2.3, there was some changes in recent Gradle version so for now probably best way is to use same version for building.
To check which tasks are accessible type 'gradle tasks' in root project directory (where 'build.gradle' and 'setting.gradle' are placed).
To build project type `gradle build` or `gradle clean build`, it will:
* clean (in case when this target was called)
* compile each module, examples and tests
* make jars necessary to properly run tests
* test project with coverage (reports are located in build/reports)
* make distribution packages (tar and zip) placed in build/

Distribution packages are identical as ant-make dist package.

## Repository artifact
This is main goal of gradle-switch. After typing `gradle install` each of submodule will be deployed in local maven repository and accessible from it.
In other words, if you have some maven/gradle/other project managed by any dependency management system, you can refer to this modules by:
* `GroupId:artifactId:version` (actual format depends on build system)

Group id is :
* `gov.nas.jpf.jpf-core`

artifacts id's are : 
* `annotations`
* `classes`
* `main` 
* `peers`

Current version is set to _`0.1`_.
So, if you want to use annotations module, here is maven example:
```
  	<dependency>
  		<groupId>gov.nasa.jpf.jpf-core</groupId>
  		<artifactId>annotations</artifactId>
  		<version>0.1</version>
  	</dependency>
```
In future probably release artifacts will be deployed on public maven repo (or something similar), so in order to write extension/software depending
on jpf, there will be no reason to download and build jpf manually (well, except that run it, of course).

## Importing to eclipse

**Prerequisites**
 * Gradle eclipse plugin : http://marketplace.eclipse.org/content/gradle-integration-eclipse-44
 * EGit : http://eclipse.org/egit/
 
At first, you need a local copy of this repo, which can be obtained :
* from command line : `git clone https://github.com/grzesuav/gjpf-core.git`
* import using eclipse wizard

*Importing using eclipse import wizard*:
* select `File -> Import -> Git -> Project from Git` and click next
* select `Create from uri` and click next
* paste into `URI`: `https://github.com/grzesuav/gjpf-core.git` and click next
* select at least `develop` branch and click next
* select desired location and click next
* wait until repository will be cloned and don't import any project, quit wizard

*Importing Gradle project to eclipse using `Gradle import wizard`* :
* select `File -> Import -> Gradle -> Gradle Project` and click next
* point to location where repository was cloned (also include `gjpf-core` directory) and click `Build Model`
* select all project visible in tree and click finish (leave all options as is)
* wait until project will be imported (could take a while)
* unfortunately, default eclipse output folder (for compiled classes and other resources) is $PROJECT_DIR/bin, which conflicts with jpf project layout. To change it, for every project (*besides root* `gjpf-core`) do the following : Right click on project name, select `Properties -> Java Build Path`, go into `Source` tab, and on the bottom there is `Default output folder`. Change it to $PROJECT_DIR/build.
* for root project `gjpf-core` eclipse project layout must be changed to fit jpf structure. To do it, right click on project name, select `Properties -> Java Build Path`, go into `Source` tab, and on the bottom there is `Default output folder`. As `Default output folder` set `build/default`. Next select checkbox  `Allow output folders for source folders` and for source folders above change `Output folder` (expand each source folder to see this) to `build/examples` and `build/tests`respectively.
* problem regarding non-existing test dependency (`RunJPF.jar`) will be resolved by building this jar, to do this, right click on project `gjpf-core` and select `Gradle -> Tasks Quick Launcher` and type `test`. It will build all required jars ad run tests.
 
 

## To be done
* discuss with jpf developers project layout/get some feedback
* put info about importing project to eclipse (and ask what IDEs developers are using)


[![Join the chat at https://gitter.im/grzesuav/gjpf-core](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/grzesuav/gjpf-core?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
