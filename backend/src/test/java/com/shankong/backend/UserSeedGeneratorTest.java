package com.shankong.backend;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 种子数据生成器 —— 开发时手动跑一次的「工具类」，不是业务测试。
 *
 * 用途：数据库里现有的账号还是明文密码，切换成统一 UUID 主键 + BCrypt 密码之后，
 * 需要拿到「新的 id」和「加密后的密码」才能改库。这个类把两样东西都打出来，
 * 并且直接拼成可以粘贴到 Navicat / DataGrip 里执行的 SQL。
 *
 * ── 怎么跑 ──
 * VSCode：打开本文件，点方法左边那个 ▶（Run Test）。
 * 命令行：cd backend ; mvn test -Dtest=UserSeedGeneratorTest
 * 结果在「控制台 / 终端输出」里，搜 "-- 生成结果" 就能看到。
 *
 * ── 关于 BCrypt，两个必须知道的点 ──
 * 1. 它每次都加随机盐，所以**同一个密码跑两次得到的哈希完全不同**，这是正常的。
 *    不要拿两个哈希去比对，判断密码对不对只能用 encoder.matches(明文, 哈希)。
 * 2. 生成的哈希固定 60 个字符，所以数据库列至少 varchar(60)（我们这里是 varchar(100)，够用）。
 *
 * ── 关于 id ──
 * 用 IdWorker.get32UUID() 而不是 UUID.randomUUID()：前者是 32 位纯十六进制（不带横线），
 * 和实体上 @TableId(type = IdType.ASSIGN_UUID) 插入时自动生成的形式**完全一致**，
 * 混用两种格式以后会很难受。列宽 VARCHAR(32)。
 */
class UserSeedGeneratorTest {

    /**
     * 要生成密码哈希的账号。改这里就行，加多少行都可以。
     * 每行是 { 用户名, 明文密码 }。
     */
    private static final String[][] ACCOUNTS = {
            { "admin", "123456" },
            { "test", "123456" },
    };

    @Test
    void generateIdsAndHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println();
        System.out.println("================ -- 生成结果 -- ================");

        StringBuilder updateSql = new StringBuilder();

        for (String[] account : ACCOUNTS) {
            String username = account[0];
            String rawPassword = account[1];

            // 新主键：32 位，和 IdType.ASSIGN_UUID 同格式
            String id = IdWorker.get32UUID();
            // 密码哈希
            String hash = encoder.encode(rawPassword);

            // 立刻回验一次。哈希正确性的唯一判断方式就是 matches，
            // 这一步能防止「列宽不够被截断」「复制时漏了字符」这类问题悄悄发生
            boolean verified = encoder.matches(rawPassword, hash);

            System.out.println();
            System.out.println("用户名      : " + username);
            System.out.println("明文密码    : " + rawPassword);
            System.out.println("新的 id     : " + id + "   (长度 " + id.length() + ")");
            System.out.println("BCrypt 哈希 : " + hash + "   (长度 " + hash.length() + ")");
            System.out.println("回验结果    : " + (verified ? "通过" : "！！！失败，别用这个哈希"));

            updateSql.append("UPDATE `user` SET `password` = '").append(hash)
                    .append("' WHERE `username` = '").append(username).append("';\n");
        }

        System.out.println();
        System.out.println("================ -- 可直接粘贴的 SQL -- ================");
        System.out.println();
        System.out.println("-- 1) 把现有账号的密码换成哈希（上面的 id 是给新增行用的）");
        System.out.print(updateSql);
        System.out.println();
        System.out.println("-- 2) 新增一个用户（id 自己生成，这里只是示例）");
        System.out.println("INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `email`, `user_type`, `status`)");
        System.out.println("VALUES ('" + IdWorker.get32UUID() + "', 'newuser', '" + encoder.encode("123456")
                + "', '新用户', 'newuser@aihealth.com', 1, 1);");
        System.out.println();
        System.out.println("-- 3) 新增用户时可以直接把明文交给后端，让后端在 {@code UserService} 里 encode，");
        System.out.println("--    不要在前端或 SQL 里预先加密（盐就失去意义了）。");
        System.out.println();
        System.out.println("=====================================================");
        System.out.println();
    }
}
