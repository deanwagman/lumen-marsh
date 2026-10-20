package com.deanwagman.lumenmarsh.venueops.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

final class DemoLeaseStore {

    enum State {
        PREPARING, ACTIVE, RESETTING, EXPIRED, FAILED
    }

    record Lease(
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            Instant hardExpiresAt,
            Instant lastActivityAt,
            State state,
            String failureDetail
    ) {
        boolean liveAt(Instant now, DemoSandboxProperties properties) {
            if (state == State.PREPARING || state == State.RESETTING) {
                return issuedAt != null && hardExpiresAt != null && now.isBefore(hardExpiresAt);
            }
            return state == State.ACTIVE
                    && expiresAt != null && hardExpiresAt != null && lastActivityAt != null
                    && now.isBefore(expiresAt)
                    && now.isBefore(hardExpiresAt)
                    && now.isBefore(lastActivityAt.plus(properties.inactivityTimeout()));
        }
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    DemoLeaseStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    Optional<Lease> reserve(String tokenHash, Instant now, DemoSandboxProperties properties) {
        return transactions.execute(status -> {
            Lease current = lock();
            if (current.liveAt(now, properties)) {
                return Optional.empty();
            }
            Instant hardExpiry = now.plus(properties.hardMaximum());
            jdbc.update("""
                    UPDATE demo_sandbox_lease
                    SET token_hash=?, issued_at=?, expires_at=NULL, hard_expires_at=?,
                        last_activity_at=?, state='PREPARING', failure_detail=NULL, version=version+1
                    WHERE singleton_id=1
                    """, tokenHash, now, hardExpiry, now);
            return Optional.of(find());
        });
    }

    Lease activate(String tokenHash, Instant now, DemoSandboxProperties properties) {
        int changed = jdbc.update("""
                UPDATE demo_sandbox_lease
                SET state='ACTIVE', expires_at=LEAST(?, hard_expires_at),
                    last_activity_at=?, failure_detail=NULL, version=version+1
                WHERE singleton_id=1 AND token_hash=? AND state='PREPARING'
                """, now.plus(properties.leaseDuration()), now, tokenHash);
        if (changed != 1) {
            throw new IllegalStateException("The reserved sandbox lease is no longer current.");
        }
        return find();
    }

    Optional<Lease> authenticate(String tokenHash, Instant now, DemoSandboxProperties properties, boolean touch) {
        Lease lease = find();
        if (!lease.liveAt(now, properties) || lease.state() != State.ACTIVE
                || !DemoTokens.constantTimeEquals(tokenHash, lease.tokenHash())) {
            return Optional.empty();
        }
        if (touch) {
            jdbc.update("""
                    UPDATE demo_sandbox_lease SET last_activity_at=?, version=version+1
                    WHERE singleton_id=1 AND token_hash=? AND state='ACTIVE'
                    """, now, tokenHash);
            lease = find();
        }
        return Optional.of(lease);
    }

    boolean beginReset(String tokenHash, Instant now, DemoSandboxProperties properties) {
        Optional<Lease> lease = authenticate(tokenHash, now, properties, true);
        return lease.isPresent() && jdbc.update("""
                UPDATE demo_sandbox_lease SET state='RESETTING', version=version+1
                WHERE singleton_id=1 AND token_hash=? AND state='ACTIVE'
                """, tokenHash) == 1;
    }

    boolean beginExpiryReset(String tokenHash, Instant now) {
        return jdbc.update("""
                UPDATE demo_sandbox_lease
                SET state='RESETTING', hard_expires_at=?, version=version+1
                WHERE singleton_id=1 AND token_hash=? AND state IN ('ACTIVE','PREPARING','RESETTING')
                """, now.plusSeconds(60), tokenHash) == 1;
    }

    Lease finishReset(String tokenHash, Instant now, DemoSandboxProperties properties) {
        int changed = jdbc.update("""
                UPDATE demo_sandbox_lease
                SET state='ACTIVE', expires_at=LEAST(?, hard_expires_at), last_activity_at=?,
                    failure_detail=NULL, version=version+1
                WHERE singleton_id=1 AND token_hash=? AND state='RESETTING'
                """, now.plus(properties.leaseDuration()), now, tokenHash);
        if (changed != 1) {
            throw new IllegalStateException("The resetting sandbox lease is no longer current.");
        }
        return find();
    }

    void expire(String tokenHash) {
        jdbc.update("""
                UPDATE demo_sandbox_lease
                SET state='EXPIRED', token_hash=NULL, expires_at=NULL, last_activity_at=NULL,
                    failure_detail=NULL, version=version+1
                WHERE singleton_id=1 AND token_hash=?
                """, tokenHash);
    }

    void fail(String tokenHash, String detail) {
        jdbc.update("""
                UPDATE demo_sandbox_lease
                SET state='FAILED', failure_detail=?, version=version+1
                WHERE singleton_id=1 AND token_hash=?
                """, detail == null ? "Reset failed" : detail.substring(0, Math.min(255, detail.length())), tokenHash);
    }

    Lease find() {
        return jdbc.queryForObject("""
                SELECT token_hash, issued_at, expires_at, hard_expires_at, last_activity_at, state, failure_detail
                FROM demo_sandbox_lease WHERE singleton_id=1
                """, DemoLeaseStore::map);
    }

    private Lease lock() {
        return jdbc.queryForObject("""
                SELECT token_hash, issued_at, expires_at, hard_expires_at, last_activity_at, state, failure_detail
                FROM demo_sandbox_lease WHERE singleton_id=1 FOR UPDATE
                """, DemoLeaseStore::map);
    }

    private static Lease map(ResultSet rs, int row) throws SQLException {
        return new Lease(
                rs.getString("token_hash"),
                instant(rs, "issued_at"),
                instant(rs, "expires_at"),
                instant(rs, "hard_expires_at"),
                instant(rs, "last_activity_at"),
                State.valueOf(rs.getString("state")),
                rs.getString("failure_detail")
        );
    }

    private static Instant instant(ResultSet rs, String name) throws SQLException {
        return rs.getTimestamp(name) == null ? null : rs.getTimestamp(name).toInstant();
    }
}
