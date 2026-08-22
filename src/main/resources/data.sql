-- Seed suppliers (real companies = reference/master data: name, country,
-- capacity, risk classification). base_cost_per_barrel below is ONLY a
-- fallback used if EIA_API_KEY is missing/unreachable — on every normal
-- startup, MarketDataService.refreshOnStartup() immediately overwrites
-- these with a value computed live from the EIA Brent spot price, and
-- sets price_source='EIA_LIVE_BRENT' + last_price_update. Rows are left at
-- price_source=NULL here; MarketDataService fills in the correct label
-- (live or fallback) as soon as the app boots.
INSERT INTO suppliers (id, name, country, base_cost_per_barrel, capacity, risk_baseline) VALUES
                                                                                             (1, 'Saudi Aramco', 'Saudi Arabia', 75.0, 200000.0, 0.10),
                                                                                             (2, 'Abu Dhabi National Oil Company', 'UAE', 77.0, 100000.0, 0.12),
                                                                                             (3, 'Iraq Oil Company', 'Iraq', 70.0, 150000.0, 0.18),
                                                                                             (4, 'Kuwait Petroleum', 'Kuwait', 72.0, 90000.0, 0.14),
                                                                                             (5, 'Nigerian National Petroleum', 'Nigeria', 68.0, 80000.0, 0.20),
                                                                                             (6, 'Indian Oil Corporation', 'India', 80.0, 120000.0, 0.08)
    ON CONFLICT (id) DO NOTHING;

-- Seed routes (linking to origin suppliers by id).
-- seed_risk_score / seed_shipping_cost carry the same numbers as
-- base_risk_score / base_shipping_cost here on first insert.
INSERT INTO routes (id, name, origin_supplier_id, distance_km, base_shipping_cost, base_risk_score, origin_lat, origin_lng, seed_risk_score, seed_shipping_cost) VALUES
                                                                                                                        (1, 'Strait of Hormuz', 1, 3500.0, 2.50, 0.15, 26.56, 56.25, 0.15, 2.50),
                                                                                                                        (2, 'Red Sea / Suez Corridor', 2, 6500.0, 4.00, 0.12, 29.50, 32.55, 0.12, 4.00),
                                                                                                                        (3, 'Arabian Sea Coastal Route', 6, 500.0, 0.80, 0.05, 18.96, 72.82, 0.05, 0.80),
                                                                                                                        (4, 'Southern Africa (Cape of Good Hope)', 5, 15000.0, 9.50, 0.22, -34.36, 18.47, 0.22, 9.50),
                                                                                                                        (5, 'Kuwait-to-India Corridor', 4, 2800.0, 2.00, 0.13, 29.37, 47.98, 0.13, 2.00),
                                                                                                                        (6, 'Iraq Basra Gulf Route', 3, 3200.0, 2.30, 0.17, 30.50, 47.81, 0.17, 2.30)
    ON CONFLICT (id) DO NOTHING;

-- Self-healing reset: base_risk_score/base_shipping_cost are LIVE columns
-- that RouteRiskService overwrites as disruption events fire, so they are
-- expected to drift away from these numbers during a session — that's by
-- design ("not persisted" is the intent). seed_risk_score/seed_shipping_cost
-- must NEVER drift, so every boot forces them back to the true reference
-- values regardless of what a previous run left behind. This also repairs
-- any route that was created before these columns existed (they'd otherwise
-- be NULL, which previously made every session compound risk from whatever
-- was last saved instead of from the real baseline).
UPDATE routes SET seed_risk_score = 0.15, seed_shipping_cost = 2.50 WHERE id = 1;
UPDATE routes SET seed_risk_score = 0.12, seed_shipping_cost = 4.00 WHERE id = 2;
UPDATE routes SET seed_risk_score = 0.05, seed_shipping_cost = 0.80 WHERE id = 3;
UPDATE routes SET seed_risk_score = 0.22, seed_shipping_cost = 9.50 WHERE id = 4;
UPDATE routes SET seed_risk_score = 0.13, seed_shipping_cost = 2.00 WHERE id = 5;
UPDATE routes SET seed_risk_score = 0.17, seed_shipping_cost = 2.30 WHERE id = 6;

UPDATE routes SET origin_lat = 26.56, origin_lng = 56.25 WHERE id = 1 AND (origin_lat IS NULL OR origin_lng IS NULL);
UPDATE routes SET origin_lat = 29.50, origin_lng = 32.55 WHERE id = 2 AND (origin_lat IS NULL OR origin_lng IS NULL);
UPDATE routes SET origin_lat = 18.96, origin_lng = 72.82 WHERE id = 3 AND (origin_lat IS NULL OR origin_lng IS NULL);
UPDATE routes SET origin_lat = -34.36, origin_lng = 18.47 WHERE id = 4 AND (origin_lat IS NULL OR origin_lng IS NULL);
UPDATE routes SET origin_lat = 29.37, origin_lng = 47.98 WHERE id = 5 AND (origin_lat IS NULL OR origin_lng IS NULL);
UPDATE routes SET origin_lat = 30.50, origin_lng = 47.81 WHERE id = 6 AND (origin_lat IS NULL OR origin_lng IS NULL);

-- Reset the identity sequences so future inserts (new suppliers/routes via the app)
-- don't collide with these manually-assigned fixed IDs.
SELECT setval(pg_get_serial_sequence('suppliers', 'id'), (SELECT MAX(id) FROM suppliers));
SELECT setval(pg_get_serial_sequence('routes', 'id'), (SELECT MAX(id) FROM routes));

-- Note: IDs are fixed here to ensure foreign keys resolve predictably during startup.
