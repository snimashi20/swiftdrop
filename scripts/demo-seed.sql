-- SwiftDrop demo data: one user for every role and one parcel in every status.
-- Run once after the app has started (Flyway has created the tables).
-- Safe to run again: existing base and M9 demo records are preserved.
-- Password for every demo account: Demo@1234

DO $$
DECLARE
    pw           CONSTANT VARCHAR := '$2a$10$6uNOFtg8mu5YoHOR0AkU8eT.cz.9tAukIihQ/W7L276Rn6tenZ5na';
    v_admin_id     BIGINT;
    v_dispatcher_id BIGINT;
    v_courier_user_id BIGINT;
    v_merchant_user_id BIGINT;
    v_customer_id  BIGINT;
    v_customer2_id BIGINT;
    v_colombo_zone BIGINT;
    v_courier_id   BIGINT;
    v_approved_merchant_id BIGINT;
    v_pending_merchant_user_id BIGINT;
    v_merchant_parcel_id BIGINT;
    v_approved_courier_user_id BIGINT;
    v_pending_courier_user_id BIGINT;
    v_courier3_user_id BIGINT;
    v_courier4_user_id BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'customer@demo.swiftdrop.lk') THEN
        RAISE NOTICE 'Base demo data already exists; checking M9 demo data.';
    ELSE

    -- Users: one per role (plus a second customer to test "cannot open another customer's parcel")
    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    VALUES ('admin@demo.swiftdrop.lk', pw, 'Demo Admin', '0711000001', 'ADMIN')
    RETURNING id INTO v_admin_id;

    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    VALUES ('dispatcher@demo.swiftdrop.lk', pw, 'Demo Dispatcher', '0711000002', 'DISPATCHER')
    RETURNING id INTO v_dispatcher_id;

    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    VALUES ('courier@demo.swiftdrop.lk', pw, 'Demo Courier', '0711000003', 'COURIER')
    RETURNING id INTO v_courier_user_id;

    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    VALUES ('merchant@demo.swiftdrop.lk', pw, 'Demo Merchant', '0711000004', 'MERCHANT')
    RETURNING id INTO v_merchant_user_id;

    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    VALUES ('customer@demo.swiftdrop.lk', pw, 'Demo Customer', '0711000005', 'CUSTOMER')
    RETURNING id INTO v_customer_id;

    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    VALUES ('customer2@demo.swiftdrop.lk', pw, 'Second Customer', '0711000006', 'CUSTOMER')
    RETURNING id INTO v_customer2_id;

    -- Courier profile (Colombo zone) and an approved merchant
    SELECT id INTO v_colombo_zone FROM zones WHERE district = 'Colombo';

    INSERT INTO courier_profiles (user_id, vehicle_type, zone_id)
    VALUES (v_courier_user_id, 'MOTORBIKE', v_colombo_zone)
    RETURNING id INTO v_courier_id;

    INSERT INTO merchants (user_id, business_name, registration_number, contact_name,
                           contact_email, contact_phone, status, reviewed_at, reviewed_by)
    VALUES (v_merchant_user_id, 'Demo Store', 'REG-DEMO-001', 'Demo Merchant',
            'merchant@demo.swiftdrop.lk', '0711000004', 'APPROVED', now(), v_admin_id);

    -- One parcel in every status for the demo customer (CREATED and CANCELLED are not assigned)
    INSERT INTO parcels (tracking_number, customer_id, status,
                         pickup_line1, pickup_city, pickup_district,
                         dropoff_line1, dropoff_city, dropoff_district,
                         receiver_name, receiver_phone, size, weight_kg, fee,
                         pickup_zone_id, assigned_courier_id, assigned_by_user_id, assigned_at,
                         failure_reason, failure_note, created_at, updated_at)
    VALUES
    ('SDDEMO000001', v_customer_id, 'CREATED',
     '12 Galle Road', 'Colombo', 'Colombo', '5 Peradeniya Road', 'Kandy', 'Kandy',
     'Nimal Perera', '0771234501', 'M', 2.50, 700.00,
     v_colombo_zone, NULL, NULL, NULL, NULL, NULL, now() - interval '6 days', now() - interval '6 days'),
    ('SDDEMO000002', v_customer_id, 'PICKED_UP',
     '34 High Level Road', 'Nugegoda', 'Colombo', '8 Church Street', 'Galle', 'Galle',
     'Kamala Silva', '0771234502', 'S', 1.00, 650.00,
     v_colombo_zone, v_courier_id, v_dispatcher_id, now() - interval '5 days', NULL, NULL, now() - interval '5 days', now() - interval '5 days'),
    ('SDDEMO000003', v_customer_id, 'IN_TRANSIT',
     '7 Duplication Road', 'Colombo', 'Colombo', '21 Kandy Road', 'Gampaha', 'Gampaha',
     'Sunil Fernando', '0771234503', 'L', 6.00, 1100.00,
     v_colombo_zone, v_courier_id, v_dispatcher_id, now() - interval '4 days', NULL, NULL, now() - interval '4 days', now() - interval '4 days'),
    ('SDDEMO000004', v_customer_id, 'OUT_FOR_DELIVERY',
     '90 Baseline Road', 'Colombo', 'Colombo', '3 Lake Road', 'Colombo', 'Colombo',
     'Dilani Jayasinghe', '0771234504', 'M', 3.00, 600.00,
     v_colombo_zone, v_courier_id, v_dispatcher_id, now() - interval '3 days', NULL, NULL, now() - interval '3 days', now() - interval '3 days'),
    ('SDDEMO000005', v_customer_id, 'DELIVERED',
     '15 Park Street', 'Colombo', 'Colombo', '44 Temple Road', 'Jaffna', 'Jaffna',
     'Ruwan Wickramasinghe', '0771234505', 'S', 0.80, 900.00,
     v_colombo_zone, v_courier_id, v_dispatcher_id, now() - interval '2 days', NULL, NULL, now() - interval '2 days', now() - interval '1 day'),
    ('SDDEMO000006', v_customer_id, 'FAILED',
     '2 Hospital Road', 'Colombo', 'Colombo', '19 Main Street', 'Gampaha', 'Gampaha',
     'Priya Nadarajah', '0771234506', 'M', 2.00, 800.00,
     v_colombo_zone, v_courier_id, v_dispatcher_id, now() - interval '2 days', 'RECIPIENT_NOT_AVAILABLE', 'Nobody at home, called twice', now() - interval '2 days', now() - interval '1 day'),
    ('SDDEMO000007', v_customer_id, 'CANCELLED',
     '60 Marine Drive', 'Colombo', 'Colombo', '11 Fort Road', 'Galle', 'Galle',
     'Asanka Rathnayake', '0771234507', 'S', 1.20, 650.00,
     v_colombo_zone, NULL, NULL, NULL, NULL, NULL, now() - interval '1 day', now() - interval '1 day'),
    -- A parcel that belongs to the second customer
    ('SDDEMO000008', v_customer2_id, 'CREATED',
     '25 Union Place', 'Colombo', 'Colombo', '9 Hill Street', 'Kandy', 'Kandy',
     'Chamari Gunawardena', '0771234508', 'S', 1.50, 700.00,
     v_colombo_zone, NULL, NULL, NULL, NULL, NULL, now(), now());

    -- Delivery proof for the delivered parcel
    INSERT INTO delivery_proofs (parcel_id, recipient_name, note)
    SELECT id, 'Ruwan Wickramasinghe', 'Handed over at the door'
    FROM parcels WHERE tracking_number = 'SDDEMO000005';

    -- Status history so every parcel's tracking timeline has rows
    INSERT INTO status_history (parcel_id, old_status, new_status, changed_by_user_id, changed_by_role, note, changed_at)
    SELECT id, NULL, 'CREATED', parcels.customer_id, 'CUSTOMER', 'Parcel booked', created_at
    FROM parcels WHERE tracking_number LIKE 'SDDEMO%';

    INSERT INTO status_history (parcel_id, old_status, new_status, changed_by_user_id, changed_by_role, changed_at)
    SELECT p.id, s.old_status, s.new_status, v_courier_user_id, 'COURIER', p.created_at + s.after_created
    FROM parcels p
    JOIN (VALUES
        ('PICKED_UP',        'CREATED',          'PICKED_UP',        interval '1 hour'),
        ('IN_TRANSIT',       'CREATED',          'PICKED_UP',        interval '1 hour'),
        ('IN_TRANSIT',       'PICKED_UP',        'IN_TRANSIT',       interval '3 hours'),
        ('OUT_FOR_DELIVERY', 'CREATED',          'PICKED_UP',        interval '1 hour'),
        ('OUT_FOR_DELIVERY', 'PICKED_UP',        'IN_TRANSIT',       interval '3 hours'),
        ('OUT_FOR_DELIVERY', 'IN_TRANSIT',       'OUT_FOR_DELIVERY', interval '6 hours'),
        ('DELIVERED',        'CREATED',          'PICKED_UP',        interval '1 hour'),
        ('DELIVERED',        'PICKED_UP',        'IN_TRANSIT',       interval '3 hours'),
        ('DELIVERED',        'IN_TRANSIT',       'OUT_FOR_DELIVERY', interval '6 hours'),
        ('DELIVERED',        'OUT_FOR_DELIVERY', 'DELIVERED',        interval '9 hours'),
        ('FAILED',           'CREATED',          'PICKED_UP',        interval '1 hour'),
        ('FAILED',           'PICKED_UP',        'IN_TRANSIT',       interval '3 hours'),
        ('FAILED',           'IN_TRANSIT',       'OUT_FOR_DELIVERY', interval '6 hours'),
        ('FAILED',           'OUT_FOR_DELIVERY', 'FAILED',           interval '9 hours')
    ) AS s(final_status, old_status, new_status, after_created) ON s.final_status = p.status
    WHERE p.tracking_number LIKE 'SDDEMO%';

    INSERT INTO status_history (parcel_id, old_status, new_status, changed_by_user_id, changed_by_role, note, changed_at)
    SELECT id, 'CREATED', 'CANCELLED', parcels.customer_id, 'CUSTOMER', 'Cancelled by customer', created_at + interval '2 hours'
    FROM parcels WHERE tracking_number = 'SDDEMO000007';

    -- The parcel's last update time is its latest status change
    UPDATE parcels p
    SET updated_at = (SELECT max(h.changed_at) FROM status_history h WHERE h.parcel_id = p.id)
    WHERE p.tracking_number LIKE 'SDDEMO%';

    END IF;

    -- Notifications for the demo parcels.
    --
    -- The seeded parcels are inserted straight into the database, so they never travel through
    -- ParcelStatusService and no ParcelStatusChangedEvent is ever published. Without this block
    -- the customer's notification feed stays empty even though the parcels and their tracking
    -- history are there. One row per channel per status change, exactly as NotificationService
    -- writes them, with recipients masked the way Masking does it.
    --
    -- The event id is derived from the status change, so rerunning the seed is a no-op.
    -- The latest change of each parcel is left unread, so the bell and the unread filter
    -- have something to show.
    INSERT INTO notification_log (event_id, parcel_id, tracking_number, old_status, new_status,
                                  customer_id, type, recipient, status, created_at, sent_at, read_at)
    SELECT e.event_id,
           e.parcel_id,
           e.tracking_number,
           e.old_status,
           e.new_status,
           e.customer_id,
           c.type,
           CASE c.type
               WHEN 'EMAIL' THEN left(u.email, 1) || '***' || substring(u.email from position('@' in u.email))
               ELSE repeat('*', length(u.phone_number) - 3) || right(u.phone_number, 3)
           END,
           'SENT',
           e.changed_at,
           e.changed_at,
           CASE WHEN e.is_latest THEN NULL ELSE e.changed_at + interval '5 minutes' END
    FROM (
        SELECT md5(h.parcel_id::text || '|' || coalesce(h.old_status, '') || '|' || h.new_status)::uuid AS event_id,
               p.id              AS parcel_id,
               p.tracking_number AS tracking_number,
               h.old_status      AS old_status,
               h.new_status      AS new_status,
               p.customer_id     AS customer_id,
               h.changed_at      AS changed_at,
               row_number() OVER (PARTITION BY h.parcel_id ORDER BY h.changed_at DESC, h.id DESC) = 1 AS is_latest
        FROM status_history h
        JOIN parcels p ON p.id = h.parcel_id
        WHERE p.tracking_number LIKE 'SDDEMO%'
          AND p.customer_id IS NOT NULL
    ) e
    JOIN users u ON u.id = e.customer_id
    CROSS JOIN (VALUES ('EMAIL'), ('SMS')) AS c(type)
    WHERE ((c.type = 'EMAIL' AND coalesce(u.email, '') <> '')
            OR (c.type = 'SMS' AND coalesce(u.phone_number, '') <> ''))
      AND NOT EXISTS (
          SELECT 1 FROM notification_log n WHERE n.event_id = e.event_id AND n.type = c.type
      );

    -- M9 merchant fixtures. These statements are idempotent so the seed can be rerun.
    SELECT id INTO v_colombo_zone
    FROM zones
    WHERE district = 'Colombo';

    SELECT id INTO v_approved_merchant_id
    FROM merchants
    WHERE contact_email = 'merchant@demo.swiftdrop.lk';

    INSERT INTO users (email, password_hash, full_name, phone_number, role)
    SELECT 'pending-merchant@demo.swiftdrop.lk', pw, 'Pending Demo Merchant', '0711000010', 'MERCHANT'
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'pending-merchant@demo.swiftdrop.lk'
    );

    SELECT id INTO v_pending_merchant_user_id
    FROM users
    WHERE email = 'pending-merchant@demo.swiftdrop.lk';

    INSERT INTO merchants (user_id, business_name, registration_number, contact_name,
                           contact_email, contact_phone, status)
    SELECT v_pending_merchant_user_id, 'Pending Demo Store', 'REG-DEMO-PENDING',
           'Pending Demo Merchant', 'pending-merchant@demo.swiftdrop.lk', '0711000010', 'PENDING'
    WHERE NOT EXISTS (
        SELECT 1 FROM merchants
        WHERE contact_email = 'pending-merchant@demo.swiftdrop.lk'
           OR registration_number = 'REG-DEMO-PENDING'
    );

    INSERT INTO parcels (tracking_number, merchant_id, status,
                         pickup_line1, pickup_city, pickup_district,
                         dropoff_line1, dropoff_city, dropoff_district,
                         receiver_name, receiver_phone, size, weight_kg, fee,
                         pickup_zone_id, created_at, updated_at)
    SELECT 'SDM9DEMO0001', v_approved_merchant_id, 'CREATED',
           '100 Merchant Street', 'Colombo', 'Colombo',
           '200 Lake Road', 'Kandy', 'Kandy',
           'Merchant Demo Receiver', '0771234510', 'M', 2.00, 700.00,
           v_colombo_zone, now() - interval '12 hours', now() - interval '12 hours'
    WHERE NOT EXISTS (
        SELECT 1 FROM parcels WHERE tracking_number = 'SDM9DEMO0001'
    );

    INSERT INTO parcels (tracking_number, merchant_id, status,
                         pickup_line1, pickup_city, pickup_district,
                         dropoff_line1, dropoff_city, dropoff_district,
                         receiver_name, receiver_phone, size, weight_kg, fee,
                         pickup_zone_id, created_at, updated_at)
    SELECT 'SDM9DEMO0002', v_approved_merchant_id, 'DELIVERED',
           '300 Merchant Avenue', 'Colombo', 'Colombo',
           '400 Temple Road', 'Galle', 'Galle',
           'Merchant Demo Customer', '0771234511', 'S', 1.25, 650.00,
           v_colombo_zone, now() - interval '3 days', now() - interval '1 day'
    WHERE NOT EXISTS (
        SELECT 1 FROM parcels WHERE tracking_number = 'SDM9DEMO0002'
    );

    SELECT id INTO v_merchant_parcel_id
    FROM parcels
    WHERE tracking_number = 'SDM9DEMO0001';

    INSERT INTO merchant_orders (merchant_id, parcel_id, idempotency_key, external_order_id, created_at)
    SELECT v_approved_merchant_id, v_merchant_parcel_id, 'demo-m9-order-1', 'DEMO-M9-ORDER-1', now() - interval '12 hours'
    WHERE NOT EXISTS (
        SELECT 1 FROM merchant_orders
        WHERE merchant_id = v_approved_merchant_id
          AND idempotency_key = 'demo-m9-order-1'
    );

    SELECT id INTO v_merchant_parcel_id
    FROM parcels
    WHERE tracking_number = 'SDM9DEMO0002';

    INSERT INTO merchant_orders (merchant_id, parcel_id, idempotency_key, external_order_id, created_at)
    SELECT v_approved_merchant_id, v_merchant_parcel_id, 'demo-m9-order-2', 'DEMO-M9-ORDER-2', now() - interval '3 days'
    WHERE NOT EXISTS (
        SELECT 1 FROM merchant_orders
        WHERE merchant_id = v_approved_merchant_id
          AND idempotency_key = 'demo-m9-order-2'
    );

    -- Courier approval fixtures. Idempotent, so the seed can be rerun.
    SELECT id INTO v_approved_courier_user_id
    FROM users
    WHERE email = 'courier@demo.swiftdrop.lk';

    INSERT INTO courier_applications (user_id, full_name, contact_email, contact_phone,
                                      license_number, vehicle_type, status, reviewed_at)
    SELECT v_approved_courier_user_id, 'Demo Courier', 'courier@demo.swiftdrop.lk', '0711000003',
           'B-1100003', 'MOTORBIKE', 'APPROVED', now() - interval '10 days'
    WHERE v_approved_courier_user_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM courier_applications WHERE user_id = v_approved_courier_user_id
    );

    INSERT INTO users (email, password_hash, full_name, phone_number, role, status, enabled)
    SELECT 'pending-courier@demo.swiftdrop.lk', pw, 'Pending Demo Courier', '0711000011',
           'COURIER', 'PENDING_APPROVAL', FALSE
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'pending-courier@demo.swiftdrop.lk'
    );

    SELECT id INTO v_pending_courier_user_id
    FROM users
    WHERE email = 'pending-courier@demo.swiftdrop.lk';

    INSERT INTO courier_applications (user_id, full_name, contact_email, contact_phone,
                                      license_number, vehicle_type, preferred_zone, status)
    SELECT v_pending_courier_user_id, 'Pending Demo Courier', 'pending-courier@demo.swiftdrop.lk',
           '0711000011', 'B-1100011', 'THREE_WHEELER', 'Colombo', 'PENDING'
    WHERE NOT EXISTS (
        SELECT 1 FROM courier_applications WHERE user_id = v_pending_courier_user_id
    );

    -- Extra courier accounts (idempotent).
    -- These were created by hand on one machine, so they only existed in that
    -- developer's database. Seeding them here gives every teammate the same riders.
    -- Each one gets an APPROVED application (so it is not stuck in the review queue)
    -- and a courier_profile (so the dispatcher can actually assign parcels to it).
    INSERT INTO users (email, password_hash, full_name, phone_number, role, status, enabled)
    SELECT 'courier3@demo.swiftdrop.lk', pw, 'Demo Courier 3', '0711000012', 'COURIER', 'ACTIVE', TRUE
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'courier3@demo.swiftdrop.lk'
    );

    SELECT id INTO v_courier3_user_id
    FROM users
    WHERE email = 'courier3@demo.swiftdrop.lk';

    INSERT INTO courier_applications (user_id, full_name, contact_email, contact_phone,
                                      license_number, vehicle_type, preferred_zone, status, reviewed_at)
    SELECT v_courier3_user_id, 'Demo Courier 3', 'courier3@demo.swiftdrop.lk', '0711000012',
           'B-1100012', 'MOTORBIKE', 'Gampaha', 'APPROVED', now() - interval '6 days'
    WHERE v_courier3_user_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM courier_applications WHERE user_id = v_courier3_user_id
    );

    INSERT INTO courier_profiles (user_id, vehicle_type, zone_id)
    SELECT v_courier3_user_id, 'MOTORBIKE', z.id
    FROM zones z
    WHERE z.district = 'Gampaha'
      AND v_courier3_user_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM courier_profiles WHERE user_id = v_courier3_user_id
    );

    INSERT INTO users (email, password_hash, full_name, phone_number, role, status, enabled)
    SELECT 'courier4@demo.swiftdrop.lk', pw, 'Demo Courier 4', '0711000013', 'COURIER', 'ACTIVE', TRUE
    WHERE NOT EXISTS (
        SELECT 1 FROM users WHERE email = 'courier4@demo.swiftdrop.lk'
    );

    SELECT id INTO v_courier4_user_id
    FROM users
    WHERE email = 'courier4@demo.swiftdrop.lk';

    INSERT INTO courier_applications (user_id, full_name, contact_email, contact_phone,
                                      license_number, vehicle_type, preferred_zone, status, reviewed_at)
    SELECT v_courier4_user_id, 'Demo Courier 4', 'courier4@demo.swiftdrop.lk', '0711000013',
           'B-1100013', 'THREE_WHEELER', 'Kandy', 'APPROVED', now() - interval '5 days'
    WHERE v_courier4_user_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM courier_applications WHERE user_id = v_courier4_user_id
    );

    INSERT INTO courier_profiles (user_id, vehicle_type, zone_id)
    SELECT v_courier4_user_id, 'THREE_WHEELER', z.id
    FROM zones z
    WHERE z.district = 'Kandy'
      AND v_courier4_user_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM courier_profiles WHERE user_id = v_courier4_user_id
    );

    RAISE NOTICE 'Demo data created or already present. Log in with any demo account, password Demo@1234.';
END $$;
