## Welcome to the Java™Pathfinder System ##

This is the main page for Java™ Pathfinder (JPF). JPF is an extensible software model checking framework for Java™ bytecode programs. The system was developed at the [NASA Ames Research Center](http://arc.nasa.gov), open sourced in 2005, and is freely available on this server under the [Apache-2.0 license](http://www.apache.org/licenses/LICENSE-2.0).


This page is our primary source of documentation, and is divided into the following sections.

   ---

  * [Introduction](intro/index.md) -- a brief introduction into model checking and JPF
    * [What is JPF?](intro/what_is_jpf.md)
    * [Testing vs. Model Checking](intro/testing_vs_model_checking.md)
         - [Random value example](intro/random_example.md)
         - [Data race example](intro/race_example.md)
    * [JPF key features](intro/classification.md)
    
    ---

  * [How to obtain and install JPF](install/index.md) -- everything to get it running on your machine
    - [System requirements](install/requirements.md)
    - Downloading [binary snapshots](install/snapshot.md) and [sources](install/repositories.md)
    - [Creating a site properties file](install/site-properties.md)
    - [Building, testing, and running](install/build.md)
    - Installing the JPF plugins
         - [Eclipse](install/eclipse-plugin.md)
         - [NetBeans](install/netbeans-plugin.md)
    
    ---
         
  * [How to use JPF](user/index.md) -- the user manual for JPF
    - [Different applications of JPF](user/application_types.md)
    - [JPF's runtime components](user/components.md)
    - [Starting JPF](user/run.md)
    - [Configuring JPF](user/config.md)
    - [Understanding JPF output](user/output.md)
    - [Using JPF's Verify API in the system under test](user/api.md)
    
    ---
        
  * [Developer guide](devel/index.md) -- what's under the hood
    * [Top-level design](devel/design.md)
    * Key mechanisms, such as 
        - [ChoiceGenerators](devel/choicegenerator.md)
        - [Partial order reduction](devel/partial_order_reduction.md)
        - [Slot and field attributes](devel/attributes.md)
    * Extension mechanisms, such as
        - [Listeners](devel/listener.md)
        - [Search Strategies](devel/design.md)
        - [Model Java Interface (MJI)](devel/mji.md)
        - [Bytecode Factories](devel/bytecode_factory.md)
    * Common utility infrastructures, such as
        - [Logging system](devel/logging.md)
        - [Reporting system](devel/report.md)
    * [Running JPF from within your application](devel/embedded.md)
    * [Writing JPF tests](devel/jpf_tests.md)
    * [Coding conventions](devel/coding_conventions.md)
    * [Hosting an Eclipse plugin update site](devel/eclipse_plugin_update.md)
        
    ---
        
  * [JPF core project](jpf-core/index.md) -- description and link to jpf-core
    
    ---
      
  * [Related research and publications](papers/index.md)

