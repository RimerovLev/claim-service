-- Users need a password hash for authentication.
-- We store the BCrypt hash, never the plain-text password.
-- nullable for now so existing rows don't break on migration.
alter table users add column password_hash varchar(255);