# Integration Tests

## Docker Integration Tests

Docker-based untegrations tests will only run if the `integration-tests-docker` profile is explicitly activated and the 
`verify` Maven goal is executed.

```sh
./mvnw -pl vcat-toolforge-webapp -am -P integration-tests-docker verify
```

*What this does:*

* A Docker image `vcat-toolforge-webapp` containing an environment that somewhat resembles the
  Toolforge container environment is generated.
* The integration test uses Testcontainers to start a container for MariaDB and a container with
  `vcat-toolforge-webapp`. Database access etc. is configured automatically. 
  
TODO:

* Set up a GitHub action.
* More tests.
* Perhaps use a real source for the `wiki` table.

## Heroku Integration Tests

Some integration tests for Toolforge's Heroku build environment are automatically run for builds in this environment.
This is triggered by the `heroku-build` profile.