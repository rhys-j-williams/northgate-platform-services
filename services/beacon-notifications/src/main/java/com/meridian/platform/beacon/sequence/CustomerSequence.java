package com.meridian.platform.beacon.sequence;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "BEACON_CUSTOMER_SEQ")
public class CustomerSequence {

    @Id
    @Column(name = "CUSTOMER_ID", length = 16)
    private String customerId;

    @Column(name = "LAST_DISPATCHED", nullable = false)
    private long lastDispatched;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public long getLastDispatched() {
        return lastDispatched;
    }

    public void setLastDispatched(long lastDispatched) {
        this.lastDispatched = lastDispatched;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
