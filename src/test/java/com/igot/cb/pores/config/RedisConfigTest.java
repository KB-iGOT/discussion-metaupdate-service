package com.igot.cb.pores.config;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    @Test
    void testJedisPoolBeanCreation() throws Exception {
        RedisConfig config = new RedisConfig();

        setField(config, "redisHost", "localhost");
        setField(config, "redisPort", 6379);

        JedisPool jedisPool = config.jedisPool();

        assertNotNull(jedisPool);
        jedisPool.close();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
