package jgnash.util;

import java.util.Set;

import jgnash.time.DateUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DateFormatTest {

    @Test
    void dateFormatTest() {
        final Set<String> dateFormats = DateUtils.getAvailableShortDateFormats();

        assertNotEquals(0, dateFormats.size());

        dateFormats.forEach(System.out::println);
    }
}