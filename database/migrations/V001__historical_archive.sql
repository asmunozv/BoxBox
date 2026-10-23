-- Migración aditiva: solo objetos nuevos dentro de boxbox_db. No cambia datos heredados.
CREATE TABLE boxbox_db.bb_races (
  race_key VARCHAR(64) NOT NULL,
  season SMALLINT UNSIGNED NOT NULL,
  round_number SMALLINT UNSIGNED NOT NULL,
  name VARCHAR(180) NOT NULL,
  race_date DATE NOT NULL,
  circuit_id VARCHAR(80) NOT NULL,
  circuit_name VARCHAR(180) NOT NULL,
  country VARCHAR(100) NULL,
  locality VARCHAR(120) NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  PRIMARY KEY (race_key),
  UNIQUE KEY uq_bb_season_round (season, round_number),
  CONSTRAINT ck_bb_season CHECK (season >= 1950),
  CONSTRAINT ck_bb_round CHECK (round_number > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE boxbox_db.bb_documents (
  race_key VARCHAR(64) NOT NULL,
  kind VARCHAR(32) NOT NULL,
  provider VARCHAR(32) NOT NULL,
  source_url VARCHAR(1024) NOT NULL,
  payload LONGTEXT NOT NULL,
  sha256 CHAR(64) CHARACTER SET ascii NOT NULL,
  fetched_at TIMESTAMP NOT NULL,
  PRIMARY KEY (race_key, kind),
  CONSTRAINT ck_bb_document_json CHECK (JSON_VALID(payload)),
  CONSTRAINT fk_bb_document_race FOREIGN KEY (race_key)
    REFERENCES boxbox_db.bb_races (race_key) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE boxbox_db.bb_password_hashes (
  id_user INT UNSIGNED NOT NULL,
  password_hash VARCHAR(255) CHARACTER SET ascii NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_user),
  CONSTRAINT fk_bb_password_user FOREIGN KEY (id_user)
    REFERENCES boxbox_db.app_users (id_user) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
