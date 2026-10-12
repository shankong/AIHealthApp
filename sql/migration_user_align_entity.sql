-- ==========================================================
-- 迁移脚本：把已存在的 user 表对齐到 entity/User.java
--
-- 背景：库里的 user 表是早先按"从远程接口反推"的字段建的，列名是
--       create_at / update_at，而且没有 gender / birthday。
--       新写的 entity/User.java 用 MyBatis-Plus，@TableField 明确要求
--       created_at / updated_at，并且实体里有 gender 和 birthday 两个字段。
--       MyBatis-Plus 生成的 SQL 会把实体所有字段都带上，于是变成：
--         select ... gender, birthday, ..., created_at, updated_at from user
--       列不存在 -> Unknown column 'gender' in 'field list'
--
-- 执行方式：
--   mysql -uroot -p --default-character-set=utf8mb4 -e "source sql/migration_user_align_entity.sql"
--   或者在 Navicat / DataGrip 里打开本文件直接执行。
--
-- 安全性：RENAME COLUMN 是改名字，不动数据；新增的两列可空，
--         现有行不会被影响。执行前建议先备份：
--         mysqldump -uroot -p ai_health user > user_backup.sql
-- ==========================================================

USE `ai_health`;

ALTER TABLE `user`
    -- 实体里 username 允许 4~100 位，原来只有 50
    MODIFY COLUMN `username` VARCHAR(100) NOT NULL COMMENT '用户名，登录用（4~100 位，只能字母/数字/下划线）',
    -- 实体上标了 @NotBlank，邮箱不能为空
    MODIFY COLUMN `email`    VARCHAR(100) NOT NULL COMMENT '邮箱（登录框写的是"用户名和邮箱"，两者都能登录）',
    -- 实体里新增的两个字段
    ADD COLUMN `gender`      TINYINT NULL COMMENT '性别：1=男，2=女' AFTER `avatar`,
    ADD COLUMN `birthday`    DATE    NULL COMMENT '生日' AFTER `gender`,
    -- 关键的两处改名，对应 @TableField("created_at") / @TableField("updated_at")
    RENAME COLUMN `create_at` TO `created_at`,
    RENAME COLUMN `update_at` TO `updated_at`;

-- 执行完可以用这句自查（应能看到 created_at / updated_at / gender / birthday）：
-- SHOW COLUMNS FROM `user`;
