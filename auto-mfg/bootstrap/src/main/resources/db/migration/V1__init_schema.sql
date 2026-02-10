-- Order Context Schema
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL UNIQUE,
    dealer_id VARCHAR(50) NOT NULL,
    vehicle_model_code VARCHAR(50) NOT NULL,
    color_code VARCHAR(30) NOT NULL,
    option_package_codes TEXT,
    status VARCHAR(30) NOT NULL,
    estimated_delivery_date DATE NOT NULL,
    price_quote DECIMAL(12,2) NOT NULL,
    change_count INT NOT NULL DEFAULT 0,
    order_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Manufacturing Context Schema
CREATE TABLE IF NOT EXISTS production_orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(25) NOT NULL UNIQUE,
    source_order_id UUID NOT NULL UNIQUE,
    vin VARCHAR(17) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    current_station_sequence INT,
    scheduled_start_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS bom_snapshots (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL REFERENCES production_orders(id),
    snapshot_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS bom_line_items (
    id UUID PRIMARY KEY,
    bom_snapshot_id UUID NOT NULL REFERENCES bom_snapshots(id),
    part_number VARCHAR(50) NOT NULL,
    part_description VARCHAR(200) NOT NULL,
    quantity_required INT NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL,
    is_available BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS assembly_processes (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL UNIQUE REFERENCES production_orders(id),
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS assembly_steps (
    id UUID PRIMARY KEY,
    assembly_process_id UUID NOT NULL REFERENCES assembly_processes(id),
    work_station_code VARCHAR(20) NOT NULL,
    work_station_sequence INT NOT NULL,
    task_description VARCHAR(500) NOT NULL,
    standard_time_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    operator_id VARCHAR(50),
    material_batch_id VARCHAR(100),
    actual_time_minutes INT,
    completed_at TIMESTAMP,
    corrects_record_id UUID
);

-- Quality Inspection
CREATE TABLE IF NOT EXISTS quality_inspections (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL REFERENCES production_orders(id),
    vin VARCHAR(17) NOT NULL,
    result VARCHAR(20),
    inspector_id VARCHAR(50) NOT NULL,
    reviewer_id VARCHAR(50),
    inspected_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    corrects_record_id UUID
);

CREATE TABLE IF NOT EXISTS inspection_items (
    id UUID PRIMARY KEY,
    inspection_id UUID NOT NULL REFERENCES quality_inspections(id),
    description VARCHAR(500) NOT NULL,
    is_safety_related BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT
);

CREATE TABLE IF NOT EXISTS rework_orders (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL REFERENCES production_orders(id),
    inspection_id UUID NOT NULL REFERENCES quality_inspections(id),
    status VARCHAR(20) NOT NULL,
    failed_items TEXT,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

-- Vehicle Configuration Context
CREATE TABLE IF NOT EXISTS vehicle_configurations (
    id UUID PRIMARY KEY,
    model_code VARCHAR(50) NOT NULL UNIQUE,
    model_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS color_options (
    id UUID PRIMARY KEY,
    vehicle_config_id UUID NOT NULL REFERENCES vehicle_configurations(id),
    color_code VARCHAR(30) NOT NULL,
    color_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS option_packages (
    id UUID PRIMARY KEY,
    vehicle_config_id UUID NOT NULL REFERENCES vehicle_configurations(id),
    package_code VARCHAR(50) NOT NULL,
    package_name VARCHAR(100) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS compatibility_rules (
    id UUID PRIMARY KEY,
    vehicle_config_id UUID NOT NULL REFERENCES vehicle_configurations(id),
    option_code_a VARCHAR(50) NOT NULL,
    option_code_b VARCHAR(50) NOT NULL,
    rule_type VARCHAR(20) NOT NULL,
    description VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS inspection_checklists (
    id UUID PRIMARY KEY,
    model_code VARCHAR(50) NOT NULL,
    item_description VARCHAR(500) NOT NULL,
    is_safety_related BOOLEAN NOT NULL,
    display_order INT NOT NULL
);

-- Infrastructure Tables
CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    consumer_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS domain_event_outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);
