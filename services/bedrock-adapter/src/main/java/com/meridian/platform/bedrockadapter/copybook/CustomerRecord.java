package com.meridian.platform.bedrockadapter.copybook;

import java.time.LocalDate;

/** MTBCUST, 200 bytes. Tax id arrives already tokenised by pii-vault-service; never the raw value. */
public class CustomerRecord {

    private String customerId;
    private String segment;
    private String firstName;
    private String lastName;
    private String orgName;
    private String taxIdToken;
    private String mobile;
    private LocalDate enrolledDate;
    private String postal;
    private String state;
    private boolean deceased;
    private boolean vulnerable;
    private boolean paperless;
    private boolean doNotCall;

    public static CustomerRecord decode(String record) {
        if (record.length() < Copybook.MTBCUST_LENGTH) {
            throw new IllegalArgumentException("MTBCUST record short: " + record.length());
        }
        CustomerRecord r = new CustomerRecord();
        r.customerId = Fixed.trimmed(record, 0, 12);
        r.segment = Fixed.trimmed(record, 12, 16);
        r.firstName = Fixed.trimmed(record, 28, 24);
        r.lastName = Fixed.trimmed(record, 52, 32);
        r.orgName = Fixed.trimmed(record, 84, 40);
        r.taxIdToken = Fixed.trimmed(record, 124, 24);
        r.mobile = Fixed.trimmed(record, 148, 16);
        r.enrolledDate = Fixed.date(Fixed.slice(record, 164, 8));
        r.postal = Fixed.trimmed(record, 172, 10);
        r.state = Fixed.trimmed(record, 182, 2);
        r.deceased = record.charAt(184) == 'Y';
        r.vulnerable = record.charAt(185) == 'Y';
        r.paperless = record.charAt(186) == 'Y';
        r.doNotCall = record.charAt(187) == 'Y';
        return r;
    }

    public String encode() {
        return Fixed.text(customerId, 12)
            + Fixed.text(segment, 16)
            + Fixed.text(firstName, 24)
            + Fixed.text(lastName, 32)
            + Fixed.text(orgName, 40)
            + Fixed.text(taxIdToken, 24)
            + Fixed.text(mobile, 16)
            + Fixed.date(enrolledDate)
            + Fixed.text(postal, 10)
            + Fixed.text(state, 2)
            + (deceased ? 'Y' : 'N')
            + (vulnerable ? 'Y' : 'N')
            + (paperless ? 'Y' : 'N')
            + (doNotCall ? 'Y' : 'N')
            + Fixed.text("", 12);
    }

    /** NAME scope: Bedrock blanks everything after the names. We do the same on the way out. */
    public CustomerRecord nameScopeOnly() {
        CustomerRecord r = new CustomerRecord();
        r.customerId = customerId;
        r.segment = segment;
        r.firstName = firstName;
        r.lastName = lastName;
        r.orgName = orgName;
        return r;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getTaxIdToken() {
        return taxIdToken;
    }

    public void setTaxIdToken(String taxIdToken) {
        this.taxIdToken = taxIdToken;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public LocalDate getEnrolledDate() {
        return enrolledDate;
    }

    public void setEnrolledDate(LocalDate enrolledDate) {
        this.enrolledDate = enrolledDate;
    }

    public String getPostal() {
        return postal;
    }

    public void setPostal(String postal) {
        this.postal = postal;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isDeceased() {
        return deceased;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    public boolean isVulnerable() {
        return vulnerable;
    }

    public void setVulnerable(boolean vulnerable) {
        this.vulnerable = vulnerable;
    }

    public boolean isPaperless() {
        return paperless;
    }

    public void setPaperless(boolean paperless) {
        this.paperless = paperless;
    }

    public boolean isDoNotCall() {
        return doNotCall;
    }

    public void setDoNotCall(boolean doNotCall) {
        this.doNotCall = doNotCall;
    }
}
