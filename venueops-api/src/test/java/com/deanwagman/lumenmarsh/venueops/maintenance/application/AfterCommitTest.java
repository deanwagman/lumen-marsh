package com.deanwagman.lumenmarsh.venueops.maintenance.application;

import com.deanwagman.lumenmarsh.venueops.support.AfterCommit;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AfterCommitTest {

    @Test
    void runsImmediatelyWhenNoTransactionIsActive() {
        AtomicBoolean ran = new AtomicBoolean(false);
        AfterCommit.run(() -> ran.set(true));
        assertThat(ran).isTrue();
    }
}
