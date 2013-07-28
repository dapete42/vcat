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

* catgraph-core: The core library to evaluate parameters, get the necessary
  category information from the wiki and render it into an image.
* catgraph-gv_java: An addition which allows the use of the Graphviz JNI,
  instead of relying on external program calls to ghe Graphviz command line
  utilities.
* catgraph-daemon: A daemon version of vCat which waits for JSON input files
  containing parameters in one directory and outputs the finished files to
  another.
* catgraph-webapp: A Java Servlet version of vCat which expects parameters as
  GET or POST parameters and directly returns the rendered file.
