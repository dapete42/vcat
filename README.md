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
* vcat-daemon: A daemon version of vCat which waits for JSON input files
  containing parameters in one directory and outputs the finished files to
  another.
* vcat-webapp: A Java Servlet version of vCat which expects parameters as GET
  or POST parameters and directly returns the rendered file.

Licensing
---------

The vcat-gv_java project is released under the Eclipse Public License (EPL)
1.0. This is because it uses the Graphviz JNI, which is released under this
license.

All other projects are released unter the GNU General Public License (GPL) 2 or
later, with an additional exception which allows them to link to and use both
vcat-gv_java and the Graphviz JNI libraries released unter the EPL. (See
https://www.gnu.org/licenses/gpl-faq.html#GPLIncompatibleLibs for an
explanation why this is necessary.)
