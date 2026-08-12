package com.bautruc.ecommerce.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class AdminEmailsParserTest {
    private final AdminEmailsParser parser = new AdminEmailsParser();

    @Test
    void parsesCommaSeparatedEmailsDeterministically() {
        assertThat(parser.parse(" Admin1@gmail.com,admin2@gmail.com, ADMIN1@gmail.com ,, "))
                .containsExactly("admin1@gmail.com", "admin2@gmail.com");
    }

    @Test
    void parsesListValuesAndCollapsesCaseInsensitiveDuplicates() {
        assertThat(parser.parse(List.of("Admin1@gmail.com", " admin2@gmail.com,ADMIN1@gmail.com ", "")))
                .containsExactly("admin1@gmail.com", "admin2@gmail.com");
    }

    @Test
    void emptyAndWhitespaceInputReturnEmptySet() {
        assertThat(parser.parse((String) null)).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
        assertThat(parser.parse(List.of("", "   "))).isEmpty();
    }

    @Test
    void invalidNonBlankEmailIsRejected() {
        assertThatThrownBy(() -> parser.parse("admin@example.com,not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not-an-email");
    }
}
