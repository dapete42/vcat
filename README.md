[![Build Status](https://travis-ci.org/dapete42/vcat.svg?branch=master)](https://travis-ci.org/dapete42/vcat)

vcat
====

This repository contains the Java code for the vCat 'visual' category renderer,
which uses Graphviz (http://graphviz.org/) to render the category structure of
MediaWiki wikis (in particular those run by the Wikimedia Foundation) as
graphical trees.

It replaces a similar tool written in PHP, Catgraph.

Projects
--------

It consists of five Maven submodules:

* vcat-core: The core library to evaluate parameters, get the necessary
  category information from the wiki and render it into an image.
* vcat-redis: An addition which allows the use of Redis (through the Jedis
  library) to store the API and metadata caches.
* vcat-webapp-base: Base classes to create Servlets which render graphs using
  vCat.
* vcat-webapp-simple: A simple Servlet version of vCat which expects
  parameters as GET or POST parameters.
* vcat-toolforge-base: Base classes for the vCat Servlet as used on Wikimedia
  Toolforge.
* vcat-toolforge-webapp: A Servlet version of vCat which uses the meta_p.wiki
  table to look up wikis, and Redis for caching, as used on Wikimedia
  Toolforge.

Copyright notice
----------------

Copyright 2017 Peter Schlömer

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
the files in this project except in compliance with the License. You may obtain
a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
