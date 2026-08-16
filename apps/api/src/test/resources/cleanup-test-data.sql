-- This runs only through MySqlIntegrationTestBase, whose external connection
-- guard accepts exactly the dedicated interview_record_test schema.
DELETE FROM SPRING_SESSION_ATTRIBUTES;
DELETE FROM SPRING_SESSION;
DELETE FROM rate_limit_buckets;
DELETE FROM interview_questions;
DELETE FROM schedule_events;
DELETE FROM interview_rounds;
DELETE FROM positions;
DELETE FROM companies;
DELETE FROM job_types;
DELETE FROM position_statuses;
DELETE FROM user_preferences;
DELETE FROM users;
