package com.meridian.platform.piivault.token;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenMapRepository extends JpaRepository<TokenMap, Long> {

    Optional<TokenMap> findByToken(String token);

    Optional<TokenMap> findFirstByValueHashAndPiiType(String valueHash, String piiType);
}
