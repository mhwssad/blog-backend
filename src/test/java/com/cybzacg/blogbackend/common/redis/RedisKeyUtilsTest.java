package com.cybzacg.blogbackend.common.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedisKeyUtilsTest {
    @Test
    void buildShouldJoinNonBlankParts() {
        assertEquals("auth:user:7", RedisKeyUtils.build(" auth ", null, "user", " ", 7L));
    }

    @Test
    void buildShouldRejectEmptyResult() {
        assertThrows(IllegalArgumentException.class, () -> RedisKeyUtils.build(null, " ", ""));
        assertThrows(IllegalArgumentException.class, RedisKeyUtils::build);
    }
}
