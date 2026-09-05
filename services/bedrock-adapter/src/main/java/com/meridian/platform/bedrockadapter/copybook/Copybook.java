package com.meridian.platform.bedrockadapter.copybook;

/**
 * Field offsets for the six copybooks this adapter speaks. Hand transcribed from copybooks/*.cpy;
 * the layout tests assert the record lengths so a stale offset fails fast.
 *
 * TODO PLAT-2231: generate this from the .cpy files instead of maintaining it by hand.
 */
public final class Copybook {

    private Copybook() {
    }

    public static final int MTBACCT_LENGTH = 136;
    public static final int MTBTRAN_LENGTH = 160;
    public static final int MTBCUST_LENGTH = 200;

    public static final int ACCT_INQ_REQUEST_LENGTH = 64;
    public static final int ACCT_INQ_RESPONSE_LENGTH = 200;
    public static final int TXN_POST_REQUEST_LENGTH = 160;
    public static final int TXN_POST_RESPONSE_LENGTH = 96;
    public static final int CUST_PROF_REQUEST_LENGTH = 56;
    public static final int CUST_PROF_RESPONSE_LENGTH = 264;

    /** Common request header, 40 bytes: tran code (4), correlation id (32), channel (4). */
    public static final int HDR_LENGTH = 40;

    public static final String TRAN_ACCT_INQ = "MTAI";
    public static final String TRAN_TXN_POST = "MTTP";
    public static final String TRAN_CUST_PROF = "MTCP";

    public static final String RC_OK = "00";
    public static final String RC_NOT_FOUND = "04";
    public static final String RC_RESTRICTED = "08";
    public static final String RC_UNAVAILABLE = "12";
    public static final String RC_ABEND = "16";
    /** MTTP only: duplicate idempotency key, original result echoed. */
    public static final String RC_DUPLICATE = "20";
    /** MTTP only: reversal refused. */
    public static final String RC_REVERSAL_REFUSED = "24";
}
