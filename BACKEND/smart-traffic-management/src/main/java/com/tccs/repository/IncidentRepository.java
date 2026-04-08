package com.tccs.repository;

import com.tccs.model.entity.Incident;
import com.tccs.model.enums.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByActiveTrue();

    List<Incident> findByJunctionCodeAndActiveTrue(String junctionCode);

    List<Incident> findByIncidentTypeAndActiveTrue(IncidentType type);

    long countByActiveTrue();
}