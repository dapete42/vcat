package org.toolforge.vcat.mediawiki;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleWikimediaWikiTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            en.wikipedia.org, https://en.wikipedia.org/w/api.php
            127.0.0.1, https://127.0.0.1/w/api.php
            [::1], https://[::1]/w/api.php
            will?be#escaped, https://will%3Fbe%23escaped/w/api.php
            """)
    void getApiUrl(String host, String expectedApiUrl) {
        final var wiki = new SimpleWikimediaWiki(host);
        final var apiUrl = wiki.getApiUrl();
        assertEquals(expectedApiUrl, apiUrl);
    }

}
