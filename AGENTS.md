# AGENTS.md — bilibili_project（AI 健康咨询平台）

> 这份文件由 DSH 的 `@deepseek-ai/dsh-agent-instructions` 插件**自动注入每个会话的上下文**
> （配置见 `~/.dsh/cordis.patch.yml`，上限 64KB）。
> 两个会话（前端 / 后端）在同一工作区，都会读到同一份内容 —— 这就是跨会话共享记忆的地方。
>
> **维护约定：改了数据库结构、加了接口、踩了新的坑，请回来更新本文件。**
> 这里写一条，以后每个新会话就少读一遍项目。

## 1. 项目概览

前后端分离的 AI 心理健康咨询平台，分三块：

- `backend/` — Spring Boot 后端，包名 `com.shankong.backend`，端口 **8080**
- `frontend/` — Vue 3 后台管理界面
- `sql/` — 建表与迁移脚本

## 2. 技术栈与版本（已在本机实测确认）

| 项 | 版本 / 路径 |
|---|---|
| JDK | Java **17.0.1**，`JAVA_HOME=D:\WorkSoftware\jdk` |
| Maven | **3.9.4**，`MAVEN_HOME=D:\WorkSoftware\apache-maven-3.9.4`（项目里的 `mvnw` 已删除） |
| Spring Boot | **4.0.8**（parent），Spring Framework 7.0.9 |
| 持久层 | **MyBatis-Plus 3.5.17**（`com.baomidou:mybatis-plus-spring-boot4-starter`） |
| MySQL | **8.0.34**，`D:\WorkSoftware\mysql-8.0.34-winx64\bin\mysql.exe` |
| JWT | **auth0 java-jwt 4.5.0**（`com.auth0:java-jwt`，**不是 jjwt**）。配置类 `config/JwtConfig.java`，签发/校验工具 `util/JWTTokenUtil.java` |
| 其他 | lombok、spring-boot-starter-validation、spring-boot-starter-security、devtools |
| 前端 | Vue 3 + Vite + Element Plus + axios + vue-router |

后端依赖树里**同时存在** `org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1` 和
MyBatis-Plus 的 starter —— 两套 autoconfiguration 会抢 `SqlSessionFactory`。目前是 MyBatis-Plus
抢赢的，能用，但属于待清理项：删掉纯 MyBatis 那两个 starter（含 `-test`）更干净。

## 3. 常用命令

```powershell
# 编译后端（最常用的验证手段）
cd E:\bilibili_project\backend ; mvn compile

# 启动后端（另起端口做隔离验证，避免动到正在跑的 8080）
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"

# 连数据库
& "D:\WorkSoftware\mysql-8.0.34-winx64\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 ai_health

# 生成「新用户 id + BCrypt 密码哈希」（会打印 32 位 UUID、60 字符哈希、回验结果和可粘贴的 SQL）
cd E:\bilibili_project\backend ; mvn test -Dtest=UserSeedGeneratorTest

# 执行某个迁移脚本（source 是 mysql 客户端命令，路径用正斜杠）
& "D:\WorkSoftware\mysql-8.0.34-winx64\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 ai_health -e "source E:/bilibili_project/sql/migration_user_uuid_bcrypt.sql"

# 前端
cd E:\bilibili_project\frontend ; npm run dev
```

VSCode 里按 **F5** 启动后端，`.vscode/launch.json` 用的是 `console: integratedTerminal`
（改成内部调试控制台会导致中文乱码 + ANSI 颜色变成 `[31m` 原样字符）。

## 4. 数据库

库名 `ai_health`，本地账号 `root / 123456`（见 `backend/src/main/resources/application.yml`）。

表：`user`、`knowledge_category`、`knowledge_article`、`file_record`。

**字段命名的坑（重要）**：`user` 表已经对齐 `entity/User.java`，用的是 `created_at` / `updated_at`，
并且有 `gender`、`birthday`；**其余三张表还是 `create_at` / `update_at`（少一个 d）**。
以后给它们写实体类时，两边必须一致，否则就是 `Unknown column` 或空字段。

