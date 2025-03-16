INSERT INTO schema_version (version)
VALUES (?)
ON CONFLICT(version) DO UPDATE SET version = excluded.version;