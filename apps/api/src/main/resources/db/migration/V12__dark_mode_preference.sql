-- 暗色模式偏好（V12）：与四套强调色主题正交，默认关闭。
ALTER TABLE user_preferences ADD COLUMN dark_mode BOOLEAN NOT NULL DEFAULT FALSE AFTER theme;
