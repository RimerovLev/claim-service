CREATE TABLE documents_v2 (
                              id BIGSERIAL PRIMARY KEY,
                              claim_id BIGINT NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
                              file_name VARCHAR(255) NOT NULL,
                              mime_type VARCHAR(255) NOT NULL,
                              file_size BIGINT NOT NULL,
                              storage_key VARCHAR(255) NOT NULL,
                              created_by BIGINT NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_documents_v2_claim_id ON documents_v2(claim_id);
CREATE UNIQUE INDEX uq_documents_v2_storage_key ON documents_v2(storage_key);
CREATE INDEX idx_documents_v2_deleted_at ON documents_v2(deleted_at);