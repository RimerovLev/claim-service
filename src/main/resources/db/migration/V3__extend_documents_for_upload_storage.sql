ALTER TABLE documents
    ADD COLUMN file_name VARCHAR(255),
    ADD COLUMN mime_type VARCHAR(255),
    ADD COLUMN file_size BIGINT,
    ADD COLUMN storage_key VARCHAR(255),
    ADD COLUMN created_by BIGINT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE documents
    ADD CONSTRAINT uq_documents_storage_key UNIQUE (storage_key);

CREATE INDEX idx_documents_deleted_at ON documents(deleted_at);
