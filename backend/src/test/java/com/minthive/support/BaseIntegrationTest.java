package com.minthive.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.ai.AiContext;
import com.minthive.ai.AiService;
import com.minthive.ai.ImageModerationService;
import com.minthive.util.BcryptUtil;
import com.minthive.util.MinioUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 集成测试基类
 *
 * <p>功能描述：启动真实 MySQL/Redis 容器（MinIO 与 AI 用 Mock），提供用户/帖子 fixture、
 * 登录与请求辅助方法，供 *IT 测试验证安全与业务链路</p>
 * <p>注意事项：</p>
 * <ul>
 *   <li>MySQL 初始化使用 docs/sql/init.sql，Flyway 以 baseline-version=1 补 V2–V4；</li>
 *   <li>容器静态共享，避免每个测试类重复启动；</li>
 *   <li>fixture 直接走 JdbcTemplate，绕开 MyBatis 审计，保证审计断言不被夹具污染。</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class BaseIntegrationTest {

    /** 测试账号统一密码 */
    protected static final String PASSWORD = "test-password-123";

    private static final AtomicInteger USER_SEQ = new AtomicInteger();

    /** MySQL 容器：初始化脚本提供基础表，Flyway 负责增量索引与审计表 */
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("minthive")
            .withUsername("root")
            .withPassword("root")
            .withCopyFileToContainer(MountableFile.forHostPath("docs/sql/init.sql"),
                    "/docker-entrypoint-initdb.d/01-schema.sql");

    /** Redis 容器 */
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        // Windows + Docker Desktop 本地执行时需设置 DOCKER_HOST（例如
        // npipe:////./pipe/dockerDesktopLinuxEngine）；Linux CI 使用默认 unix socket，无需配置
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockBean
    protected MinioUtil minioUtil;

    @MockBean
    protected AiContext aiContext;

    @MockBean
    protected ImageModerationService imageModerationService;

    /**
     * 默认 AI 审核桩：可用且不违规
     */
    @BeforeEach
    void stubAi() {
        when(aiContext.detectContent(any(), any()))
                .thenReturn(new AiService.AiDetectResult(false, "无", "low"));
        when(imageModerationService.moderate(any(), anyString()))
                .thenReturn(new AiService.AiDetectResult(false, "无", "low"));
    }

    /**
     * 创建测试用户
     *
     * @param nickname 昵称
     * @param role     角色
     * @return 用户ID
     */
    protected long createUser(String nickname, int role) {
        String account = "it_" + System.nanoTime() + "_" + USER_SEQ.incrementAndGet();
        String phone = "139" + String.format("%08d", Math.abs(account.hashCode()) % 100000000);
        jdbcTemplate.update("INSERT INTO user(account,password,phone,nickname,status,cancel_status,role,register_time,deleted) "
                        + "VALUES(?,?,?,?,1,0,?,NOW(),0)",
                account, BcryptUtil.encode(PASSWORD), phone, nickname, role);
        return jdbcTemplate.queryForObject("SELECT id FROM user WHERE account=?", Long.class, account);
    }

    /**
     * 获取测试账号（按用户ID反查）
     *
     * @param userId 用户ID
     * @return 账号
     */
    protected String accountOf(long userId) {
        return jdbcTemplate.queryForObject("SELECT account FROM user WHERE id=?", String.class, userId);
    }

    /**
     * 登录并返回 Token
     *
     * @param userId 用户ID
     * @return JWT
     * @throws Exception 请求失败
     */
    protected String login(long userId) throws Exception {
        String account = accountOf(userId);
        String body = objectMapper.writeValueAsString(Map.of("account", account, "password", PASSWORD));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = node.path("data").path("token").asText();
        if (token.isEmpty()) {
            throw new IllegalStateException("登录失败: " + node);
        }
        return token;
    }

    /**
     * 发送 JSON 请求
     *
     * @param method    GET/POST/PUT/DELETE
     * @param path      路径
     * @param token     Token（可为空）
     * @param body      请求体（可为空）
     * @return 解析后的响应 JSON
     * @throws Exception 请求失败
     */
    protected JsonNode request(String method, String path, String token, Object body) throws Exception {
        var builder = switch (method) {
            case "POST" -> post(path);
            case "PUT" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path);
            case "DELETE" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path);
            default -> get(path);
        };
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .content(body instanceof String text ? text : objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    /**
     * 创建帖子（夹具）
     *
     * @param userId     作者
     * @param visibility 可见范围
     * @param circleId   圈子ID（可为空）
     * @param auditStatus 审核状态
     * @return 帖子ID
     */
    protected long createPost(long userId, int visibility, Long circleId, int auditStatus) {
        jdbcTemplate.update("INSERT INTO post(user_id,content,visibility,audit_status,circle_id,ai_generated,deleted,create_time,update_time) "
                        + "VALUES(?,?,?,?,?,0,0,NOW(),NOW())",
                userId, "it-post-" + System.nanoTime(), visibility, auditStatus, circleId);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM post", Long.class);
    }

    /**
     * 创建私密圈子并加入成员
     *
     * @param ownerId 圈主ID
     * @param memberId 成员ID（可为空）
     * @return 圈子ID
     */
    protected long createPrivateCircle(long ownerId, Long memberId) {
        String suffix = String.valueOf(System.nanoTime());
        jdbcTemplate.update("INSERT INTO circle_category(name,sort,status) VALUES(?,0,1)", "it-cat-" + suffix);
        Long categoryId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM circle_category", Long.class);
        jdbcTemplate.update("INSERT INTO circle(owner_id,name,category_id,intro,type,member_count,status,deleted) "
                        + "VALUES(?,?,?,'it-circle',1,1,1,0)",
                ownerId, "it-circle-" + suffix, categoryId);
        Long circleId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM circle", Long.class);
        jdbcTemplate.update("INSERT INTO circle_user(circle_id,user_id,role,audit_status,join_time) "
                + "VALUES(?,?,1,1,NOW())", circleId, ownerId);
        if (memberId != null) {
            jdbcTemplate.update("INSERT INTO circle_user(circle_id,user_id,role,audit_status,join_time) "
                    + "VALUES(?,?,0,1,NOW())", circleId, memberId);
        }
        return circleId;
    }

    /**
     * 统计某路径的审计主记录数
     *
     * @param path 请求路径
     * @return 条数
     */
    protected long auditCount(String path) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE request_path=?", Long.class, path);
        return count == null ? 0 : count;
    }
}
