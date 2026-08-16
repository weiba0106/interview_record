-- 富文本字段升级：岗位描述、面试过程记录、整体复盘（V9）
-- 写入时经过服务端白名单清洗，读出时再次清洗兜底旧数据。
ALTER TABLE positions MODIFY COLUMN description LONGTEXT NULL;
ALTER TABLE interview_rounds MODIFY COLUMN process_notes LONGTEXT NULL;
ALTER TABLE interview_rounds MODIFY COLUMN review_summary LONGTEXT NULL;
