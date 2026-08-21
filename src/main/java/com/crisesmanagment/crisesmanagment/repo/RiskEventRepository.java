package com.crisesmanagment.crisesmanagment.repo;

import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
    List<RiskEvent> findByRouteId(Long routeId);
}