-- This runs only through MySqlIntegrationTestBase, whose external connection
-- guard accepts exactly the dedicated interview_record_test schema.
DELETE FROM SPRING_SESSION_ATTRIBUTES;
DELETE FROM SPRING_SESSION;
DELETE FROM rate_limit_buckets;
DELETE FROM users;
