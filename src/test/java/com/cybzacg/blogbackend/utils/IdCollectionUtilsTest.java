package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdCollectionUtilsTest {

    @Test
    void distinctNonNullIdsShouldPreserveOrderAndRemoveDuplicates() {
        List<Long> result = IdCollectionUtils.distinctNonNullIds(
                List.of(3L, 2L, 3L, 5L),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "ID不能为空");

        assertEquals(List.of(3L, 2L, 5L), result);
    }

    @Test
    void requireUniqueNonNullIdsShouldRejectDuplicateValues() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                IdCollectionUtils.requireUniqueNonNullIds(
                        List.of(3L, 2L, 3L),
                        ResultErrorCode.ILLEGAL_ARGUMENT,
                        "ID不能为空",
                        "ID重复"));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("ID重复", exception.getMessage());
    }

    @Test
    void toCommaSeparatedIdsShouldReturnNullForEmptyList() {
        assertNull(IdCollectionUtils.toCommaSeparatedIds(List.of()));
        assertEquals("7,9,11", IdCollectionUtils.toCommaSeparatedIds(List.of(7L, 9L, 11L)));
    }
}
