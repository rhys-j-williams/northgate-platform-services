package com.northgate.platform.beacon.sequence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSequenceRepository extends JpaRepository<CustomerSequence, String> {
}