- `user.id`：**VARCHAR(32) 的 UUID 字符串**（32 位十六进制、不带横线），插入时由 MyBatis-Plus 的
  `@TableId(type = IdType.ASSIGN_UUID)` 自动生成 —— 所以建表**不能写 AUTO_INCREMENT**，
  `entity/User.java` 的 id 是 `String` 不是 `Long`。`knowledge_article.author_id` 同样是 VARCHAR(32)，
  存的就是 `user.id`。（`knowledge_article.id` / `knowledge_category.id` 还是 BIGINT 自增）
- `user.status`：**0=禁用，1=正常**（以 `enumClass/UserStatus.java` 为准：`DISABLED(0)` / `NORMAL(1)`；
  `entity/User.java` 的 `isActive()` 判的就是 `status == 1`。**注意**：早期文档写的「1=正常，2=禁用」是错的，
  2 在这里不是禁用）；`user.user_type`：**1=普通用户，2=管理员**
- 测试账号：`admin / 123456`（管理员，user_type=2，id=`4b42b7912c60132dad7822002105c673`）、
  `test / 123456`（普通用户，id=`81aa16cce2320397bc9160ad9209ca8c`）
- 密码存的是 **BCrypt 哈希**（60 字符、`$2a$10$` 开头），`UserService` 里用
  `BCryptPasswordEncoder.matches(明文, 哈希)` 校验。**不能拿字符串直接比**；库里如果还是明文
  `'123456'`，`matches()` 返回 false，表现是「密码错误」而不是格式错。
- 迁移脚本（都在 `sql/`，按顺序执行，都已跑过）：
  1. `migration_user_align_entity.sql` —— `create_at`→`created_at`、加 `gender`/`birthday`
  2. `migration_user_uuid_bcrypt.sql` —— 主键转 VARCHAR(32) UUID、密码转 BCrypt、文章 `author_id` 同步转
- 要造新账号：跑 `UserSeedGeneratorTest` 拿 id + 哈希，别手写哈希（随机盐，写不出来）。

MyBatis-Plus 的 `selectOne` 会生成**实体全字段**的 SELECT，所以实体里多一个字段、
表里少一列，就是 `Unknown column 'xxx' in 'field list'`。

## 5. 接口现状

### 已实现

| 接口 | 状态 |
|---|---|
| `POST /api/user/login` | **已完成**（2026-10-12 在隔离的 8081 实例上端到端验证过）。按「用户名或邮箱」查库（`BusinessException("用户不存在")`）、BCrypt 校验密码（`"密码错误"`）、`user.isActive()` 判禁用（`"用户已被禁用，请联系管理员"`）、`JWTTokenUtil.generateToken(...)` 签发 token、`service/convert/UserConvert.java` 组装 DTO，`controller/User.java` 用 `Result.success(...)` 包一层。<br>实测 `admin/123456` → `{"code":"200","msg":"操作成功","data":{"token":"…（264 字符）","roleType":"2","userInfo":{…}}}`；`userInfo` 里 `nickname`/`displayName` 都是「超级管理员」，`userTypeDisplayName`=管理员、`statusDisplayName`=正常、`genderDisplayName`=男、`birthday`=`2019-09-02` |

登录请求体（`UserLoginCommandDTO`，`@Valid` 校验）：`username` 4~100 位、`password` 6~50 位，都不能为空。

### 前端已调用但后端未实现（现在打过去是 404）

- `GET /knowledge/category/tree`
- `GET /knowledge/article/page?currentPage=&size=&title=&categoryId=&status=`
- `GET /knowledge/article/{id}`、`POST /knowledge/article`、`PUT /knowledge/article/{id}`
- `PUT /knowledge/article/{id}/status`
- `POST /file/upload`（multipart：`file` / `businessType=ARTICLE` / `businessId=UUID` / `businessField=cover|content`）

### 远程后端（重要参考）

