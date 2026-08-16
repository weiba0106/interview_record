-- 单条日程提醒覆盖：
--   NULL      跟随用户偏好默认规则
--   ''        关闭提醒
--   '1440,30' 自定义提醒时间（事件前分钟数，逗号分隔，倒序去重）
ALTER TABLE schedule_events
    ADD COLUMN reminder_offsets VARCHAR(120) NULL AFTER manual_urgency;
