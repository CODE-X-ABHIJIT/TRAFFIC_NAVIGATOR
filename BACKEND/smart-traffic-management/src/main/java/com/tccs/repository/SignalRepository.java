package com.tccs.repository;

import com.tccs.model.entity.Signal;
import com.tccs.model.enums.SignalState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SignalRepository extends JpaRepository<Signal, Long> {

    List<Signal> findByJunctionId(Long junctionId);

    @Query("SELECT s FROM Signal s WHERE s.junction.junctionCode = :code")
    List<Signal> findByJunctionCode(@Param("code") String junctionCode);

    @Query("SELECT s FROM Signal s WHERE s.junction.junctionCode = :code " +
           "AND s.direction = :direction")
    Optional<Signal> findByJunctionCodeAndDirection(
            @Param("code") String junctionCode,
            @Param("direction") String direction);

    @Query("SELECT s FROM Signal s WHERE s.junction.junctionCode = :code " +
           "AND s.currentState = :state")
    List<Signal> findByJunctionCodeAndState(
            @Param("code") String junctionCode,
            @Param("state") SignalState state);
}