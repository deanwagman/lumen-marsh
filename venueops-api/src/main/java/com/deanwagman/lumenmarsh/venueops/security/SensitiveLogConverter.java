package com.deanwagman.lumenmarsh.venueops.security;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class SensitiveLogConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveLogScrubber.scrub(event.getFormattedMessage());
    }
}
