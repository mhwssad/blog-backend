package com.cybzacg.blogbackend.utils;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringBeanUtilsTest {

    @Test
    void getBeansOfTypeShouldReturnRegisteredBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("alphaBean", AlphaBean.class);
            context.registerBean("betaBean", BetaBean.class);
            context.refresh();

            Map<String, DemoBean> beans = SpringBeanUtils.getBeansOfType(context, DemoBean.class);

            assertEquals(2, beans.size());
            assertTrue(beans.containsKey("alphaBean"));
            assertTrue(beans.containsKey("betaBean"));
            assertThrows(UnsupportedOperationException.class, () -> beans.put("extra", new AlphaBean()));
        }
    }

    @Test
    void getBeansWithAnnotationShouldReturnAnnotatedBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("markedBean", MarkedBean.class);
            context.registerBean("plainBean", PlainBean.class);
            context.refresh();

            Map<String, Object> beans = SpringBeanUtils.getBeansWithAnnotation(context, Marker.class);

            assertEquals(1, beans.size());
            assertTrue(beans.containsKey("markedBean"));
        }
    }

    @Test
    void resolveOrderShouldSupportOrderedAndOrderAnnotation() {
        assertEquals(5, SpringBeanUtils.resolveOrder(new OrderedBean()));
        assertEquals(9, SpringBeanUtils.resolveOrder(new AnnotatedOrderBean()));
        assertEquals(Ordered.LOWEST_PRECEDENCE, SpringBeanUtils.resolveOrder(new PlainBean()));
    }

    @Test
    void resolveTargetClassShouldUnwrapProxy() {
        ProxyFactory proxyFactory = new ProxyFactory(new AnnotatedOrderBean());

        Object proxy = proxyFactory.getProxy();

        assertEquals(AnnotatedOrderBean.class, SpringBeanUtils.resolveTargetClass(proxy));
    }

    private interface DemoBean {
    }

    private static final class AlphaBean implements DemoBean {
    }

    private static final class BetaBean implements DemoBean {
    }

    @Marker
    private static final class MarkedBean {
    }

    private static final class OrderedBean implements Ordered {
        @Override
        public int getOrder() {
            return 5;
        }
    }

    @Order(9)
    public static class AnnotatedOrderBean {
    }

    private static class PlainBean {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Marker {
    }
}
