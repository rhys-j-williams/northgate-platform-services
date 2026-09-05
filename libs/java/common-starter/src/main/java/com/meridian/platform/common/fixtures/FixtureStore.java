package com.meridian.platform.common.fixtures;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read only view over platform-services/fixtures/meridian-fixtures.json, the serialised output of
 * the shared @meridian/domain-fixtures package. Java services use it to answer when their upstream
 * (Bedrock, the mocks) is not running, and to seed local databases.
 *
 * Location is resolved in order: system property meridian.fixtures.path, env MERIDIAN_FIXTURES,
 * then ../../fixtures/meridian-fixtures.json relative to the working directory (which is what
 * `mvn spring-boot:run` from a service directory gives you), then the classpath.
 *
 * It is deliberately untyped (JsonNode). The TypeScript types are the source of truth and nobody
 * wanted to keep a second set of POJOs in step. Callers pull the handful of fields they need.
 */
public class FixtureStore {

    private static final Logger LOG = LoggerFactory.getLogger(FixtureStore.class);
    private static final String DEFAULT_RELATIVE = "../../fixtures/meridian-fixtures.json";

    private final JsonNode root;

    public FixtureStore(JsonNode root) {
        this.root = root;
    }

    public static FixtureStore load() {
        return load(null);
    }

    public static FixtureStore load(String configuredPath) {
        ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        List<String> candidates = new ArrayList<>();
        if (configuredPath != null && !configuredPath.isBlank()) {
            candidates.add(configuredPath);
        }
        String prop = System.getProperty("meridian.fixtures.path");
        if (prop != null) {
            candidates.add(prop);
        }
        String env = System.getenv("MERIDIAN_FIXTURES");
        if (env != null) {
            candidates.add(env);
        }
        candidates.add(DEFAULT_RELATIVE);
        candidates.add("../" + DEFAULT_RELATIVE);
        for (String candidate : candidates) {
            Path p = Paths.get(candidate);
            if (Files.isRegularFile(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    LOG.info("fixtures loaded from {}", p.toAbsolutePath().normalize());
                    return new FixtureStore(mapper.readTree(in));
                } catch (IOException e) {
                    throw new IllegalStateException("unreadable fixture file " + p, e);
                }
            }
        }
        try (InputStream in = FixtureStore.class.getResourceAsStream("/meridian-fixtures.json")) {
            if (in != null) {
                LOG.info("fixtures loaded from classpath");
                return new FixtureStore(mapper.readTree(in));
            }
        } catch (IOException e) {
            throw new IllegalStateException("unreadable classpath fixtures", e);
        }
        LOG.warn("no fixture file found (tried {}); fixture backed responses will be empty", candidates);
        return new FixtureStore(mapper.createObjectNode());
    }

    public boolean isEmpty() {
        return root == null || !root.has("customers");
    }

    public String seed() {
        return root.path("seed").asText("");
    }

    public List<JsonNode> customers() {
        return list("customers");
    }

    public List<JsonNode> accounts() {
        return list("accounts");
    }

    public List<JsonNode> transactions() {
        return list("transactions");
    }

    public List<JsonNode> alertPreferences() {
        return list("alertPreferences");
    }

    public List<JsonNode> entitlements() {
        return list("entitlements");
    }

    public List<String> bedrockAccountRecords() {
        List<String> out = new ArrayList<>();
        root.path("bedrock").path("accountRecords").forEach(n -> out.add(n.asText()));
        return out;
    }

    public List<String> bedrockTransactionRecords() {
        List<String> out = new ArrayList<>();
        root.path("bedrock").path("transactionRecords").forEach(n -> out.add(n.asText()));
        return out;
    }

    public Optional<JsonNode> customer(String customerId) {
        return first(customers(), n -> customerId.equals(n.path("customerId").asText()));
    }

    public Optional<JsonNode> account(String accountId) {
        return first(accounts(), n -> accountId.equals(n.path("accountId").asText()));
    }

    public List<JsonNode> accountsFor(String customerId) {
        return filter(accounts(), n -> customerId.equals(n.path("customerId").asText()));
    }

    public List<JsonNode> transactionsFor(String accountId) {
        return filter(transactions(), n -> accountId.equals(n.path("accountId").asText()));
    }

    public List<JsonNode> alertPreferencesFor(String customerId) {
        return filter(alertPreferences(), n -> customerId.equals(n.path("customerId").asText()));
    }

    private List<JsonNode> list(String field) {
        JsonNode arr = root.path(field);
        if (!arr.isArray()) {
            return Collections.emptyList();
        }
        List<JsonNode> out = new ArrayList<>(arr.size());
        arr.forEach(out::add);
        return out;
    }

    private static Optional<JsonNode> first(List<JsonNode> nodes, Predicate<JsonNode> p) {
        return nodes.stream().filter(p).findFirst();
    }

    private static List<JsonNode> filter(List<JsonNode> nodes, Predicate<JsonNode> p) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode n : nodes) {
            if (p.test(n)) {
                out.add(n);
            }
        }
        return out;
    }
}
