# Integration Tests

Integrations tests will only run if the `integration-test` profile is explicitly activated and the 
`verify` Maven goal is executed.

```sh
./mvnw -pl vcat-toolforge-webapp -am -P integration-test verify
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
