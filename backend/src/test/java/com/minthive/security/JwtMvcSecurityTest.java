package com.minthive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.config.CorsConfig;
import com.minthive.config.CorsProperties;
import com.minthive.config.JwtConfig;
import com.minthive.config.WebMvcConfig;
import com.minthive.entity.User;
import com.minthive.mapper.UserMapper;
import com.minthive.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(JwtMvcSecurityTest.Config.class)
@WebAppConfiguration
class JwtMvcSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtils jwt;
    @Autowired RedisUtil redis;
    @Autowired UserMapper users;
    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setup() {
        UserContext.clear();
        reset(redis, users);
        token = jwt.generateToken(2L, "alice", 2);
        // 多端登录：只要求当前 Token 仍是有效会话
        when(redis.hasLoginToken(eq(2L), anyString())).thenReturn(true);
        when(users.selectById(2L)).thenReturn(UserPrivacyTest.privateUser());
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/user/me", "GET,/api/user/likes", "GET,/api/user/collects",
            "PUT,/api/user/password", "PUT,/api/user/interests", "DELETE,/api/user/cancel",
            "PUT,/api/user/2", "DELETE,/api/post/2", "GET,/api/post/draft",
            "POST,/api/announcement/list", "GET,/api/file/content/private.jpg",
            "GET,/api/auth/info", "POST,/api/auth/logout"})
    void privateMethodsAndPathsNeverAllowAnonymous(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isUnauthorized());
        assertNull(UserContext.get());
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/user/2", "GET,/api/user/2/posts", "GET,/api/post/2",
            "GET,/api/post/feed", "GET,/api/circle/2", "GET,/api/circle/2/posts",
            "GET,/api/search", "GET,/api/search/user", "GET,/api/announcement/latest",
            "GET,/api/config/banners", "POST,/api/auth/login", "POST,/api/auth/login/sms",
            "POST,/api/auth/register", "POST,/api/auth/sms", "POST,/api/auth/reset"})
    void declaredPublicEndpointsAllowAnonymous(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isOk());
        assertNull(UserContext.get());
    }

    @Test
    void publicRequestWithTokenReceivesVerifiedIdentityAndThenClearsContext() throws Exception {
        mvc.perform(get("/api/user/2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("userId").value(2));
        assertNull(UserContext.get());
    }

    @Test
    void revokedTokenOnPublicEndpointCannotBecomeAnonymous() throws Exception {
        when(redis.hasLoginToken(eq(2L), anyString())).thenReturn(false);
        mvc.perform(get("/api/user/2").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertNull(UserContext.get());
    }

    @Test
    void malformedCredentialsOnPublicEndpointsAreRejected() throws Exception {
        for (String authorization : new String[]{"Basic ignored", "Bearer ", "Bearer not-a-jwt", ""}) {
            mvc.perform(get("/api/post/2").header("Authorization", authorization))
                    .andExpect(status().isUnauthorized());
            assertNull(UserContext.get());
        }
    }

    @Test
    void authorizationUsesDatabaseRoleRatherThanOldAdminClaim() throws Exception {
        User current = UserPrivacyTest.privateUser();
        current.setRole(0);
        when(users.selectById(2L)).thenReturn(current);
        mvc.perform(get("/api/admin/user/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        assertNull(UserContext.get());
        mvc.perform(get("/api/user/2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value(0));
    }

    @ParameterizedTest
    @CsvSource({"0,0,0", "1,1,0", "1,2,0", "1,0,1"})
    void bannedCancelledOrDeletedUsersCannotUseCachedTokens(int status, int cancellation, int deleted) throws Exception {
        User current = UserPrivacyTest.privateUser();
        current.setStatus(status);
        current.setCancelStatus(cancellation);
        current.setDeleted(deleted);
        when(users.selectById(2L)).thenReturn(current);
        mvc.perform(get("/api/user/2").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertNull(UserContext.get());
        verify(redis).deleteLoginToken(2L);
    }

    @Test
    void missingDatabaseUserCannotUseCachedToken() throws Exception {
        when(users.selectById(2L)).thenReturn(null);
        mvc.perform(get("/api/admin/user/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertNull(UserContext.get());
    }

    @Test
    void anotherDeviceSessionDoesNotInvalidateThisToken() throws Exception {
        // 多端登录：其他设备的登录不再踢掉当前设备的 Token
        when(redis.hasLoginToken(eq(2L), anyString())).thenReturn(true);
        mvc.perform(get("/api/post/2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertNull(UserContext.get());
    }

    @Test
    void corsOnlyAllowsConfiguredOriginsAndNeverSendsCredentials() throws Exception {
        // 未列入白名单的来源直接被拒绝
        mvc.perform(options("/api/user/me").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        // 白名单来源可以通过预检，但不携带凭证
        mvc.perform(options("/api/user/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        assertNull(UserContext.get());
    }

    @Test
    void eachLoginIssuesDistinctTokenSoRevokedTokensCannotReturnAfterQuickRelogin() {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) tokens.add(jwt.generateToken(2L, "alice", 2));
        assertEquals(20, tokens.size());
    }

    @Test
    void validAdminRetainsAccessAndContextNeverSurvivesRequests() throws Exception {
        mvc.perform(get("/api/admin/user/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("role").value(2));
        assertNull(UserContext.get());
        UserContext.set(new LoginUser(99L, "stale", 2));
        mvc.perform(get("/api/user/me")).andExpect(status().isUnauthorized());
        assertNull(UserContext.get());
        UserContext.set(new LoginUser(99L, "stale", 2));
        mvc.perform(get("/api/post/2")).andExpect(status().isOk()).andExpect(jsonPath("userId").isEmpty());
        assertNull(UserContext.get());
    }

    @Test
    void expiredSignedTokenCannotBecomeAnonymous() throws Exception {
        String expired = io.jsonwebtoken.Jwts.builder().claim("userId", 2L)
                .expiration(new java.util.Date(System.currentTimeMillis() - 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-only-signing-secret-32-bytes-or-more".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
        mvc.perform(get("/api/post/2").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        assertNull(UserContext.get());
    }

    @Test
    void asyncHandoffClearsRequestThreadIdentity() throws Exception {
        JwtInterceptor interceptor = context.getBean(JwtInterceptor.class);
        UserContext.set(new LoginUser(2L, "alice", 2));
        interceptor.afterConcurrentHandlingStarted(new org.springframework.mock.web.MockHttpServletRequest(),
                new org.springframework.mock.web.MockHttpServletResponse(), new Object());
        assertNull(UserContext.get());
    }

    @Configuration
    @EnableWebMvc
    @Import({WebMvcConfig.class, CorsConfig.class, JwtInterceptor.class, AdminAuthInterceptor.class, JwtUtils.class})
    static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean RedisUtil redisUtil() { return mock(RedisUtil.class); }
        @Bean UserMapper users() { return mock(UserMapper.class); }
        @Bean CorsProperties corsProperties() {
            CorsProperties properties = new CorsProperties();
            properties.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
            return properties;
        }
        @Bean ProbeController probe() { return new ProbeController(); }
        @Bean JwtConfig jwtConfig() {
            JwtConfig config = new JwtConfig();
            config.setHeader("Authorization");
            config.setPrefix("Bearer ");
            config.setSecret("test-only-signing-secret-32-bytes-or-more");
            config.setExpire(3600L);
            return config;
        }
    }

    @RestController
    static class ProbeController {
        @RequestMapping("/api/**")
        Map<String, Object> probe() {
            Map<String, Object> data = new LinkedHashMap<>();
            LoginUser user = UserContext.get();
            data.put("userId", user == null ? null : user.getUserId());
            data.put("role", user == null ? null : user.getRole());
            return data;
        }
    }
}
