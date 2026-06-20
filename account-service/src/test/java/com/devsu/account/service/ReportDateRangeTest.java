package com.devsu.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsu.account.exception.InvalidReportRangeException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReportDateRangeTest {

    @Test
    void parsesInclusiveDateRange() {
        ReportDateRange range = ReportDateRange.parse("2026-06-01,2026-06-30");

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
    }

    @Test
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> ReportDateRange.parse("2026-06-01"))
                .isInstanceOf(InvalidReportRangeException.class);
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> ReportDateRange.parse("2026-06-30,2026-06-01"))
                .isInstanceOf(InvalidReportRangeException.class)
                .hasMessage("La fecha inicial no puede ser posterior a la fecha final");
    }
}
