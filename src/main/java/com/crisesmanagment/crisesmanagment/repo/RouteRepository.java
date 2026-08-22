package com.crisesmanagment.crisesmanagment.repo;

import com.crisesmanagment.crisesmanagment.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    // NEW: needed to derive a supplier's live risk from the route(s) that
    // originate from them. Spring Data generates the query from the method
    // name — no @Query needed since Route.originSupplier is a mapped FK.
    List<Route> findByOriginSupplierId(Long supplierId);
}