package jgnash.util;

import java.util.Set;

import jgnash.time.DateUtils;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class DateFormatTest {

    @Test
    public void dateFormatTest() {
        final Set<String> dateFormats = DateUtils.getAvailableDateFormats();

        assertNotEquals(0, dateFormats.size());

        dateFormats.forEach(System.out::println);
    }
}