package org.toolforge.vcat.toolforge.webapp.test.integration.docker.util;

import org.junit.jupiter.api.BeforeAll;

/**
 * Base integration test.
 */
public abstract class VcatToolforgeDockerITBase {

    @BeforeAll
    static void beforeAll() {
        VcatToolforgeContainers.instance().start();
    }

}
