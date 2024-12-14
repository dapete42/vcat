package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class VcatToolforgeContainers {

    private final MariaDBContainer<?> mariadbContainer;
    private final GenericContainer<?> vcatToolforgeWebappContainer;

    public VcatToolforgeContainers() {
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
        return "jdbc:mariadb://mariadb:3306/meta_p?driver=" + mariadbContainer.getDriverClassName();
    }

    public void stop() {
        mariadbContainer.stop();
        vcatToolforgeWebappContainer.stop();
    }

    public String getUrl(String path) {
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
