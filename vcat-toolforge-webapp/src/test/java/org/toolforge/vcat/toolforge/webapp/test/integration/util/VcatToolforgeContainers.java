package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
public class VcatToolforgeContainers {

    private static VcatToolforgeContainers INSTANCE;

    private Network network;
    private MariaDBContainer<?> mariadbContainer;
    private GenericContainer<?> vcatToolforgeWebappContainer;

    public static VcatToolforgeContainers instance() {
        if (INSTANCE == null) {
            INSTANCE = new VcatToolforgeContainers();
        }
        return INSTANCE;
    }

    public void start() {
        newNetwork();
        startMariadbContainer();
        startVcatToolforgeWebappContainer();
    }

    private void newNetwork() {
        if (network == null) {
            network = Network.newNetwork();
        }
    }

    private void startMariadbContainer() {
        if (mariadbContainer == null) {
            LOG.info("Starting Mariadb container");
            mariadbContainer = new MariaDBContainer<>("mariadb:11");
            mariadbContainer.withNetwork(network)
                    .withNetworkAliases("mariadb")
                    .withDatabaseName("meta_p")
                    .withInitScript("integration/mariadb-init.sql")
                    .start();
        }
    }

    private void startVcatToolforgeWebappContainer() {
        if (vcatToolforgeWebappContainer == null) {
            LOG.info("Starting vCat Toolforge webapp container");
            vcatToolforgeWebappContainer = new GenericContainer<>("vcat-toolforge-webapp");
            vcatToolforgeWebappContainer.withNetwork(network)
                    .withEnv(getEnv())
                    .withExposedPorts(8000)
                    .start();
        }
    }

    private Map<String, String> getEnv() {
        if (mariadbContainer == null) {
            throw new IllegalStateException("Mariadb container not started");
        }
        return Map.of(
                "TOOL_REPLICA_USER", mariadbContainer.getUsername(),
                "TOOL_REPLICA_PASSWORD", mariadbContainer.getPassword(),
                "QUARKUS_DATASOURCE_JDBC_URL", getQuarkusDatasourceJdbcUrl(),
                "GRAPHVIZ_DIR", "/usr/bin"
        );
    }

    private String getQuarkusDatasourceJdbcUrl() {
        if (mariadbContainer == null) {
            throw new IllegalStateException("Mariadb container not started");
        }
        // driver has to be explicitly added as a parameter
        return "jdbc:mariadb://mariadb:3306/meta_p?driver=" + mariadbContainer.getDriverClassName();
    }

    void stop() {
        if (mariadbContainer != null) {
            LOG.info("Stopping Mariadb container");
            mariadbContainer.stop();
            mariadbContainer = null;
        }
        if (vcatToolforgeWebappContainer != null) {
            LOG.info("Stopping vCat Toolforge webapp container");
            vcatToolforgeWebappContainer.stop();
            vcatToolforgeWebappContainer = null;
        }
    }

    public String getUrl(String path) {
        if (vcatToolforgeWebappContainer == null) {
            throw new IllegalStateException("vCat Toolforge webapp container not started");
        }
        return String.format("http://%s:%s/%s",
                vcatToolforgeWebappContainer.getHost(), vcatToolforgeWebappContainer.getMappedPort(8000), path);
    }

    public HttpResponse<byte[]> getHttpResponse(String path) throws IOException, InterruptedException {
        try (var httpClient = HttpClient.newHttpClient()) {
            final var uri = URI.create(getUrl(path));
            final var request = HttpRequest.newBuilder(uri).build();
            final var bodyHandler = HttpResponse.BodyHandlers.ofByteArray();
            return httpClient.send(request, bodyHandler);
        }
    }

    public HttpResponse<String> getHttpResponseStringBody(String path) throws IOException, InterruptedException {
        try (var httpClient = HttpClient.newHttpClient()) {
            final var uri = URI.create(getUrl(path));
            final var request = HttpRequest.newBuilder(uri).build();
            final var bodyHandler = HttpResponse.BodyHandlers.ofString();
            return httpClient.send(request, bodyHandler);
        }
    }

}
