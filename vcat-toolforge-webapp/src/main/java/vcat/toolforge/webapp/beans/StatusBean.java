package vcat.toolforge.webapp.beans;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
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
        freeMemory = Runtime.getRuntime().freeMemory() / (1024L * 1024L);
        maxMemory = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        totalMemory = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
    }

    public String getSystemProperty(String key) {
        return System.getProperty(key);
    }

}
