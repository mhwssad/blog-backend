package com.cybzacg.blogbackend.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionUtilsTest {

    @Test
    void newInstanceShouldSupportPrivateConstructor() {
        PrivateConstructorHolder instance = ReflectionUtils.newInstance(PrivateConstructorHolder.class);

        assertEquals("created", instance.getValue());
    }

    @Test
    void readFieldShouldTraverseSuperclassHierarchy() {
        assertEquals("parent-secret", ReflectionUtils.readField(new ChildHolder(), "secret").orElse(null));
    }

    @Test
    void writeFieldShouldUpdatePrivateField() {
        ChildHolder holder = new ChildHolder();

        assertTrue(ReflectionUtils.writeField(holder, "name", "updated"));
        assertEquals("updated", ReflectionUtils.readField(holder, "name").orElse(null));
    }

    @Test
    void invokeNoArgMethodShouldCallPrivateMethod() {
        assertEquals("hello-codex", ReflectionUtils.invokeNoArgMethod(new ChildHolder(), "buildMessage").orElse(null));
    }

    @Test
    void readPropertyShouldFallbackToGetterAndField() {
        ChildHolder holder = new ChildHolder();

        assertEquals(18, ReflectionUtils.readProperty(holder, "age").orElse(null));
        assertEquals("child", ReflectionUtils.readProperty(holder, "name").orElse(null));
        assertTrue((Boolean) ReflectionUtils.readProperty(holder, "active").orElse(false));
    }

    @Test
    void missingMembersShouldReturnEmptyOrFalse() {
        ChildHolder holder = new ChildHolder();

        assertTrue(ReflectionUtils.readField(holder, "missing").isEmpty());
        assertTrue(ReflectionUtils.invokeNoArgMethod(holder, "missingMethod").isEmpty());
        assertFalse(ReflectionUtils.writeField(holder, "missing", "value"));
    }

    private static class ParentHolder {
        private final String secret = "parent-secret";
    }

    private static class ChildHolder extends ParentHolder {
        private String name = "child";

        private int getAge() {
            return 18;
        }

        private boolean isActive() {
            return true;
        }

        private String buildMessage() {
            return "hello-codex";
        }
    }

    private static final class PrivateConstructorHolder {
        private final String value;

        private PrivateConstructorHolder() {
            this.value = "created";
        }

        private String getValue() {
            return value;
        }
    }
}
