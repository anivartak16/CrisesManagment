package com.crisesmanagment.crisesmanagment.repo;

import com.crisesmanagment.crisesmanagment.model.ProcurementAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcurementAllocationRepository extends JpaRepository<ProcurementAllocation, Long> {
}
