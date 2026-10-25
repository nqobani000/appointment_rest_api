BEGIN;

-- Disclaimer, to save on time - I seeded this using AI (deepseek) -
-- nothing is guaranteed to be correct, but it should be a good starting point for testing and development.

-- ===========================================================
-- 0. Pre-flight
--    btree_gist is required by both EXCLUDE USING gist constraints.
--    If your DB user can't CREATE EXTENSION, run this line manually
--    once as superuser and remove it here.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

DO
$$
    BEGIN
        IF (SELECT count(*) FROM service_categories) <> 9 THEN
            RAISE EXCEPTION 'Expected 9 service_categories, found %',
                    (SELECT count(*) FROM service_categories);
        END IF;
        IF (SELECT count(*) FROM branches) <> 30 THEN
            RAISE EXCEPTION 'Expected 30 branches, found %',
                    (SELECT count(*) FROM branches);
        END IF;
        IF (SELECT count(*) FROM services WHERE is_active) = 0 THEN
            RAISE EXCEPTION 'No active services found - nothing to seed';
        END IF;
    END
$$;

-- ============================================================
-- 1. Branch operating hours
-- ============================================================
INSERT INTO branch_operating_hours (branch_id, day_of_week, opens_at, closes_at)
SELECT b.id,
       d.day,
       CASE WHEN d.day = 6 THEN '08:30'::time ELSE '09:00'::time END,
       CASE WHEN d.day = 6 THEN '13:00'::time ELSE '17:00'::time END
FROM branches b
         CROSS JOIN (VALUES (1), (2), (3), (4), (5), (6)) d(day)
ON CONFLICT (branch_id, day_of_week, opens_at, closes_at) DO NOTHING;

-- ============================================================
-- 2. Branch services: full cross product with active services
-- ============================================================
INSERT INTO branch_services (branch_id, service_id)
SELECT b.id, s.id
FROM branches b
         CROSS JOIN services s
WHERE s.is_active
ON CONFLICT (branch_id, service_id) DO NOTHING;

-- ============================================================
-- 3. Consultants — deterministic names, statuses, shifts
-- ============================================================
CREATE TEMP TABLE seed_consultants ON COMMIT DROP AS
WITH branch_ranks AS (SELECT id                                  AS branch_id,
                             row_number() OVER (ORDER BY id) - 1 AS branch_rank
                      FROM branches),
     slots AS (SELECT br.branch_id,
                      br.branch_rank,
                      sl.slot_rank,
                      br.branch_rank * 6 + sl.slot_rank AS global_slot
               FROM branch_ranks br
                        CROSS JOIN (SELECT generate_series(0, 5) AS slot_rank) sl),
     name_pools AS (SELECT ARRAY [
                               'Sipho','Thabo','Naledi','Lerato','Bongani','Tshepo','Karabo','Refilwe','Nomsa',
                               'Priya','Rajesh','Sanjay','Fatima','Ayesha','Mohammed','Pieter','Riaan','Elsa',
                               'Jaco','Heinrich','Anna','Maria','Ruan','Lindiwe','Zanele','Kabelo','Zinhle',
                               'Thandi','Andre','Chandré'
                               ] AS first_names,
                           ARRAY [
                               'Nkosi','Dlamini','Mokoena','Sithole','Mthembu','Ndlovu','Botha','Van der Merwe',
                               'Pretorius','Steyn','Smith','Naidoo','Pillai','Patel','Khan','Adams','Hendricks',
                               'Jacobs','Moloi','Khumalo','Maseko','Du Toit','Coetzee','Erasmus','Fourie'
                               ] AS surnames),
     generated AS (SELECT s.branch_id,
                          s.branch_rank,
                          s.slot_rank,
                          s.global_slot,
                          p.first_names[1 + (abs(('x' || substr(md5('fn_' || s.global_slot::text), 1, 8))::bit(32)::int)
                              % array_length(p.first_names, 1))]                                         AS first_name,
                          p.surnames[1 + (abs(('x' || substr(md5('sn_' || s.global_slot::text), 1, 8))::bit(32)::int)
                              % array_length(p.surnames, 1))]                                            AS surname,
                          abs(('x' || substr(md5('st_' || s.global_slot::text), 1, 8))::bit(32)::int)    AS status_hash,
                          abs(('x' || substr(md5('shift_' || s.global_slot::text), 1, 8))::bit(32)::int) AS shift_hash,
                          abs(('x' || substr(md5('sat_' || s.global_slot::text), 1, 8))::bit(32)::int)   AS sat_hash,
                          abs(('x' || substr(md5('multi_' || s.global_slot::text), 1, 8))::bit(32)::int) AS multi_hash
                   FROM slots s
                            CROSS JOIN name_pools p),
     classified AS (SELECT *,
                           CASE
                               WHEN slot_rank = 5 AND status_hash % 100 < 10 THEN 'DEACTIVATED'
                               WHEN slot_rank = 5 AND status_hash % 100 < 20 THEN 'AWAY'
                               WHEN status_hash % 100 < 7 THEN 'AWAY'
                               ELSE 'ACTIVE'
                               END AS status,
                           CASE
                               WHEN shift_hash % 100 < 60 THEN 'FULL'
                               WHEN shift_hash % 100 < 80 THEN 'AM'
                               ELSE 'PM'
                               END AS shift_type
                    FROM generated),
     with_email AS (SELECT *,
                           lower(regexp_replace(first_name || '.' || surname, '[^a-zA-Z.]', '', 'g')
                               || '@capitecbank.co.za') AS email_base
                    FROM classified),
     numbered AS (SELECT *,
                         row_number() OVER (PARTITION BY email_base ORDER BY global_slot) AS dup_rank
                  FROM with_email)
