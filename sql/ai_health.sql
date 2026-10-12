-- ==========================================================
-- AI 心理健康平台 —— 本地数据库初始化脚本
-- 数据库名：ai_health（和 backend 的 application.yml 里 url 一致）
--
-- 用法：
--   1. 打开 Navicat / DataGrip / 命令行，连上本地 MySQL
--   2. 直接整个文件执行，或者：mysql -uroot -p < ai_health.sql
--
-- 【这份表结构是怎么来的】
--   远程后端是别人写的，我拿不到它的建表语句，所以这里的字段是
--   **从前端实际发出的请求和实际读取的字段反推出来的**，保证前端不用改代码就能对上。
--   依据是这几个文件：
--     src/api/admin.js                 —— 接口地址和提交的参数名
--     src/views/login.vue              —— 登录返回 token + userInfo.userType
--     src/views/knowledge.vue          —— 分页参数、列表列名
--     src/components/ArticleDialog.vue —— 文章表单字段
--     src/components/RichTextEditor.vue—— 图片上传参数
--
-- 【两个容易踩的点】
--   1. 时间列名。这里分两种情况，别搞混：
--        · user 表：已按 backend 的 entity/User.java 对齐，用的是
--          created_at / updated_at（实体里也显式写了 @TableField("created_at")）。
--        · 其余三张表：暂时还是 create_at / update_at，
--          配合 application.yml 里的 map-underscore-to-camel-case: true
--          映射成 Java 的 createAt / updateAt。
--      前端列表里"发布时间"这一列取的就是 updateAt，别改成 update_time，
--      否则前端要一起改。以后给这几张表写实体类时，要么实体跟着写成 createAt，
--      要么把表统一改成 created_at / updated_at，两边必须一致。
--   2. 分页接口要返回 { records, total } 两个字段
--      （前端是 const { records, total } = await articlePage(paras) 这样解的），
--      入参是 currentPage 和 size。
-- ==========================================================


-- ---------------- 建库 ----------------
CREATE DATABASE IF NOT EXISTS `ai_health`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `ai_health`;


-- ==========================================================
-- 1. 用户表
--    对应接口：POST /user/login
--    返回：{ token, userInfo }，其中 userInfo.userType === 2 代表后台管理员
-- ==========================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`         VARCHAR(32)  NOT NULL                COMMENT '主键：32 位 UUID（不带横线），插入时由 MyBatis-Plus 的 IdType.ASSIGN_UUID 生成，所以不写 AUTO_INCREMENT',
    `username`   VARCHAR(100) NOT NULL                COMMENT '用户名，登录用（4~100 位，只能字母/数字/下划线）',
    `password`   VARCHAR(100) NOT NULL                COMMENT '密码：BCrypt 哈希（60 字符，$2a$10$ 开头），只能用 BCryptPasswordEncoder.matches() 校验',
    `nickname`   VARCHAR(50)      NULL                COMMENT '昵称',
    `email`      VARCHAR(100) NOT NULL                COMMENT '邮箱（登录框写的是"用户名和邮箱"，两者都能登录）',
    `phone`      VARCHAR(20)      NULL                COMMENT '手机号，1[3-9] 开头的 11 位',
    `avatar`     VARCHAR(255)     NULL                COMMENT '头像地址',
    `gender`     TINYINT          NULL                COMMENT '性别：1=男，2=女',
    `birthday`   DATE             NULL                COMMENT '生日',
    `user_type`  TINYINT      NOT NULL DEFAULT 1      COMMENT '角色：1=普通用户，2=后台管理员',
    `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0=禁用，1=正常（对应 enumClass/UserStatus.java，User.isActive() 判的就是 ==1）',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';


-- ==========================================================
-- 2. 文章分类表
--    对应接口：GET /knowledge/category/tree
--    返回：[{ id, categoryName, children }]
--    parent_id = 0 表示顶级分类，前端目前只用了一层，但留着字段方便以后做树
-- ==========================================================
CREATE TABLE IF NOT EXISTS `knowledge_category` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`     BIGINT      NOT NULL DEFAULT 0      COMMENT '父分类 id，0=顶级',
    `category_name` VARCHAR(50) NOT NULL                COMMENT '分类名称',
    `sort`          INT         NOT NULL DEFAULT 0      COMMENT '排序，小的在前',
    `status`        TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：0=禁用，1=启用',
    `create_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识文章分类表';


