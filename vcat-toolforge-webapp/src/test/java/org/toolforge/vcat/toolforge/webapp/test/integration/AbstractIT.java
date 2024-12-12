package org.toolforge.vcat.toolforge.webapp.test.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.toolforge.vcat.toolforge.webapp.test.integration.apps.VcatToolforgeWebapp;

abstract class AbstractIT {

    protected static VcatToolforgeWebapp VCAT_TOOLFORGE_WEBAPP;

    @BeforeAll
    static void beforeAll() {
        VCAT_TOOLFORGE_WEBAPP = new VcatToolforgeWebapp();
        VCAT_TOOLFORGE_WEBAPP.start();
    }

    @AfterAll
    static void afterAll() {
        VCAT_TOOLFORGE_WEBAPP.stop();
    }

}
