package com.cybzacg.blogbackend.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 树结构遍历工具类。
 *
 * <p>提供通用的 BFS/DFS 树遍历能力，适用于评论子树、菜单树等层级数据结构。
 */
public final class TreeTraversalUtils {

    private TreeTraversalUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 通过 BFS 收集树子树的所有节点。
     *
     * @param root            根节点
     * @param idGetter        获取节点 ID 的函数
     * @param parentIdGetter  获取父节点 ID 的函数
     * @param allNodes        当前上下文中的所有节点（已按 target 预加载）
     * @param <T>             节点类型
     * @return 从根节点开始的所有后代节点列表（包含 root 自身）
     */
    public static <T> List<T> bfsCollectSubtree(
            T root,
            Function<T, Long> idGetter,
            Function<T, Long> parentIdGetter,
            List<T> allNodes) {
        if (root == null || allNodes == null || allNodes.isEmpty()) {
            return List.of();
        }
        Map<Long, List<T>> byParent = allNodes.stream()
                .collect(Collectors.groupingBy(parentIdGetter));
        ArrayDeque<T> queue = new ArrayDeque<>();
        queue.add(root);
        List<T> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            T current = queue.poll();
            if (!visited.add(idGetter.apply(current))) {
                continue;
            }
            result.add(current);
            for (T child : byParent.getOrDefault(idGetter.apply(current), List.of())) {
                queue.add(child);
            }
        }
        return result;
    }
}
