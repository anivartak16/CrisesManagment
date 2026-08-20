-- Seed suppliers (realistic major crude suppliers to India)
INSERT INTO suppliers (id, name, country, base_cost_per_barrel, capacity, risk_baseline) VALUES
                                                                                             (1, 'Saudi Aramco', 'Saudi Arabia', 75.0, 200000.0, 0.10),
                                                                                             (2, 'Abu Dhabi National Oil Company', 'UAE', 77.0, 100000.0, 0.12),
                                                                                             (3, 'Iraq Oil Company', 'Iraq', 70.0, 150000.0, 0.18),
                                                                                             (4, 'Kuwait Petroleum', 'Kuwait', 72.0, 90000.0, 0.14),
                                                                                             (5, 'Nigerian National Petroleum', 'Nigeria', 68.0, 80000.0, 0.20),
                                                                                             (6, 'Indian Oil Corporation', 'India', 80.0, 120000.0, 0.08)
    ON CONFLICT (id) DO NOTHING;

-- Seed routes (linking to origin suppliers by id)
INSERT INTO routes (id, name, origin_supplier_id, distance_km, base_shipping_cost, base_risk_score) VALUES
                                                                                                        (1, 'Strait of Hormuz', 1, 3500.0, 2.50, 0.15),
                                                                                                        (2, 'Red Sea / Suez Corridor', 2, 6500.0, 4.00, 0.12),
                                                                                                        (3, 'Arabian Sea Coastal Route', 6, 500.0, 0.80, 0.05),
                                                                                                        (4, 'Southern Africa (Cape of Good Hope)', 5, 15000.0, 9.50, 0.22),
                                                                                                        (5, 'Kuwait-to-India Corridor', 4, 2800.0, 2.00, 0.13),
                                                                                                        (6, 'Iraq Basra Gulf Route', 3, 3200.0, 2.30, 0.17)
    ON CONFLICT (id) DO NOTHING;

-- Reset the identity sequences so future inserts (new suppliers/routes via the app)
-- don't collide with these manually-assigned fixed IDs.
SELECT setval(pg_get_serial_sequence('suppliers', 'id'), (SELECT MAX(id) FROM suppliers));
SELECT setval(pg_get_serial_sequence('routes', 'id'), (SELECT MAX(id) FROM routes));

-- Note: IDs are fixed here to ensure foreign keys resolve predictably during startup.