SELECT branch_id,
       branch_rank,
       slot_rank,
       global_slot,
       first_name,
       surname,
       CASE
           WHEN dup_rank = 1 THEN email_base
           ELSE replace(email_base, '@', dup_rank::text || '@')
           END                 AS email,
       status,
       shift_type,
       (sat_hash % 100 < 40)   AS works_saturday,
       (multi_hash % 100 < 20) AS is_multi_branch
FROM numbered;

INSERT INTO consultants (name, surname, email, status)
SELECT first_name, surname, email, status
FROM seed_consultants
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- 4a. consultant_branches — primary
-- ============================================================
INSERT INTO consultant_branches (consultant_id, branch_id)
SELECT c.id, sc.branch_id
FROM seed_consultants sc
         JOIN consultants c ON c.email = sc.email
WHERE sc.status <> 'DEACTIVATED'
ON CONFLICT (consultant_id, branch_id) DO NOTHING;

-- ============================================================
-- 4b. consultant_branches — secondary (~20%, deterministic pick)
-- ============================================================
INSERT INTO consultant_branches (consultant_id, branch_id)
SELECT c.id,
       (SELECT b2.id
        FROM branches b2
        WHERE b2.id <> sc.branch_id
        ORDER BY abs(('x' || substr(md5(sc.email || b2.id::text), 1, 8))::bit(32)::int)
        LIMIT 1)
FROM seed_consultants sc
         JOIN consultants c ON c.email = sc.email
WHERE sc.status = 'ACTIVE'
  AND sc.is_multi_branch
ON CONFLICT (consultant_id, branch_id) DO NOTHING;

