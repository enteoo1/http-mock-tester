CREATE TABLE IF NOT EXISTS app_user (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'USER',
  timezone TEXT NOT NULL DEFAULT 'UTC',
  must_change_password INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS app_setting (
  setting_key TEXT PRIMARY KEY,
  setting_value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS mock_context (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  name TEXT NOT NULL COLLATE NOCASE,
  description TEXT,
  created_at TEXT NOT NULL,
  UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS scenario (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  context_id INTEGER NOT NULL REFERENCES mock_context(id) ON DELETE CASCADE,
  name TEXT NOT NULL COLLATE NOCASE,
  description TEXT,
  allowed_methods TEXT NOT NULL,
  body_mode TEXT NOT NULL DEFAULT 'OPTIONAL',
  require_valid_json INTEGER NOT NULL DEFAULT 0,
  auth_type TEXT NOT NULL DEFAULT 'NONE',
  api_key_header TEXT,
  response_status INTEGER NOT NULL DEFAULT 200,
  response_body TEXT,
  response_content_type TEXT,
  response_headers TEXT,
  delay_ms INTEGER NOT NULL DEFAULT 0,
  enabled INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL,
  UNIQUE (context_id, name)
);

CREATE TABLE IF NOT EXISTS request_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER,
  context_id INTEGER,
  scenario_id INTEGER,
  path_username TEXT,
  context_name TEXT,
  scenario_name TEXT,
  received_at TEXT NOT NULL,
  responded_at TEXT,
  method TEXT NOT NULL,
  url TEXT,
  query_string TEXT,
  remote_ip TEXT,
  request_headers TEXT,
  request_body TEXT,
  request_body_truncated INTEGER NOT NULL DEFAULT 0,
  outcome TEXT NOT NULL,
  response_status INTEGER NOT NULL,
  response_headers TEXT,
  response_body TEXT,
  duration_ms INTEGER
);

CREATE INDEX IF NOT EXISTS idx_log_user_time ON request_log(user_id, received_at);
CREATE INDEX IF NOT EXISTS idx_log_time ON request_log(received_at);
