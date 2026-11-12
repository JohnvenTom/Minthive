package com.minthive.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.minthive.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 会话生命周期与内容可见性矩阵集成测试
 *
 * <p>覆盖：多端登录/登出隔离、封禁撤销会话、匿名/作者/粉丝/圈成员对帖子的访问</p>
 */
class SessionVisibilityIT extends BaseIntegrationTest {

    @Test
    void multiDeviceLogoutOnlyRevokesCurrentDevice() throws Exception {
        long user = createUser("multi-device", 0);
        String deviceA = login(user);
        String deviceB = login(user);

        assertEquals(200, request("GET", "/api/auth/info", deviceA, null).path("code").asInt());
        assertEquals(200, request("GET", "/api/auth/info", deviceB, null).path("code").asInt());

        assertEquals(200, request("POST", "/api/auth/logout", deviceA, null).path("code").asInt());

        assertEquals(1006, request("GET", "/api/auth/info", deviceA, null).path("code").asInt());
        assertEquals(200, request("GET", "/api/auth/info", deviceB, null).path("code").asInt());
    }

    @Test
    void banRevokesAllDeviceTokens() throws Exception {
        long user = createUser("banned-user", 0);
        String token = login(user);
        jdbcTemplate.update("UPDATE user SET status=0 WHERE id=?", user);

        assertEquals(401, request("GET", "/api/auth/info", token, null).path("code").asInt());
        assertEquals(1006, request("GET", "/api/auth/info", token, null).path("code").asInt());
    }

    @Test
    void visibilityMatrixAllowsOnlyAuthorizedViewers() throws Exception {
        long owner = createUser("owner", 0);
        long follower = createUser("follower", 0);
        long stranger = createUser("stranger", 0);
        long member = createUser("member", 0);
        jdbcTemplate.update("INSERT INTO follow(user_id,follow_user_id) VALUES(?,?)", follower, owner);
        long circleId = createPrivateCircle(owner, member);

        long publicPost = createPost(owner, 0, null, 1);
        long followerPost = createPost(owner, 1, null, 1);
        long selfPost = createPost(owner, 2, null, 1);
        long circlePost = createPost(owner, 0, circleId, 1);

        assertEquals(200, request("GET", "/api/post/" + publicPost, null, null).path("code").asInt());
        assertEquals(1009, request("GET", "/api/post/" + followerPost, null, null).path("code").asInt());
        assertEquals(1009, request("GET", "/api/post/" + selfPost, null, null).path("code").asInt());
        assertEquals(1009, request("GET", "/api/post/" + circlePost, null, null).path("code").asInt());

        String followerToken = login(follower);
        assertEquals(200, request("GET", "/api/post/" + followerPost, followerToken, null).path("code").asInt());
        assertEquals(1009, request("GET", "/api/post/" + selfPost, followerToken, null).path("code").asInt());
        assertEquals(1009, request("GET", "/api/post/" + circlePost, followerToken, null).path("code").asInt());

        String memberToken = login(member);
        assertEquals(200, request("GET", "/api/post/" + circlePost, memberToken, null).path("code").asInt());

        String ownerToken = login(owner);
        assertEquals(200, request("GET", "/api/post/" + selfPost, ownerToken, null).path("code").asInt());

        JsonNode page = request("GET", "/api/post/page?current=1&size=100", null, null);
        assertEquals(200, page.path("code").asInt());
    }
}
