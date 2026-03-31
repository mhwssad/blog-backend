package com.cybzacg.blogbackend.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamUtilsTest {

    @Test
    void streamShouldReturnEmptyStreamForNullCollection() {
        assertEquals(List.of(), StreamUtils.stream(null).toList());
    }

    @Test
    void nonNullStreamShouldFilterNullValues() {
        List<String> result = StreamUtils.nonNullStream(Arrays.asList("a", null, "b")).toList();

        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void mapToSetShouldPreserveEncounterOrder() {
        Set<String> result = StreamUtils.mapToSet(List.of("A", "B", "A", "C"), String::toLowerCase);

        assertEquals(List.of("a", "b", "c"), new ArrayList<>(result));
    }

    @Test
    void distinctToListShouldRemoveDuplicatesAndKeepOrder() {
        List<Integer> result = StreamUtils.distinctToList(List.of(3, 1, 3, 2, 1));

        assertEquals(List.of(3, 1, 2), result);
    }

    @Test
    void toLinkedMapShouldKeepLastValueForDuplicateKey() {
        List<TestItem> items = List.of(
                new TestItem(1L, "article", "first"),
                new TestItem(2L, "comment", "second"),
                new TestItem(1L, "article", "latest")
        );

        Map<Long, String> result = StreamUtils.toLinkedMap(items, TestItem::id, TestItem::name);

        assertEquals(List.of(1L, 2L), new ArrayList<>(result.keySet()));
        assertEquals("latest", result.get(1L));
        assertEquals("second", result.get(2L));
    }

    @Test
    void groupByToListShouldKeepGroupAndElementOrder() {
        List<TestItem> items = List.of(
                new TestItem(1L, "article", "a-1"),
                new TestItem(2L, "comment", "c-1"),
                new TestItem(3L, "article", "a-2")
        );

        Map<String, List<TestItem>> result = StreamUtils.groupByToList(items, TestItem::type);

        assertEquals(List.of("article", "comment"), new ArrayList<>(result.keySet()));
        assertEquals(List.of(items.get(0), items.get(2)), result.get("article"));
        assertEquals(List.of(items.get(1)), result.get("comment"));
    }

    @Test
    void emptyCollectionCollectorsShouldReturnEmptyImmutableResults() {
        assertTrue(StreamUtils.mapToList(List.<String>of(), String::toLowerCase).isEmpty());
        assertTrue(StreamUtils.mapToSet(List.<String>of(), String::toLowerCase).isEmpty());
        assertTrue(StreamUtils.toLinkedMap(List.<TestItem>of(), TestItem::id).isEmpty());
        assertTrue(StreamUtils.groupByToList(List.<TestItem>of(), TestItem::type).isEmpty());
    }

    private record TestItem(Long id, String type, String name) {
    }
}
