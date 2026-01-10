CREATE TABLE IF NOT EXISTS session_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    device_info TEXT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_session_audit_logs_session_id ON session_audit_logs(session_id);
CREATE INDEX idx_session_audit_logs_user_id ON session_audit_logs(user_id);
CREATE INDEX idx_session_audit_logs_tenant_id ON session_audit_logs(tenant_id);
CREATE INDEX idx_session_audit_logs_created_at ON session_audit_logs(created_at);

CREATE TABLE IF NOT EXISTS session_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_session_events_session_id ON session_events(session_id);
CREATE INDEX idx_session_events_user_id ON session_events(user_id);
CREATE INDEX idx_session_events_tenant_id ON session_events(tenant_id);
CREATE INDEX idx_session_events_timestamp ON session_events(timestamp);

