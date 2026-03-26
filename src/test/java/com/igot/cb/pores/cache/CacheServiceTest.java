package com.igot.cb.pores.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheServiceTest {

    private CacheService cacheService;

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CbServerProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cacheService = new CacheService(properties);
        ReflectionTestUtils.setField(cacheService, "jedisPool", jedisPool);
        ReflectionTestUtils.setField(cacheService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(cacheService, "cacheTtl", 3600L);
    }

    @Test
    void testPutCache() throws Exception {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(objectMapper.writeValueAsString(any())).thenReturn("data");

        cacheService.putCache("key", new Object());

        verify(jedis).set(Constants.REDIS_KEY_PREFIX + "key", "data");
    }

    @Test
    void testPutCache_Exception() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("fail"));
        assertDoesNotThrow(()-> cacheService.putCache("key", new Object()));
        // should log error and not throw
    }

    @Test
    void testGetCache() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(Constants.REDIS_KEY_PREFIX + "key")).thenReturn("value");

        String result = cacheService.getCache("key");

        assertEquals("value", result);
    }

    @Test
    void testGetCache_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        String result = cacheService.getCache("key");
        assertNull(result);
    }

    @Test
    void testDeleteCache_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.del(Constants.REDIS_KEY_PREFIX + "key")).thenReturn(1L);

        Long result = cacheService.deleteCache("key");

        assertEquals(1L, result);
    }

    @Test
    void testDeleteCache_NotFound() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.del(Constants.REDIS_KEY_PREFIX + "key")).thenReturn(0L);

        Long result = cacheService.deleteCache("key");

        assertEquals(0L, result);
    }

    @Test
    void testDeleteCache_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        Long result = cacheService.deleteCache("key");

        assertNull(result);
    }

    @Test
    void testUpsertUserToHash_NewField() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hset("key", "field", "value")).thenReturn(1L);
        when(properties.getRedisCommunityUserDataTtlSeconds()).thenReturn(3600L);

        cacheService.upsertUserToHash("key", "field", "value");
        verify(jedis).hset("key", "field", "value");
    }

    @Test
    void testUpsertUserToHash_ExistingField() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hset("key", "field", "value")).thenReturn(0L);
        when(properties.getRedisCommunityUserDataTtlSeconds()).thenReturn(3600L);

        cacheService.upsertUserToHash("key", "field", "value");
        verify(jedis).hset("key", "field", "value");
    }

    @Test
    void testUpsertUserToHash_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        assertDoesNotThrow(()-> cacheService.upsertUserToHash("key", "field", "value"));
    }

    @Test
    void testPutCacheWithoutPrefix() throws Exception {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(objectMapper.writeValueAsString(any())).thenReturn("data");
        when(properties.getRedisCommunityUserDataTtlSeconds()).thenReturn(3600L);

        cacheService.putCacheWithoutPrefix("key", new Object());

        verify(jedis).set("key", "data");
    }

    @Test
    void testPutCacheWithoutPrefix_Exception() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("fail"));
        assertDoesNotThrow(()-> cacheService.putCacheWithoutPrefix("key", new Object()));
    }

    @Test
    void testGetCacheWithoutPrefix() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("key")).thenReturn("value");

        String result = cacheService.getCacheWithoutPrefix("key");

        assertEquals("value", result);
    }

    @Test
    void testGetCacheWithoutPrefix_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        String result = cacheService.getCacheWithoutPrefix("key");
        assertNull(result);
    }

    @Test
    void isRedisHealthy_ShouldReturnTrue_WhenPingSuccessful() {

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn(Constants.REDIS_PONG_RESPONSE);

        boolean result = cacheService.isRedisHealthy();

        assertTrue(result);

        verify(jedis).ping();
        verify(jedis).close();
    }

    @Test
    void isRedisHealthy_ShouldReturnFalse_WhenExceptionOccurs() {

        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis Down"));

        boolean result = cacheService.isRedisHealthy();

        assertFalse(result);
    }
}
