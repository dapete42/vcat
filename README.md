[![Maven](https://github.com/dapete42/vcat/actions/workflows/maven.yml/badge.svg)](https://github.com/dapete42/vcat/actions/workflows/maven.yml)
[![CodeQL](https://github.com/dapete42/vcat/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/dapete42/vcat/actions/workflows/codeql-analysis.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/dapete42/vcat/badge)](https://www.codefactor.io/repository/github/dapete42/vcat)
[![codecov](https://codecov.io/gh/dapete42/vcat/branch/main/graph/badge.svg)](https://codecov.io/gh/dapete42/vcat)

vcat
====

This repository contains the Java code for the vCat 'visual' category renderer,
which uses Graphviz (http://graphviz.org/) to render the category structure of
MediaWiki wikis (in particular those run by the Wikimedia Foundation) as
graphical trees.

It replaces a similar tool written in PHP, Catgraph.

It is designed to run on the
[Toolforge](https://wikitech.wikimedia.org/wiki/Help:Toolforge) environment
provided by the Wikimedia Foundation.

Maven modules
-------------

It consists of five Maven submodules:

* `vcat-core` - The core library to evaluate parameters, get the necessary
  category information from the wiki and render it into an image.
* `vcat-caffeine` - An addition which allows the use of Caffeine to store the 
  API and metadata caches.
* `vcat-toolforge-webapp`-  A version of vCat which uses the `meta_p.wiki`
  table available in the Toolforge environment to look up wikis, and Caffeine
  for caching. Builds an executable JAR file based on Quarkus
  (https://quarkus.io/). This is what runs in the Toolforge environment (also
  see the next section). 
* `vcat-webapp-simple` - A simpler version of vCat for local testing. This also
  creates am executable JAR file based on Quarkus, but does not require
  anything else.

Toolforge Build Service
-----------------------

The whole project is designed to be used with the
[Toolforge Build Service](https://wikitech.wikimedia.org/wiki/Help:Toolforge/Build_Service)
to create an image which can be run in the Toolforge environment as a web
application.

In particular this includes:

* A `Procfile` to define how to start the web application
  ([see 'Procfile' in the Build Service documentation](https://wikitech.wikimedia.org/wiki/Help:Toolforge/Build_Service#Procfile)).
* `bin/vcat-toolforge-wrapper`, which is the start script referenced in
  `Procfile`.
* An `Aptfile` to define additional Ubuntu packages to be installed, which is
  used by
  [heroku-buildpack-apt](https://elements.heroku.com/buildpacks/heroku/heroku-buildpack-apt)
  included in the Toolforge Build Service
  ([see 'Installing Apt packages' in the Build Service documentation](https://wikitech.wikimedia.org/wiki/Help:Toolforge/Build_Service#Installing_Apt_packages)).
  In particular, this is used to install Graphviz and additional fonts.

Copyright notice
----------------

Copyright 2013-2024 Peter Schlömer

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
the files in this project except in compliance with the License. You may obtain
a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
