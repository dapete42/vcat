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

Integration tests
-----------------

The `vcat-toolforge-webapp` module has integration tests. They will only run if the
`integration-test` Maven profile is explicitly activated and the `verify` Maven goal is executed.

To run *only* the integration tests, which is set up as a GitHub action that runs for every commit
and also once a week: 

```sh
./mvnw -pl vcat-toolforge-webapp -am -P integration-test verify
```

What these tests do:

* Generate a Docker image `vcat-toolforge-webapp` containing an environment that somewhat resembles the
  Toolforge container environment.
* The integration test uses Testcontainers to start a container for MariaDB and a container with
  `vcat-toolforge-webapp`. Database access etc. is configured automatically.

For Testcontainers to work, Docker or some other supported container runtime must be available (see 
[General Container runtime requirements](https://java.testcontainers.org/supported_docker_environment/)).

### Image with simulated Toolforge environment

The `vcat-toolforge-webapp` image in these tests is made to be similar to the real Toolforge
environment.

* The base image used is the same OS, based on the `heroku.image` property in the main `pom.xml`.
* It contains an installation of the same major JDK version (installed using *SDKMAN!*), based on
  the JDK version defined by the `java.runtime.version` property in `system.properties`.
* The same APT packages are installed, based on `Aptfile`.

The main `pom.xml` also defines `heroku.os` and `heroku.os.version`. When the build runs in the
Toolforge build environment, these will be checked against the actual OS. If this fails, both
these properties and `heroku.image` have to be changed accordingly. Then the integration test can
be used to verify whether everything still work works.

### Font rendering test

The font rendering test calls a special REST endpoint in `vcat-toolforge-webapp`, which is only
activated for the integration tests. This is used to renders images and compare them to reference
images. It is primarily used to check for language support, i.e. that the script used for a
language can be rendered correctly.

These tests are defined by the following:

* `vcat-toolforge-webapp/src/test/resources/testFontRendering.csv`, which contains the test cases.
  This is a CSV file with the string that will be rendered and a name, which will be used as the
  name of the reference image.
* The `vcat-toolforge-webapp/src/test/resources/reference-images` directory, which contains the
  reference images.

A test case will fail if there is no reference image or the reference image does not match. The
generated image will always be in `vcat-toolforge-webapp/target/reference-images` afterwards.

* This is expected if a new test case was added. All that needs to be done to pass the test is
  move the generated image to `vcat-toolforge-webapp/src/test/resources/reference-images` and
  commit it to Git.
* Otherwise, you need to investigate what has changed. Either the font rendering is broken, or
  there has been some change to the fonts or which fonts are used.

Copyright notice
----------------

Copyright 2013-2025 Peter Schlömer

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
the files in this project except in compliance with the License. You may obtain
a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
