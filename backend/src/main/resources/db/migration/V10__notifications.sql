CREATE TABLE notifications (
 id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES app_users(id),event_key VARCHAR(300) NOT NULL,
 kind VARCHAR(20) NOT NULL,title VARCHAR(200) NOT NULL,body VARCHAR(1000) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,read_at TIMESTAMP WITH TIME ZONE,
 UNIQUE(user_id,event_key)
);
CREATE INDEX idx_notifications_user ON notifications(user_id,created_at);
