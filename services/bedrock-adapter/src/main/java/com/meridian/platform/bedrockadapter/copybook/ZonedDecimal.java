package com.meridian.platform.bedrockadapter.copybook;

/**
 * Signed zoned decimal, sign trailing included, as Bedrock writes it. Port of
 * libs/ts/domain-fixtures/src/bedrock.ts, which is the reference implementation; the two are
 * kept in step by CopybookFixtureParityTest reading the same examples out of copybooks/README.md.
 *
 * -1234 minor units in 10 positions is "000000123M". Getting this wrong was INC0048812.
 */
public final class ZonedDecimal {

    private static final String NEGATIVE = "}JKLMNOPQR";
    private static final String POSITIVE = "{ABCDEFGHI";

    private ZonedDecimal() {
    }

    public static String encode(long minorUnits, int width) {
        boolean negative = minorUnits < 0;
        String digits = Long.toString(Math.abs(minorUnits));
        if (digits.length() > width) {
            throw new IllegalArgumentException("amount " + minorUnits + " does not fit in " + width + " positions");
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = digits.length(); i < width; i++) {
            sb.append('0');
        }
        sb.append(digits);
        int last = sb.charAt(width - 1) - '0';
        sb.setCharAt(width - 1, (negative ? NEGATIVE : POSITIVE).charAt(last));
        return sb.toString();
    }

    public static long decode(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("empty zoned decimal field");
        }
        char sign = field.charAt(field.length() - 1);
        String lead = field.substring(0, field.length() - 1);
        int neg = NEGATIVE.indexOf(sign);
        int pos = POSITIVE.indexOf(sign);
        if (neg < 0 && pos < 0) {
            throw new IllegalArgumentException("not a zoned decimal field: " + field);
        }
        int digit = neg < 0 ? pos : neg;
        long magnitude = Long.parseLong(lead + digit);
        return neg < 0 ? magnitude : -magnitude;
    }
}