前端 `vite.config.js` 的 `/api` 代理指向 `http://159.75.169.224:1235/` —— 那是**功能完整的参考实现**，
本地这份是在重写它。两边契约**已经不一致**，写代码前先对齐：

| | 本地 | 远程 |
|---|---|---|
| 响应体外层 | `{code,msg,data}` | 多 `message`、`success` 两个字段 |
| 用户主键 | `VARCHAR(32)` UUID（已对齐） | UUID 字符串 |
| 文章主键 | `BIGINT` 自增 | **UUID 字符串** |
| 登录返回 | **已实现** `{token, roleType, userInfo}`，`userInfo` 含 `gender`/`birthday`/`displayName`/`xxxDisplayName`/`status`/`createdAt`（字段与远程基本一致，已能直接用） | 同左 |
| 分类接口 | 未实现 | 返回**扁平数组**（叫 tree 但没有 children） |
| 时间格式 | `LocalDateTime` 直接序列化成 **`"2026-09-30T14:58:27"`**（带 `T`，还没配 Jackson 的日期格式） | `"2025-08-30 12:00:01"` |

### 安全配置（`config/SecurityConfig.java`）

`PUBLIC_PATHS = { "/", "/api/test", "/api/user/login" }`，其余 `anyRequest().authenticated()`，
CSRF 关闭、`STATELESS`。

- 未认证访问非放行路径返回 **403**（没开 `formLogin`/`httpBasic`，所以不会跳登录页、也不是 401）
- **没有 JWT 过滤器**，登录签发的 token 目前没有任何地方校验它
- `util/JWTTokenUtil.java`（auth0 java-jwt）目前只有一个静态方法
  `generateToken(String userId, String username, Integer userType)`：用 `ApplicationContextAware`
  静态拿到 `config/JwtConfig.java`（`@Component @ConfigurationProperties(prefix="jwt")`，字段
  `secret`/`expiration`/`refreshExpiration`/`issuer`/`header`/`tokenPrefix`）里的 secret，
  算法 `Algorithm.HMAC256(secret)`，写 iss + userId/username/userType + iat/exp。**解析/校验方法还没写**。
- 曾经的 `util/JwtUtil.java`（jjwt 版）**已删除**，别再照着它改
- 启动日志里的 `Using generated security password: ...` 是 Boot 自动建的 in-memory 用户，现在用不上

## 6. 已经踩过的坑（别再踩）

1. **`Result.success(T data)` 必须同时设 `code` 和 `msg`**。前端 `requst.js` 判断的是
   `String(data.code) === '200'`，只 `setData` 的话前端把成功当失败，弹「请求失败」。
2. **控制器不要重新包装 service 的返回值**。`Result.success(result.getData())` 会把
   `Result.error`（data=null）包装成 code=200 的「成功」，前端永远看不到错误提示；
   service 返回 null 时还会直接空指针。
3. **`@ExceptionHandler(Exception.class)` 的参数类型必须真的是 `Exception`**。
   写成 `MethodArgumentNotValidException` 会导致参数解析失败、异常接不住，
   前端看到 Spring 默认的 500 大堆栈。
4. **兜底异常处理器会把「路径不存在」也变成 `code=500`**：未匹配的路径会抛
   `NoResourceFoundException`，被兜底接住后返回 `{"code":"500","msg":"系统错误"}`（HTTP 状态码其实是 200）。
   调接口报 500 时，**先确认这个接口到底写了没有**。
5. **控制台一片白**：`application.yml` 里设了 `spring.output.ansi.enabled: always`。
   Spring Boot 默认是 `detect`，在本机判断失败就不上色。
6. **devtools 会盯着 `target/classes`**：只要 IDE 编译写入了这个目录，正在运行的实例就会自动重启；
   如果重启时正好撞上代码改到一半的状态，上下文可能起不来 —— 表现为 **JVM 还在、端口不监听**。
   排查时先看 `Get-NetTCPConnection -LocalPort 8080 -State Listen`。