-- ============================================================
-- 5. consultant_services — 2 categories each, by sort_order stride 2
--    rank 0: {1,2}  rank 1: {3,4}  rank 2: {5,6}
--    rank 3: {7,8}  rank 4: {9,1}  rank 5: {2,3}
--    → all 9 categories covered; 1, 2, 3 doubled (highest-volume)
-- ============================================================
INSERT INTO consultant_services (consultant_id, service_id)
WITH consultant_cats AS (SELECT sc.email,
                                cat_a.code AS cat_a,
                                cat_b.code AS cat_b
                         FROM seed_consultants sc
                                  JOIN service_categories cat_a ON cat_a.sort_order = (sc.slot_rank * 2) % 9 + 1
                                  JOIN service_categories cat_b ON cat_b.sort_order = (sc.slot_rank * 2 + 1) % 9 + 1
                         WHERE sc.status <> 'DEACTIVATED')
SELECT c.id, s.id
FROM consultant_cats cc
         JOIN consultants c ON c.email = cc.email
         JOIN services s ON s.category IN (cc.cat_a, cc.cat_b) AND s.is_active
ON CONFLICT (consultant_id, service_id) DO NOTHING;

-- ============================================================
-- 6a. Availability — Mon-Wed at primary (all ACTIVE consultants)
-- ============================================================
INSERT INTO consultant_availability (consultant_id, branch_id, day_of_week, start_time, end_time)
SELECT c.id,
       sc.branch_id,
       d.day,
       CASE sc.shift_type WHEN 'FULL' THEN '09:00'::time WHEN 'AM' THEN '09:00'::time ELSE '13:00'::time END,
       CASE sc.shift_type WHEN 'FULL' THEN '17:00'::time WHEN 'AM' THEN '13:00'::time ELSE '17:00'::time END
FROM seed_consultants sc
         JOIN consultants c ON c.email = sc.email
         CROSS JOIN (VALUES (1), (2), (3)) d(day)
WHERE sc.status = 'ACTIVE'
ON CONFLICT (consultant_id, branch_id, day_of_week, start_time, end_time) DO NOTHING;

-- 6b. Availability — Thu-Fri at primary for single-branch consultants
INSERT INTO consultant_availability (consultant_id, branch_id, day_of_week, start_time, end_time)
SELECT c.id,
       sc.branch_id,
       d.day,
       CASE sc.shift_type WHEN 'FULL' THEN '09:00'::time WHEN 'AM' THEN '09:00'::time ELSE '13:00'::time END,
       CASE sc.shift_type WHEN 'FULL' THEN '17:00'::time WHEN 'AM' THEN '13:00'::time ELSE '17:00'::time END
FROM seed_consultants sc
         JOIN consultants c ON c.email = sc.email
         CROSS JOIN (VALUES (4), (5)) d(day)
WHERE sc.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1
                  FROM consultant_branches cb
                  WHERE cb.consultant_id = c.id
                    AND cb.branch_id <> sc.branch_id)
ON CONFLICT (consultant_id, branch_id, day_of_week, start_time, end_time) DO NOTHING;

-- 6c. Availability — Thu-Fri at secondary for multi-branch consultants
INSERT INTO consultant_availability (consultant_id, branch_id, day_of_week, start_time, end_time)
SELECT c.id,
       cb.branch_id,
       d.day,
       CASE sc.shift_type WHEN 'FULL' THEN '09:00'::time WHEN 'AM' THEN '09:00'::time ELSE '13:00'::time END,
       CASE sc.shift_type WHEN 'FULL' THEN '17:00'::time WHEN 'AM' THEN '13:00'::time ELSE '17:00'::time END
FROM seed_consultants sc
         JOIN consultants c ON c.email = sc.email
         JOIN consultant_branches cb ON cb.consultant_id = c.id AND cb.branch_id <> sc.branch_id
         CROSS JOIN (VALUES (4), (5)) d(day)
WHERE sc.status = 'ACTIVE'
ON CONFLICT (consultant_id, branch_id, day_of_week, start_time, end_time) DO NOTHING;

-- 6d. Availability — Saturday at primary (FULL/AM, 40% attendance)
INSERT INTO consultant_availability (consultant_id, branch_id, day_of_week, start_time, end_time)
SELECT c.id, sc.branch_id, 6, '08:30'::time, '13:00'::time
FROM seed_consultants sc
         JOIN consultants c ON c.email = sc.email
