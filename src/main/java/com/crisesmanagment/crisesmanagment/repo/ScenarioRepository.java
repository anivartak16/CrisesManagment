package com.crisesmanagment.crisesmanagment.repo;

import com.crisesmanagment.crisesmanagment.model.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {}