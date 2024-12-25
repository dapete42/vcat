package org.toolforge.vcat.toolforge.webapp.rest;

import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class ResponseService {

    /**
     * Qute error template
     */
    @Inject
    Template error;

    public Response errorResponse(Response.Status status, String message) {
        return errorResponse(status, message, "");
    }

    public Response errorResponse(Response.Status status, Exception e) {
        var stacktrace = Stream.of(e.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
        return errorResponse(status, e.getMessage(), stacktrace);
    }

    private Response errorResponse(Response.Status status, String errorMessage, String stacktrace) {
        return Response.ok()
                .entity(
                        error.data(
                                "status", status,
                                "errorMessage", errorMessage,
                                "stacktrace", stacktrace
                        ).render().getBytes(StandardCharsets.UTF_8)
                )
                .type(MediaType.TEXT_HTML_TYPE)
                .status(status)
                .build();
    }

}
