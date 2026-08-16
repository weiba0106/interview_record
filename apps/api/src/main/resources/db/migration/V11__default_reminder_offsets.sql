-- 默认提醒规则调整（用户确认）：所有日程类型统一在提前 24 小时和 2 小时各提醒一次。
-- 仅迁移仍为旧默认值的用户偏好（面试 [1440,30] → [1440,120]；截止 [1440] → [1440,120]），
-- 已自定义的偏好与单条日程的手动覆盖不受影响。
UPDATE user_preferences SET interview_reminder_offsets = '[1440,120]'
    WHERE interview_reminder_offsets = '[1440,30]';
UPDATE user_preferences SET deadline_reminder_offsets = '[1440,120]'
    WHERE deadline_reminder_offsets = '[1440]';
