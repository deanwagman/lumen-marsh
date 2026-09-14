package com.deanwagman.lumenmarsh.venueops.security;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;

public class SensitiveThrowableConverter extends ThrowableProxyConverter {

    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        return SensitiveLogScrubber.scrub(super.throwableProxyToString(tp));
    }
}
