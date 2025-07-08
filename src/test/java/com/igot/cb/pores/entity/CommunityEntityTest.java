package com.igot.cb.pores.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class CommunityEntityTest {

    @Test
    void testNoArgsConstructorAndSettersGetters() {
        CommunityEntity entity = new CommunityEntity();

        String communityId = "community123";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        JsonNode data = new ObjectMapper().createObjectNode().put("key", "value");

        entity.setCommunityId(communityId);
        entity.setData(data);
        entity.setCreatedOn(now);
        entity.setUpdatedOn(now);
        entity.setCreated_by("creator");
        entity.setActive(true);

        assertEquals(communityId, entity.getCommunityId());
        assertEquals(data, entity.getData());
        assertEquals(now, entity.getCreatedOn());
        assertEquals(now, entity.getUpdatedOn());
        assertEquals("creator", entity.getCreated_by());
        assertTrue(entity.isActive());
    }

    @Test
    void testAllArgsConstructor() {
        String communityId = "community456";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        JsonNode data = new ObjectMapper().createObjectNode().put("anotherKey", "anotherValue");

        CommunityEntity entity = new CommunityEntity(
                communityId,
                data,
                now,
                now,
                "creator2",
                false
        );

        assertEquals(communityId, entity.getCommunityId());
        assertEquals(data, entity.getData());
        assertEquals(now, entity.getCreatedOn());
        assertEquals(now, entity.getUpdatedOn());
        assertEquals("creator2", entity.getCreated_by());
        assertFalse(entity.isActive());
    }
}

