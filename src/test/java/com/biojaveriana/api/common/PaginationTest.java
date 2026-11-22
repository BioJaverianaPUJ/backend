package com.biojaveriana.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PaginationTest {

    @Test
    @DisplayName("from: maps page metadata onto the response record")
    void fromMapsPageFields() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 10), 25);

        Pagination pagination = Pagination.from(page);

        assertEquals(1, pagination.page());
        assertEquals(10, pagination.size());
        assertEquals(25, pagination.totalElements());
        assertEquals(3, pagination.totalPages());
        assertFalse(pagination.first());
        assertFalse(pagination.last());
    }

    @Test
    @DisplayName("from: an empty first page is both first and last")
    void fromEmptyPage() {
        PageImpl<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        Pagination pagination = Pagination.from(page);

        assertEquals(0, pagination.page());
        assertEquals(0, pagination.totalElements());
        assertEquals(0, pagination.totalPages());
        assertTrue(pagination.first());
        assertTrue(pagination.last());
    }
}
