-- 私信历史查询索引：按接收方 + 发送时间倒序分页
-- 说明：历史消息接口目前按 to_user_id 过滤并按 create_time 排序，补充复合索引避免回表扫描
ALTER TABLE message ADD INDEX idx_to_created (to_user_id, create_time);
