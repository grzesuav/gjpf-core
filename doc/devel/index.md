# JPF Developer Guide #

From the previous two sections, you have learned that JPF has one recurring, major theme: it is not a monolithic system, but rather a configured collection of components that implement different functions like state space search strategies, report generation and much more. Being adaptive is JPF's answer to the scalability problem of software model checking.

This not only makes JPF a suitable system for research, but chances are that if are you serious enough about JPF application, you sooner or later end up extending it. This section includes the following topics which describe the different mechanisms that can be used to extend JPF.

 * [Top-level design](design.md)
 * Key mechanisms, such as 
     - [ChoiceGenerators](choicegenerator.md)
     - [Partial order reduction](partial_order_reduction.md)
     - [Slot and field attributes](attributes.md)
 * Extension mechanisms, such as
     - [Listeners](listener.md)
     - [Search Strategies](design.md)
     - [Model Java Interface (MJI)](mji.md)
     - [Bytecode Factories](bytecode_factory.md)
 * Common utility infrastructures, such as
     - [Logging system](loggin.md)
     - [Reporting system](report.md)
 * [Running JPF from within your application](embedded.md)
 * [Writing JPF tests](jpf_tests.md)
 * [Coding conventions](coding_conventions.md)
 * [Hosting an Eclipse plugin update site](eclipse_plugin_update.md)