7. **前端请求头里的 token 字段名就叫 `token`**（不是 `Authorization`），见 `frontend/src/utils/requst.js`。
8. **`jwt.secret` 目前明文写在 `application.yml`**，生产要挪到环境变量。
9. **BCrypt 哈希是随机盐，不可手写、不可比较字符串**。登录一直报「密码错误」时先
   `SELECT LENGTH(password), LEFT(password,7) FROM user;`：不是 60 / 不是 `$2a$10$` 就说明还是明文。
10. **Maven 写本地仓库会被 DSH 沙箱拒绝**：本地仓库 `D:\WorkSoftware\apache-maven-3.9.4\mvn_repo`
    在工作区外，`workspace-write` 策略下跑 `mvn compile` / `mvn test` 会 `BUILD FAILURE`，报
    `java.nio.file.AccessDeniedException: ...\mvn_repo\org\springframework\boot\spring-boot-starter-security\4.0.8`。
    需要放宽到 full access 再跑（或把 `-Dmaven.repo.local` 指到工作区内，但那样要重下全部依赖）。
11. **`code=500`「系统错误」不等于后端崩了**。`common/GlobalExceptionHandler.java` 只精确接住三类异常：
    `MethodArgumentNotValidException`（→400）、`HttpMessageNotReadableException`（→400「请求体不是合法的
    JSON」）、`BusinessException`；其余全落进 `@ExceptionHandler(Exception.class)` 的兜底变成 500。
    **实测**：body 少写引号（`{username:"admin"}`）在加这个 handler 之前返回
    `{"code":"500","msg":"系统错误"}`，加了之后返回 `{"code":"400","msg":"参数错误"}`。
    用 curl 复现时注意 **Windows PowerShell 5.1 会把 `-d '{...}'` 里的双引号吃掉**，发出去的是非法 JSON，
    于是自己造出一个 500 还以为是后端坏了 —— 要用 `-d "@body.json"` 传文件。
12. **「HTTP 200 + 空 body」是 service 成功分支漏了 `return`（返回了 `null`）**，不是后端崩溃。
    `UserService.login` 曾经就是这样（末尾 `return null`），2026-10-12 已补完 DTO 组装并验证通过。
    以后某个接口拿不到 `code`、前端弹「请求失败」而控制台又没异常时，先看 service 是不是走到底没返回值。

## 7. 现有代码约定

- 统一响应体：`common/Result<T>` + `common/ResultCode` 枚举（`200` 成功、`-1` 失败、`400` 参数错误、
  `401` 未登录、`500` 系统错误、`6001~6004` 业务错误、`A02xx` token 相关）
- 分层：`controller` → `service` → `mapper`（MyBatis-Plus `BaseMapper`），
  入参 DTO 放 `DTO/command`，出参 DTO 放 `DTO/response`，实体放 `entity`
- **service 返回业务 DTO，controller 用 `Result.success(...)` 包**：
  `controller/User.java` 里是 `UserLoginResponseDTO result = userService.login(commandDTO); return Result.success(result);`。
  别反过来让 service 返回 `Result`、controller 再 `Result.success(result.getData())`（见坑 2）
- 实体 → 出参 DTO 的转换集中在 `service/convert/`（例：`UserConvert.entityToLoginResponse` /
  `entityToDetailResponse`），出参 DTO 用 lombok `@Builder` + 内部类组织
  （`UserLoginResponseDTO.UserDetailResponseDTO`）
- 业务失败抛 `exception/BusinessException`，由 `common/GlobalExceptionHandler.java` 统一转成 `Result`
  （**它在 `common/` 包，早期在 `exception/`，已搬走**）
- 业务失败**不改变 HTTP 状态码**（仍然 200），由响应体里的 `code` 表达 —— 前端拦截器就是按 `code` 判断的
- 配置类集中在 `config/`（`SecurityConfig`、`JwtConfig`），工具类在 `util/`
- 注释用中文，写「为什么」而不是「是什么」；踩过的坑要留在注释里
