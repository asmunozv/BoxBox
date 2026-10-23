-- BoxBox - instalacion administrativa en una base NUEVA y vacia.
-- No ejecutar este archivo sobre la base ya restaurada.
-- Ejecutar por etapas y detenerse ante cualquier error. No usar --force.

CREATE DATABASE boxbox_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE boxbox_db;

-- =========================
-- Core master tables
-- =========================

CREATE TABLE constructors (
  id_constructor INT UNSIGNED NOT NULL AUTO_INCREMENT,
  ergast_constructor_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  country VARCHAR(80) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_constructor),
  UNIQUE KEY uq_constructors_ergast_id (ergast_constructor_id),
  UNIQUE KEY uq_constructors_name (name)
) ENGINE=InnoDB;

CREATE TABLE drivers (
  id_driver INT UNSIGNED NOT NULL AUTO_INCREMENT,
  ergast_driver_id VARCHAR(64) NOT NULL,
  permanent_number SMALLINT UNSIGNED NULL,
  code VARCHAR(8) NULL,
  given_name VARCHAR(80) NOT NULL,
  family_name VARCHAR(80) NOT NULL,
  date_of_birth DATE NULL,
  nationality VARCHAR(80) NULL,
  audio_file_path VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_driver),
  UNIQUE KEY uq_drivers_ergast_id (ergast_driver_id),
  UNIQUE KEY uq_drivers_code (code),
  KEY idx_drivers_family_name (family_name)
) ENGINE=InnoDB;

CREATE TABLE circuits (
  id_circuit INT UNSIGNED NOT NULL AUTO_INCREMENT,
  ergast_circuit_id VARCHAR(64) NOT NULL,
  name VARCHAR(150) NOT NULL,
  country VARCHAR(80) NULL,
  locality VARCHAR(100) NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_circuit),
  UNIQUE KEY uq_circuits_ergast_id (ergast_circuit_id),
  KEY idx_circuits_name (name)
) ENGINE=InnoDB;

-- =========================
-- Relationship tables
-- =========================

-- Supports "Assign driver to constructor (by season)" requirement.
CREATE TABLE driver_constructor_seasons (
  id_driver_constructor_season INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_driver INT UNSIGNED NOT NULL,
  id_constructor INT UNSIGNED NOT NULL,
  season YEAR NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_driver_constructor_season),
  UNIQUE KEY uq_driver_constructor_season (id_driver, season),
  KEY idx_dcs_constructor_season (id_constructor, season),
  CONSTRAINT fk_dcs_driver
    FOREIGN KEY (id_driver)
    REFERENCES drivers (id_driver)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_dcs_constructor
    FOREIGN KEY (id_constructor)
    REFERENCES constructors (id_constructor)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT chk_dcs_season CHECK (season >= 1950)
) ENGINE=InnoDB;

CREATE TABLE races (
  id_race INT UNSIGNED NOT NULL AUTO_INCREMENT,
  season YEAR NOT NULL,
  round_number TINYINT UNSIGNED NOT NULL,
  name VARCHAR(150) NOT NULL,
  race_date DATE NOT NULL,
  id_circuit INT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_race),
  UNIQUE KEY uq_race_season_round (season, round_number),
  KEY idx_races_circuit (id_circuit),
  CONSTRAINT fk_races_circuit
    FOREIGN KEY (id_circuit)
    REFERENCES circuits (id_circuit)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT chk_race_round CHECK (round_number BETWEEN 1 AND 30),
  CONSTRAINT chk_race_season CHECK (season >= 1950)
) ENGINE=InnoDB;

CREATE TABLE results (
  id_result INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_race INT UNSIGNED NOT NULL,
  id_driver INT UNSIGNED NOT NULL,
  id_constructor INT UNSIGNED NOT NULL,
  session_type VARCHAR(10) NOT NULL DEFAULT 'RACE',
  finishing_position TINYINT UNSIGNED NOT NULL,
  points DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_result),
  UNIQUE KEY uq_results_race_driver_session (id_race, id_driver, session_type),
  UNIQUE KEY uq_results_race_session_position (id_race, session_type, finishing_position),
  KEY idx_results_constructor (id_constructor),
  CONSTRAINT fk_results_race
    FOREIGN KEY (id_race)
    REFERENCES races (id_race)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_results_driver
    FOREIGN KEY (id_driver)
    REFERENCES drivers (id_driver)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT fk_results_constructor
    FOREIGN KEY (id_constructor)
    REFERENCES constructors (id_constructor)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT chk_results_position CHECK (finishing_position BETWEEN 1 AND 20),
  CONSTRAINT chk_results_points CHECK (points >= 0)
) ENGINE=InnoDB;

-- =========================
-- User accounts and prediction bingo (prepared here, not by JavaFX startup)
-- =========================

CREATE TABLE app_users (
  id_user INT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_user),
  UNIQUE KEY uq_app_users_username (username)
) ENGINE=InnoDB;

CREATE TABLE user_bingo_predictions (
  id_user INT UNSIGNED NOT NULL,
  slot_index TINYINT UNSIGNED NOT NULL,
  prediction_text VARCHAR(255) NULL,
  is_completed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_user, slot_index),
  CONSTRAINT fk_bingo_user FOREIGN KEY (id_user)
    REFERENCES app_users (id_user) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- Useful standings views: permissions belong to the querying account.
CREATE SQL SECURITY INVOKER VIEW vw_driver_standings AS
SELECT
  d.id_driver,
  d.given_name,
  d.family_name,
  d.nationality,
  COALESCE(SUM(r.points), 0) AS total_points,
  COALESCE(SUM(CASE WHEN r.finishing_position = 1 THEN 1 ELSE 0 END), 0) AS wins,
  COALESCE(SUM(CASE WHEN r.finishing_position <= 3 THEN 1 ELSE 0 END), 0) AS podiums
FROM drivers d
LEFT JOIN results r ON r.id_driver = d.id_driver
GROUP BY d.id_driver, d.given_name, d.family_name, d.nationality;

CREATE SQL SECURITY INVOKER VIEW vw_constructor_standings AS
SELECT
  c.id_constructor,
  c.name,
  c.country,
  COALESCE(SUM(r.points), 0) AS total_points
FROM constructors c
LEFT JOIN results r ON r.id_constructor = c.id_constructor
GROUP BY c.id_constructor, c.name, c.country;
