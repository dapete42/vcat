vcat
====

This repository contains the Java code for the vCat 'visual' category renderer,
which uses Graphviz (http://graphviz.org/) to render the category structure of
MediaWiki wikis (in particular those run by the Wikimedia Foundation) as
graphical trees.

It replaces a similar tool written in PHP, Catgraph.

Projects
--------

It consists of four projects, all of which are Eclipse projects as well as
Maven artifacts. These are:

* vcat-core: The core library to evaluate parameters, get the necessary
  category information from the wiki and render it into an image.
* vcat-gv_java: An addition which allows the use of the Graphviz JNI, instead
  of relying on external program calls to the Graphviz command line utilities.
* vcat-redis: An addition which allows the use of Redis (through the Jedis
  library) to store the API and metadata caches.
* vcat-webapp: A Java Servlet version of vCat which expects parameters as GET
  or POST parameters and directly returns the rendered file.
* vcat-toollabs: A deamon version of vCat for use on Tool Labs.

Licensing
---------

The vcat-gv_java project is released under the Eclipse Public License (EPL)
1.0. This is because it contains code from the Graphviz JNI, which is released
under this license.

All other projects are released under the Apache License, Version 2.0.
