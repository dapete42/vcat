package org.toolforge.vcat.toolforge.webapp.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serial;
import java.io.Serializable;

@Named("error")
@RequestScoped
public class ErrorBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 8361096415662247334L;

    @Setter
    private Exception exception;

    public String getMessage() {
        if (exception == null) {
            return null;
        }
        return exception.getMessage();
    }

    public String getStacktrace() {
        if (exception == null) {
            return null;
        }
        return ExceptionUtils.getStackTrace(exception);
    }

}
