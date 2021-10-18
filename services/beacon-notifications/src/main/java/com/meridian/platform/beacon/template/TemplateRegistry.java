package com.meridian.platform.beacon.template;

import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.event.EventType;
import com.meridian.platform.beacon.preference.PreferenceDecision;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Templates live on the classpath under templates/{code}.txt: first line is the subject, the rest
 * is the body. Placeholders are {{name}}. Regulatory templates carry the disclosure block from
 * Legal verbatim; do not edit them without a LEGAL ticket (the 2021 wording change was rolled
 * back, see git history for REG_OVERDRAFT_NOTICE).
 *
 * <p>There is no per-locale template; locale only affects amount formatting. es-US was promised
 * in PLAT-0930 and never delivered.
 */
@Component
public class TemplateRegistry {

    static final Map<EventType, String> CODES = new EnumMap<>(EventType.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    static {
        CODES.put(EventType.LARGE_DEBIT, "LARGE_DEBIT");
        CODES.put(EventType.LARGE_CREDIT, "LARGE_CREDIT");
        CODES.put(EventType.LOW_BALANCE, "LOW_BALANCE");
        CODES.put(EventType.OVERDRAFT, "REG_OVERDRAFT_NOTICE");
        CODES.put(EventType.CARD_DECLINED, "CARD_DECLINED");
        CODES.put(EventType.LOGIN_NEW_DEVICE, "LOGIN_NEW_DEVICE");
        CODES.put(EventType.PAYMENT_DUE, "PAYMENT_DUE");
        CODES.put(EventType.RATE_CHANGE, "REG_RATE_CHANGE");
        CODES.put(EventType.DISPUTE_RESOLVED, "REG_ERROR_RESOLUTION");
        CODES.put(EventType.PRIVACY_NOTICE, "REG_PRIVACY_ANNUAL");
    }

    private final Map<String, String> sources = new HashMap<>();

    public String codeFor(EventType type) {
        return CODES.get(type);
    }

    public RenderedTemplate render(AccountEvent event, PreferenceDecision decision) {
        String code = codeFor(event.getEventType());
        String source = load(code);
        Map<String, String> model = new HashMap<>();
        model.put("customerId", event.getCustomerId());
        model.put("accountId", event.getAccountId() == null ? "" : event.getAccountId());
        model.put("accountTail", event.getAccountId() == null || event.getAccountId().length() < 4 ? "" :
            event.getAccountId().substring(event.getAccountId().length() - 4));
        model.put("amount", money(event.getAmountMinor()));
        model.put("balance", money(event.getBalanceAfterMinor()));
        model.put("description", event.getDescription() == null ? "" : event.getDescription());
        model.put("occurredAt", STAMP.format(event.getOccurredAt().atZone(ZoneId.of("America/New_York"))));
        model.put("eventId", event.getEventId());
        String filled = substitute(source, model);
        int nl = filled.indexOf('\n');
        String subject = nl < 0 ? filled : filled.substring(0, nl).trim();
        String body = nl < 0 ? "" : filled.substring(nl + 1).trim();
        return new RenderedTemplate(code, subject, body);
    }

    static String substitute(String source, Map<String, String> model) {
        Matcher m = PLACEHOLDER.matcher(source);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String value = model.getOrDefault(m.group(1), "");
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    static String money(long minor) {
        BigDecimal value = BigDecimal.valueOf(Math.abs(minor)).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
        String plain = value.toPlainString();
        int dot = plain.indexOf('.');
        StringBuilder whole = new StringBuilder(plain.substring(0, dot));
        for (int i = whole.length() - 3; i > 0; i -= 3) {
            whole.insert(i, ',');
        }
        return (minor < 0 ? "-$" : "$") + whole + plain.substring(dot);
    }

    private String load(String code) {
        return sources.computeIfAbsent(code, c -> {
            try (InputStream in = getClass().getResourceAsStream("/templates/" + c + ".txt")) {
                if (in == null) {
                    throw new IllegalStateException("template " + c + " missing from classpath");
                }
                return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("template " + c + " unreadable", e);
            }
        });
    }
}
