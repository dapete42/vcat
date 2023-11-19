package org.toolforge.vcat.toolforge.webapp.beans;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Getter
@Named("status")
@RequestScoped
public class StatusBean implements Serializable {

    @Serial
    private static final long serialVersionUID = -3546694105983195743L;

    private List<Thread> threads;

    private int numberOfThreads;

    private long freeMemory;

    private long maxMemory;

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
        final long mb = 1024L * 1024L;
        freeMemory = Runtime.getRuntime().freeMemory() / mb;
        maxMemory = Runtime.getRuntime().maxMemory() / mb;
        totalMemory = Runtime.getRuntime().totalMemory() / mb;
    }

    public String getSystemProperty(String key) {
        return System.getProperty(key);
    }

}
