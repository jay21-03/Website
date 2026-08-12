package com.bautruc.ecommerce.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void mapsSpringPageToRequiredPaginationContract() {
        PageResponse<String> response = PageResponse.from(new PageImpl<>(
                List.of("a", "b"),
                PageRequest.of(0, 20),
                125
        ));

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(125);
        assertThat(response.totalPages()).isEqualTo(7);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
    }

    @Test
    void exposesDefaultPaginationLimits() {
        assertThat(PageResponse.DEFAULT_PAGE).isZero();
        assertThat(PageResponse.DEFAULT_SIZE).isEqualTo(20);
        assertThat(PageResponse.MAX_SIZE).isEqualTo(100);
    }
}
