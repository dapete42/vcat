package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import org.junit.jupiter.api.BeforeAll;

/**
 * Base integration test.
 */
public abstract class VcatToolforgeITBase {

    @BeforeAll
    static void beforeAll() {
        VcatToolforgeContainers.instance().start();
    }

}
