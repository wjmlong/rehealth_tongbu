package org.jeecg.modules.rehealth.service.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HealthAgentProfileUpdateExtractor {
    private static final String SEGMENT_START = "(?:^|[，,。；;\\n])\\s*";
    private static final String SEGMENT_END = "(?=\\s*(?:[，,。；;\\n]|$))";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE;
    private static final Pattern NAME = Pattern.compile(
            SEGMENT_START
                    + "(?:我叫|我的姓名\\s*(?:是|为|[:：])?|姓名\\s*[:：])\\s*"
                    + "([\\p{IsHan}·]{1,16}|[A-Za-z][A-Za-z .'-]{0,31}?)"
                    + SEGMENT_END,
            FLAGS
    );
    private static final Pattern GENDER = Pattern.compile(
            SEGMENT_START
                    + "(?:(?:我的)?性别\\s*(?:是|为|[:：])?|我是\\s*)"
                    + "(男性|女性|男生|女生|男|女)"
                    + SEGMENT_END,
            FLAGS
    );
    private static final Pattern AGE = Pattern.compile(
            SEGMENT_START
                    + "(?:我今年|我的年龄\\s*(?:是|为|[:：])?|年龄\\s*[:：]|我是\\s*)"
                    + "(\\d{1,3})\\s*岁?"
                    + SEGMENT_END,
            FLAGS
    );
    private static final Pattern HEIGHT = Pattern.compile(
            SEGMENT_START
                    + "(?:(?:我的)?身高\\s*(?:是|为|[:：])?)\\s*"
                    + "(\\d{1,3}(?:\\.\\d{1,2})?|[12]\\.\\d{1,2})\\s*"
                    + "(cm|厘米|公分|米)?"
                    + SEGMENT_END,
            FLAGS
    );
    private static final Pattern WEIGHT = Pattern.compile(
            SEGMENT_START
                    + "(?:(?:我的)?体重\\s*(?:是|为|[:：])?)\\s*"
                    + "(\\d{1,3}(?:\\.\\d{1,2})?)\\s*"
                    + "(kg|公斤|千克|斤)?"
                    + SEGMENT_END,
            FLAGS
    );

    public HealthAgentProfilePatch extract(String message) {
        if (message == null || message.isBlank()) {
            return HealthAgentProfilePatch.empty();
        }
        return new HealthAgentProfilePatch(
                matchedText(NAME, message),
                gender(message),
                age(message),
                heightCm(message),
                weightKg(message)
        );
    }

    private String gender(String message) {
        String value = matchedText(GENDER, message);
        if (value == null) {
            return null;
        }
        return value.startsWith("男") ? "male" : "female";
    }

    private Integer age(String message) {
        String value = matchedText(AGE, message);
        if (value == null) {
            return null;
        }
        int parsed = Integer.parseInt(value);
        return parsed >= 1 && parsed <= 120 ? parsed : null;
    }

    private Double heightCm(String message) {
        Matcher matcher = HEIGHT.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = normalized(matcher.group(2));
        if ("米".equals(unit) || (unit == null && value <= 3.0)) {
            value *= 100.0;
        }
        value = rounded(value);
        return value >= 50.0 && value <= 250.0 ? value : null;
    }

    private Double weightKg(String message) {
        Matcher matcher = WEIGHT.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        double value = Double.parseDouble(matcher.group(1));
        if ("斤".equals(normalized(matcher.group(2)))) {
            value /= 2.0;
        }
        value = rounded(value);
        return value >= 2.0 && value <= 500.0 ? value : null;
    }

    private String matchedText(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).strip();
        return value.isEmpty() ? null : value;
    }

    private String normalized(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private double rounded(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

record HealthAgentProfilePatch(
        String name,
        String gender,
        Integer age,
        Double heightCm,
        Double weightKg
) {
    static HealthAgentProfilePatch empty() {
        return new HealthAgentProfilePatch(null, null, null, null, null);
    }

    boolean isEmpty() {
        return name == null && gender == null && age == null && heightCm == null && weightKg == null;
    }
}
