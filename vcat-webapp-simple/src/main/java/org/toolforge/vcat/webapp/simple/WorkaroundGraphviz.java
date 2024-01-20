package org.toolforge.vcat.webapp.simple;

import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.params.GraphvizParams;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

public class WorkaroundGraphviz implements Graphviz {

    private static final String SERVICE_URI_TEMPLATE = "https://dapete.net/graphviz/?algorithm=%s&outputFormat=%s";

    private final Graphviz otherGraphviz;

    public WorkaroundGraphviz(Graphviz otherGraphviz) {
        this.otherGraphviz = otherGraphviz;
    }

    @Override
    public void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException {
        // Try to run other Graphviz first, if there is an error try the service
        try {
            otherGraphviz.render(inputFile, outputFile, params);
        } catch (Exception e) {
            try {
                final var uri = URI.create(String.format(SERVICE_URI_TEMPLATE,
                        params.getAlgorithm().name(), params.getOutputFormat().name()));
                final var httpRequest = HttpRequest.newBuilder(uri)
                        .POST(HttpRequest.BodyPublishers.ofFile(inputFile))
                        .build();
                final var responseBodyHandler = HttpResponse.BodyHandlers.ofByteArray();
                final HttpResponse<byte[]> httpResponse;
                try (var httpClient = HttpClient.newHttpClient()) {
                    httpResponse = httpClient.send(httpRequest, responseBodyHandler);
                }
                if (httpResponse.statusCode() == 200) {
                    Files.write(outputFile, httpResponse.body());
                } else {
                    final String body = new String(httpResponse.body(), StandardCharsets.UTF_8);
                    throw new GraphvizException(MessageFormat.format("Status {0}: {1}", httpResponse.statusCode(), body));
                }
            } catch (InterruptedException | IOException ex) {
                throw new GraphvizException(ex);
            }
        }
    }

}
