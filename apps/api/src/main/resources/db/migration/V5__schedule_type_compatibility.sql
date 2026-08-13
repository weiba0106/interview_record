-- V4 used ASSESSMENT while the approved PRD defines HR communication instead.
-- Preserve existing assessment records by treating them as written tests before
-- replacing the check constraint with the six PRD types.
UPDATE schedule_events
SET event_type = 'WRITTEN_TEST'
WHERE event_type = 'ASSESSMENT';

ALTER TABLE schedule_events DROP CHECK ck_schedules_type;

ALTER TABLE schedule_events
    ADD CONSTRAINT ck_schedules_type
    CHECK (event_type IN (
        'INTERVIEW', 'WRITTEN_TEST', 'HR_COMMUNICATION',
        'APPLY_DEADLINE', 'OFFER_DEADLINE', 'CUSTOM'
    ));
