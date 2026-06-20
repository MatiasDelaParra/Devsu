package com.devsu.account.service;

import com.devsu.account.exception.InvalidReportRangeException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public record ReportDateRange(LocalDate from, LocalDate to) {

    public static ReportDateRange parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidReportRangeException(
                    "El parámetro fecha es obligatorio y debe tener formato YYYY-MM-DD,YYYY-MM-DD"
            );
        }

        String[] dates = value.split(",", -1);
        if (dates.length != 2) {
            throw invalidFormat();
        }

        try {
            LocalDate from = LocalDate.parse(dates[0].trim());
            LocalDate to = LocalDate.parse(dates[1].trim());
            if (from.isAfter(to)) {
                throw new InvalidReportRangeException(
                        "La fecha inicial no puede ser posterior a la fecha final"
                );
            }
            return new ReportDateRange(from, to);
        } catch (DateTimeException exception) {
            throw invalidFormat();
        }
    }

    public Instant fromInclusive() {
        return from.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public Instant toExclusive() {
        return to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static InvalidReportRangeException invalidFormat() {
        return new InvalidReportRangeException(
                "El parámetro fecha debe tener formato YYYY-MM-DD,YYYY-MM-DD"
        );
    }
}
