package com.crisesmanagment.crisesmanagment.repo;

import com.crisesmanagment.crisesmanagment.model.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {}