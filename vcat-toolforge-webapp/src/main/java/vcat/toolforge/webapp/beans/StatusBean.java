package vcat.toolforge.webapp.beans;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Named("status")
@RequestScoped
public class StatusBean implements Serializable {

    @Serial
    private static final long serialVersionUID = -3546694105983195743L;

    @Getter
    private List<Thread> threads;

    @Getter
    private int numberOfThreads;

    @Getter
    private long freeMemory;

    @Getter
    private long maxMemory;

    @Getter
    private long totalMemory;

    @PostConstruct
    void init() {
        Thread[] threads = new Thread[Thread.activeCount()];
        numberOfThreads = Thread.enumerate(threads);
        while (threads.length != numberOfThreads) {
            threads = new Thread[numberOfThreads];
            numberOfThreads = Thread.enumerate(threads);
        }
        this.threads = Arrays.asList(threads);
        final long mb = 1024 * 1024;
        freeMemory = Runtime.getRuntime().freeMemory();
        maxMemory = Runtime.getRuntime().maxMemory();
        totalMemory = Runtime.getRuntime().totalMemory();
    }

    @SuppressWarnings("unchecked")
    public String getServerInfo() {
        return ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getServerInfo();
    }

    public String getSystemProperty(String key) {
        return System.getProperty(key);
    }

}
