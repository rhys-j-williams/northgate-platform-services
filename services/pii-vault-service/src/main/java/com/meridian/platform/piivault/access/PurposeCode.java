package com.meridian.platform.piivault.access;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PURPOSE_CODE")
public class PurposeCode {

    @Id
    @Column(name = "PURPOSE", length = 40)
    private String purpose;

    @Column(name = "DESCRIPTION", nullable = false, length = 120)
    private String description;

    @Column(name = "ALLOWS_DETOKENISE", nullable = false)
    private boolean allowsDetokenise;

    public String getPurpose() { return purpose; }
    public String getDescription() { return description; }
    public boolean isAllowsDetokenise() { return allowsDetokenise; }
}
