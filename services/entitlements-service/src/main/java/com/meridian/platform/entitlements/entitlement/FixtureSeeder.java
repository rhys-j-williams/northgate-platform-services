package com.meridian.platform.entitlements.entitlement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds ENTITLEMENT from platform-services/fixtures/meridian-fixtures.json when the table is empty.
 * Same lookup order as common-starter's FixtureStore (property, MERIDIAN_FIXTURES, ../../fixtures).
 * Local and test only; in the bank the table is populated by the onboarding batch.
 */
@Component
public class FixtureSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(FixtureSeeder.class);

    private final EntitlementRepository repository;
    private final String configuredPath;

    public FixtureSeeder(EntitlementRepository repository, @Value("${meridian.fixtures.path:}") String configuredPath) {
        this.repository = repository;
        this.configuredPath = configuredPath;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        JsonNode root = locate();
        if (root == null) {
            LOG.warn("fixtures not found, ENTITLEMENT left empty");
            return;
        }
        Instant now = Instant.now();
        List<Entitlement> batch = new ArrayList<>();
        for (JsonNode n : root.path("entitlements")) {
            Entitlement e = new Entitlement();
            e.setEntitlementId(n.path("entitlementId").asText());
            e.setOrganisationId(n.path("organisationId").asText());
            e.setCustomerId(n.path("customerId").asText());
            e.setUserHandle(n.path("userHandle").asText());
            e.setRoleCode(n.path("role").asText());
            e.setDualApprovalRequired(n.path("dualApprovalRequired").asBoolean(false));
            e.setLimitPerTxnMinor(n.hasNonNull("limitPerTransactionMinor") ? n.get("limitPerTransactionMinor").asLong() : null);
            e.setLimitPerDayMinor(n.hasNonNull("limitPerDayMinor") ? n.get("limitPerDayMinor").asLong() : null);
            e.setStatus("ACTIVE");
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            batch.add(e);
        }
        repository.saveAll(batch);
        LOG.info("seeded {} entitlements from fixtures", batch.size());
    }

    private JsonNode locate() {
        ObjectMapper mapper = new ObjectMapper();
        List<String> candidates = new ArrayList<>();
        if (configuredPath != null && !configuredPath.isBlank()) {
            candidates.add(configuredPath);
        }
        String env = System.getenv("MERIDIAN_FIXTURES");
        if (env != null) {
            candidates.add(env);
        }
        candidates.add("../../fixtures/meridian-fixtures.json");
        candidates.add("../../../fixtures/meridian-fixtures.json");
        for (String c : candidates) {
            Path p = Paths.get(c);
            if (Files.isRegularFile(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    LOG.info("fixtures loaded from {}", p.toAbsolutePath().normalize());
                    return mapper.readTree(in);
                } catch (IOException e) {
                    throw new IllegalStateException("unreadable fixture file " + p, e);
                }
            }
        }
        return null;
    }
}
