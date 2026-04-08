package com.tccs.repository;

import com.tccs.model.entity.Junction;
import com.tccs.model.enums.ControlMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JunctionRepository extends JpaRepository<Junction, Long> {

    Optional<Junction> findByJunctionCode(String junctionCode);

    List<Junction> findByActiveTrue();

    List<Junction> findByControlMode(ControlMode controlMode);

    boolean existsByJunctionCode(String junctionCode);
}