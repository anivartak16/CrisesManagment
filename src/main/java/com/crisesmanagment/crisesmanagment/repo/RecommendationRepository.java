package com.crisesmanagment.crisesmanagment.repo;

import com.crisesmanagment.crisesmanagment.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {}