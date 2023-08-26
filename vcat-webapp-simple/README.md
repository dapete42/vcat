vcat-webapp-simple
==================

This README assumes you are running Linux. Graphviz (https://graphviz.org/) must be installed. 

To get the web application running, first build it with `mwnv` (or `mvn` if you have Maven installed already) from the
root directory of the repository:

```shell
./mvnw package -pl vcat-webapp-simple -am
```

Then launch the JAR:

```shell
java -jar vcat-webapp-simple/target/vcat-simple-runner.jar
```

It should now start and be reachable at http://localhost:8080/render.

Example: http://localhost:8080/render?wiki=de.wikipedia.org&category=Berlin

The `wiki` parameter is the domain name of the MediaWiki wiki to be accessed; `de.wikipedia.org` is German Wikipedia
`en.wikipedia.org` is English Wikipedia etc. Other than that is works mostly the same as the real vCat as described
on https://meta.wikimedia.org/wiki/User:Dapete/vCat.

Configuration
-------------

See `src/main/resources/application.properties`.

* `cache.dir` is where intermediate results and final rendered images are stored.
* `graphviz.dir` is where the Graphviz binaries (`dot`, `fdp` etc.) are expected.