WHERE sc.status = 'ACTIVE'
  AND sc.shift_type IN ('FULL', 'AM')
  AND sc.works_saturday
ON CONFLICT (consultant_id, branch_id, day_of_week, start_time, end_time) DO NOTHING;

-- ============================================================
-- 7. Verification (RAISE EXCEPTION on failure → full rollback)
-- ============================================================

-- 7a. Every (branch, active category) has ≥1 non-deactivated consultant
DO
$$
    DECLARE
        missing INT;
    BEGIN
        SELECT count(*)
        INTO missing
        FROM branches b
                 CROSS JOIN (SELECT DISTINCT category FROM services WHERE is_active) cat
        WHERE NOT EXISTS (SELECT 1
                          FROM consultant_branches cb
                                   JOIN consultants c ON c.id = cb.consultant_id AND c.status <> 'DEACTIVATED'
                                   JOIN consultant_services cs ON cs.consultant_id = c.id
                                   JOIN services s ON s.id = cs.service_id
                          WHERE cb.branch_id = b.id
                            AND s.category = cat.category);
        IF missing > 0 THEN
            RAISE EXCEPTION 'Category coverage gap: % (branch, category) pairs unstaffed', missing;
        END IF;
    END
$$;

-- 7b. Availability windows within branch operating hours
DO
$$
    DECLARE
        bad INT;
    BEGIN
        SELECT count(*)
        INTO bad
        FROM consultant_availability ca
                 JOIN branch_operating_hours boh
                      ON boh.branch_id = ca.branch_id AND boh.day_of_week = ca.day_of_week
        WHERE ca.start_time < boh.opens_at
           OR ca.end_time > boh.closes_at;
        IF bad > 0 THEN
            RAISE EXCEPTION 'Availability out of branch hours: % rows', bad;
        END IF;
    END
$$;

-- 7c. Every ACTIVE consultant with a branch has some availability
DO
$$
    DECLARE
        bad INT;
    BEGIN
        SELECT count(*)
        INTO bad
        FROM consultants c
                 JOIN consultant_branches cb ON cb.consultant_id = c.id
        WHERE c.status = 'ACTIVE'
          AND NOT EXISTS (SELECT 1 FROM consultant_availability ca WHERE ca.consultant_id = c.id);
        IF bad > 0 THEN
            RAISE EXCEPTION 'ACTIVE consultants with no availability: %', bad;
        END IF;
    END
$$;

-- 7d. Every branch has ≥1 ACTIVE consultant each weekday
DO
$$
    DECLARE
        bad INT;
    BEGIN
        SELECT count(*)
        INTO bad
        FROM branches b
                 CROSS JOIN (VALUES (1), (2), (3), (4), (5)) d(day)
        WHERE NOT EXISTS (SELECT 1
                          FROM consultant_availability ca
                                   JOIN consultants c ON c.id = ca.consultant_id AND c.status = 'ACTIVE'
                          WHERE ca.branch_id = b.id
                            AND ca.day_of_week = d.day);
        IF bad > 0 THEN
            RAISE EXCEPTION 'Branches with no ACTIVE consultant on weekday: % (branch, day) gaps', bad;
        END IF;
    END
$$;

-- ============================================================
-- 8. Summary (visible in psql output)
-- ============================================================
SELECT 'consultants' AS table_name, count(*) AS rows
FROM consultants
UNION ALL
SELECT 'consultant_branches', count(*)
FROM consultant_branches
UNION ALL
SELECT 'consultant_services', count(*)
FROM consultant_services
UNION ALL
SELECT 'consultant_availability', count(*)
FROM consultant_availability
UNION ALL
SELECT 'branch_operating_hours', count(*)
FROM branch_operating_hours
UNION ALL
SELECT 'branch_services', count(*)
FROM branch_services;

COMMIT;