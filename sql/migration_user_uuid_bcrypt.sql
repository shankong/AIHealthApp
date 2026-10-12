-- ==========================================================
-- 迁移：user 主键 BIGINT 自增 → VARCHAR(32) UUID，密码明文 → BCrypt 哈希
--       knowledge_article.author_id 同步改成 VARCHAR(32)
--
-- 适用：本地库 ai_health（root/123456）
-- 注意：**只跑一次**。MySQL 的 DDL 不能回滚，跑之前先备份：
--       & "D:\WorkSoftware\mysql-8.0.34-winx64\bin\mysqldump.exe" -uroot -p123456 --default-character-set=utf8mb4 ai_health > sql\backup_before_uuid.sql
-- 执行：
--       & "D:\WorkSoftware\mysql-8.0.34-winx64\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 ai_health -e "source E:/bilibili_project/sql/migration_user_uuid_bcrypt.sql"
--
-- 为什么可以直接改主键类型：本地没有任何外键引用 user.id
-- （已用 information_schema.KEY_COLUMN_USAGE 查过，结果为空），所以不用先拆外键。
--
-- 为什么 id 是 32 位不带横线：MyBatis-Plus 的 IdType.ASSIGN_UUID 生成的就是这种
-- （等价 IdWorker.get32UUID()，32 位十六进制），所以列宽 VARCHAR(32)，格式必须和它一致。
-- ==========================================================

USE ai_health;

-- ----------------------------------------------------------
-- 1. 先加临时列算出新 UUID，不动老主键（这样出问题还能退回）
-- ----------------------------------------------------------
ALTER TABLE `user` ADD COLUMN `id_new` VARCHAR(32) NULL AFTER `id`;

-- 演示账号固定成 UserSeedGeneratorTest 里打印出来的那两个 id，
-- 这样 sql/ai_health.sql 的种子数据、以及文章表的 author_id 都能对得上。
UPDATE `user` SET `id_new` = '4b42b7912c60132dad7822002105c673' WHERE `username` = 'admin';
UPDATE `user` SET `id_new` = '81aa16cce2320397bc9160ad9209ca8c' WHERE `username` = 'test';

-- 其余行（如果以后加了账号）自己生成一个：UUID() 每行求值一次，所以每行都不一样
UPDATE `user` SET `id_new` = REPLACE(UUID(), '-', '') WHERE `id_new` IS NULL;

-- ----------------------------------------------------------
-- 2. 文章表：按老的 author_id = user.id 关联，把作者换成新 UUID
-- ----------------------------------------------------------
ALTER TABLE `knowledge_article` ADD COLUMN `author_id_new` VARCHAR(32) NULL AFTER `author_id`;
UPDATE `knowledge_article` a JOIN `user` u ON a.`author_id` = u.`id` SET a.`author_id_new` = u.`id_new`;

-- ----------------------------------------------------------
-- 3. 换主键：先把老 id 的 AUTO_INCREMENT 摘掉，再删列、改名、重建主键
--    （直接 DROP PRIMARY KEY 会报 1075：auto 列必须是 key，所以分步骤来）
-- ----------------------------------------------------------
ALTER TABLE `user` MODIFY COLUMN `id` BIGINT NOT NULL COMMENT '旧主键（迁移用，本脚本第 4 步删掉）';
ALTER TABLE `user` DROP PRIMARY KEY;
ALTER TABLE `user` DROP COLUMN `id`;
ALTER TABLE `user` CHANGE COLUMN `id_new` `id` VARCHAR(32) NOT NULL COMMENT '主键：32 位 UUID（不带横线），插入时由 MyBatis-Plus 的 IdType.ASSIGN_UUID 生成';
ALTER TABLE `user` ADD PRIMARY KEY (`id`);

ALTER TABLE `knowledge_article` DROP COLUMN `author_id`;
ALTER TABLE `knowledge_article` CHANGE COLUMN `author_id_new` `author_id` VARCHAR(32) NULL COMMENT '作者 id，对应 user.id（32 位 UUID 字符串）';

-- ----------------------------------------------------------
-- 4. 密码明文 → BCrypt 哈希（60 字符，$2a$10$ 开头）
--    哈希来自 backend/src/test/java/com/shankong/backend/UserSeedGeneratorTest.java，
--    已用它自己的 matches() 回验通过。BCrypt 每次加随机盐，同一明文两次结果不同，
--    想换一批就重跑那个测试，把下面两串换掉。
-- ----------------------------------------------------------
UPDATE `user` SET `password` = '$2a$10$oOla05JBlIVK83sCMsBZm.UgH9SwtJxktFSX2hXr.rcpOsSdc/KvW' WHERE `username` = 'admin';
UPDATE `user` SET `password` = '$2a$10$nkvaKOWcK8beNZpIYOQ.BeHI0OF5sV8OJXtsaCZLtEGB6cgR1UHk.' WHERE `username` = 'test';
ALTER TABLE `user` MODIFY COLUMN `password` VARCHAR(100) NOT NULL COMMENT '密码：BCrypt 哈希（60 字符），只能用 BCryptPasswordEncoder.matches() 校验';

-- ----------------------------------------------------------
-- 5. 自检：id 应该是 32 位、密码应该是 60 位且以 $2a$10$ 开头、文章 author_id 应该能对上 user.id
-- ----------------------------------------------------------
SELECT `id`, `username`, LENGTH(`id`) AS id_len, LEFT(`password`, 7) AS pw_prefix,
       LENGTH(`password`) AS pw_len, `user_type`, `status`
FROM `user`;

SELECT a.`id`, a.`author_id`, a.`author_name`, u.`username` AS matched_author
FROM `knowledge_article` a LEFT JOIN `user` u ON a.`author_id` = u.`id`;
