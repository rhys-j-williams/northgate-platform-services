package com.meridian.platform.piivault.access;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PiiAccessRepository extends JpaRepository<PiiAccess, Long> {

    List<PiiAccess> findTop200ByTokenOrderByAccessedAtDesc(String token);

    List<PiiAccess> findTop200ByPrincipalOrderByAccessedAtDesc(String principal);
}
