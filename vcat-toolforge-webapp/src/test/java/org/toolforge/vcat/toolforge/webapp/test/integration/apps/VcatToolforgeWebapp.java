package org.toolforge.vcat.toolforge.webapp.test.integration.apps;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;

import java.util.Map;

public class VcatToolforgeWebapp {

    private final MariaDBContainer<?> mariadbContainer;
    private final GenericContainer<?> vcatToolforgeWebappContainer;

    public VcatToolforgeWebapp() {
        mariadbContainer = new MariaDBContainer<>("mariadb:11");
        vcatToolforgeWebappContainer = new GenericContainer<>("vcat-toolforge-webapp");
    }

    public void start() {
        final var network = Network.newNetwork();
        mariadbContainer.withNetwork(network)
                .withNetworkAliases("mariadb")
                .withDatabaseName("meta_p")
                .withInitScript("integration/mariadb-init.sql")
                .start();
        vcatToolforgeWebappContainer.withNetwork(network)
                .withEnv(getEnv())
                .withExposedPorts(8000)
                .start();
        System.out.printf("Test instance of vcat-toolforge-webapp running on %s%n", getUrl(""));
    }

    private Map<String, String> getEnv() {
        return Map.of(
                "TOOL_REPLICA_USER", mariadbContainer.getUsername(),
                "TOOL_REPLICA_PASSWORD", mariadbContainer.getPassword(),
                "QUARKUS_DATASOURCE_JDBC_URL", getQuarkusDatasourceJdbcUrl(),
                "GRAPHVIZ_DIR", "/usr/bin"
        );
    }

    private String getQuarkusDatasourceJdbcUrl() {
        // driver has to be explicitly added as a parameter
        return "jdbc:mariadb://mariadb:3306/meta_p?driver=" +  mariadbContainer.getDriverClassName();
    }

    public void stop() {
        vcatToolforgeWebappContainer.stop();
    }

    public String getUrl(String path) {
        return String.format("http://%s:%s/%s",
                vcatToolforgeWebappContainer.getHost(), vcatToolforgeWebappContainer.getMappedPort(8000), path);
    }

}
