package com.igot.cb.pores.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CbServerPropertiesTest {

    @Test
    void testGettersAndSetters() {
        CbServerProperties props = new CbServerProperties();

        props.setElasticCommunityJsonPath("path/to/community.json");
        props.setDiscussionEntity("discussion_entity");
        props.setUserPostCountSearchCriteria("user_post_criteria");
        props.setKafkaUserPostCountTopic("kafka_topic");

        assertEquals("path/to/community.json", props.getElasticCommunityJsonPath());
        assertEquals("discussion_entity", props.getDiscussionEntity());
        assertEquals("user_post_criteria", props.getUserPostCountSearchCriteria());
        assertEquals("kafka_topic", props.getKafkaUserPostCountTopic());
    }
}
