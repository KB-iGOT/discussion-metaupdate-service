package com.igot.cb;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class DiscussionMetaUpdateServiceApplicationTest {

    @Test
    void restTemplateBeanIsCreated() {
        discussionMetaUpdateServiceApplication app = new discussionMetaUpdateServiceApplication();
        RestTemplate rt = app.restTemplate();
        assertNotNull(rt);

        ClientHttpRequestFactory factory = rt.getRequestFactory();
        assertNotNull(factory);
    }

    @Test
    void clientHttpRequestFactoryIsCreated() {
        discussionMetaUpdateServiceApplication app = new discussionMetaUpdateServiceApplication();

        ClientHttpRequestFactory factory = invokePrivateFactory(app);
        assertNotNull(factory);
    }

    private ClientHttpRequestFactory invokePrivateFactory(discussionMetaUpdateServiceApplication app) {
        try {
            var method = discussionMetaUpdateServiceApplication.class
                    .getDeclaredMethod("getClientHttpRequestFactory");
            method.setAccessible(true);
            return (ClientHttpRequestFactory) method.invoke(app);
        } catch (Exception e) {
            fail("Failed to invoke getClientHttpRequestFactory: " + e.getMessage());
            return null;
        }
    }
}
