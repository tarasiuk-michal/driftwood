package com.driftwood.repository;

import com.driftwood.domain.DeadLetterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetterEntry, UUID> {
}
