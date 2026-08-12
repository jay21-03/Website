package com.bautruc.ecommerce.identity.application;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AdminEmailsParser {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    public Set<String> parse(Collection<String> rawValues) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        if (rawValues == null) {
            return emails;
        }
        for (String rawValue : rawValues) {
            emails.addAll(parse(rawValue));
        }
        return emails;
    }

    public Set<String> parse(String rawValue) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        if (rawValue == null || rawValue.isBlank()) {
            return emails;
        }
        for (String candidate : rawValue.split(",")) {
            String normalized = candidate.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                throw new IllegalArgumentException("Invalid ADMIN_EMAILS entry: " + normalized);
            }
            emails.add(normalized);
        }
        return emails;
    }
}