-- ==========================================================
-- 3. 知识文章表
--    对应接口：
--      GET    /knowledge/article/page      列表（?currentPage=&size=&title=&categoryId=&status=）
--      GET    /knowledge/article/{id}      详情
--      POST   /knowledge/article           新增
--      PUT    /knowledge/article/{id}      修改
--      PUT    /knowledge/article/{id}/status 改状态（发布/下线）
--
--    status 三个值直接对应前端筛选项：
--      0 = 草稿（前端"发布"按钮出现）
--      1 = 已发布（前端"下线"按钮出现）
--      2 = 已下线（前端"发布"按钮出现）
-- ==========================================================
CREATE TABLE IF NOT EXISTS `knowledge_article` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(200)  NOT NULL                COMMENT '文章标题',
    `category_id` BIGINT        NOT NULL                COMMENT '分类 id，对应 knowledge_category.id',
    `summary`     VARCHAR(1000)     NULL                COMMENT '摘要',
    `content`     LONGTEXT          NULL                COMMENT '正文，存富文本 HTML',
    `cover_image` VARCHAR(255)      NULL                COMMENT '封面图相对路径，前端用 fileBaseUrl 拼完整地址',
    `tags`        VARCHAR(255)      NULL                COMMENT '标签，逗号分隔（前端提交前 join(",")）',
    `status`      TINYINT       NOT NULL DEFAULT 0      COMMENT '0=草稿，1=已发布，2=已下线',
    `author_id`   VARCHAR(32)       NULL                COMMENT '作者 id，对应 user.id（32 位 UUID 字符串）',
    `author_name` VARCHAR(50)       NULL                COMMENT '作者名，列表直接显示，省一次连表',
    `read_count`  INT           NOT NULL DEFAULT 0      COMMENT '阅读量',
    `create_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，前端"发布时间"列取这个字段',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_update_at` (`update_at`),
    KEY `idx_title` (`title`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识文章表';


-- ==========================================================
-- 4. 文件上传记录表
--    对应接口：POST /file/upload（multipart）
--    入参：file、businessType（前端固定传 ARTICLE）、businessId（UUID）、businessField
--    返回：{ filePath }  ← 注意字段名就叫 filePath，前端拿它拼图片地址
--
--    business_field 用来区分这张图是干什么的：
--      cover   = 文章封面
--      content = 正文里插入的图片
-- ==========================================================
CREATE TABLE IF NOT EXISTS `file_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_name`      VARCHAR(255)     NULL                COMMENT '原始文件名',
    `file_path`      VARCHAR(500) NOT NULL                COMMENT '相对存储路径，上传接口返回的 filePath',
    `file_size`      BIGINT           NULL                COMMENT '文件大小，单位字节',
    `file_type`      VARCHAR(100)     NULL                COMMENT 'MIME 类型，如 image/png',
    `business_type`  VARCHAR(50)      NULL                COMMENT '业务类型，目前只有 ARTICLE',
    `business_id`    VARCHAR(64)      NULL                COMMENT '业务 id，前端生成的 UUID',
    `business_field` VARCHAR(50)      NULL                COMMENT '用途：cover=封面，content=正文插图',
    `create_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_business` (`business_type`, `business_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件上传记录表';


-- ==========================================================
-- 初始化数据
-- ==========================================================

-- ---------------- 用户 ----------------
-- 密码列存的是 **BCrypt 哈希**，不是明文。
-- 怎么生成：跑 backend/src/test/java/com/shankong/backend/UserSeedGeneratorTest.java
--           （cd backend ; mvn test -Dtest=UserSeedGeneratorTest），它会打印每个账号的新 id、
--           哈希、回验结果和可直接粘贴的 SQL。
-- 两个要点：
--   1) BCrypt 每次掺随机盐，同一个明文两次算出的哈希不一样，所以下面这两串只是当时那一次的结果，
--      自己重跑测试拿到新哈希替换即可；
--   2) 校验只能用 BCryptPasswordEncoder.matches(明文, 哈希)，不能拿字符串直接相等比较。
-- 新增用户时直接把明文交给后端 encode（盐才有意义），不要在前端或 SQL 里预先加密。
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `gender`, `birthday`, `user_type`, `status`)
VALUES ('4b42b7912c60132dad7822002105c673', 'admin', '$2a$10$oOla05JBlIVK83sCMsBZm.UgH9SwtJxktFSX2hXr.rcpOsSdc/KvW', '超级管理员', 'admin@aihealth.com', '19999525252', 1, '2019-09-02', 2, 1),
       ('81aa16cce2320397bc9160ad9209ca8c', 'test', '$2a$10$nkvaKOWcK8beNZpIYOQ.BeHI0OF5sV8OJXtsaCZLtEGB6cgR1UHk.', '测试用户', 'test@aihealth.com', '13800138000', 1, NULL, 1, 1)
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;


-- ---------------- 文章分类 ----------------
INSERT INTO `knowledge_category` (`id`, `parent_id`, `category_name`, `sort`)
VALUES (1, 0, '情绪管理', 1),
       (2, 0, '焦虑与压力', 2),
       (3, 0, '人际关系', 3),
       (4, 0, '自我成长', 4),
       (5, 0, '睡眠健康', 5)
ON DUPLICATE KEY UPDATE `update_at` = CURRENT_TIMESTAMP;


-- ---------------- 文章示例 ----------------
-- 放两条数据，方便一进页面就能看到列表、顺便验证分页和筛选
INSERT INTO `knowledge_article`
    (`id`, `title`, `category_id`, `summary`, `content`, `cover_image`, `tags`, `status`, `author_id`, `author_name`, `read_count`)
VALUES (1, '总是焦虑怎么办？试试这 5 个方法', 2,
        '焦虑本身不是坏事，它是身体在提醒你"有事情需要处理"。',
        '<p>焦虑本身不是坏事，它是身体在提醒你<strong>有事情需要处理</strong>。</p><p>下面 5 个方法可以帮你把焦虑降下来：</p><ul><li>把担心的事写下来</li><li>做 4-7-8 呼吸</li><li>规律作息</li><li>减少咖啡因</li><li>找人聊一聊</li></ul>',
        NULL, '焦虑,情绪管理,压力管理', 1, '4b42b7912c60132dad7822002105c673', '超级管理员', 0),
       (2, '如何建立健康的睡眠习惯', 5,
        '睡眠是最容易被牺牲、又最影响情绪的一件事。',
        '<p>睡眠是最容易被牺牲、又最影响情绪的一件事。</p><p>固定起床时间，比固定入睡时间更重要。</p>',
        NULL, '睡眠健康,健康生活', 1, '4b42b7912c60132dad7822002105c673', '超级管理员', 0)
ON DUPLICATE KEY UPDATE `update_at` = CURRENT_TIMESTAMP;


-- ==========================================================
-- 建完之后建议先跑一下这两句，确认数据进去了：
--   SELECT id, username, user_type FROM `user`;
--   SELECT id, title, status FROM knowledge_article;
-- ==========================================================
