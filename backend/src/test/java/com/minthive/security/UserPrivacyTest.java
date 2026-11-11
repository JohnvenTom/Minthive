package com.minthive.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.entity.User;
import com.minthive.entity.Follow;
import com.minthive.controller.UserController;
import com.minthive.mapper.FollowMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.service.UserService;
import com.minthive.service.InterestVectorService;
import com.minthive.service.impl.FollowServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class UserPrivacyTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    @Mock UserMapper users;
    @Mock FollowMapper follows;
    @Mock InterestVectorService interests;
    @Mock UserService userService;
    @InjectMocks UserController controller;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void passwordIsWriteOnlyAndRemainsAvailableForLogin() throws Exception {
        User user = json.readValue("{\"account\":\"alice\",\"password\":\"bcrypt-hash\"}", User.class);
        assertEquals("bcrypt-hash", user.getPassword());
        JsonNode output = json.valueToTree(user);
        assertFalse(output.has("password"), "No User response may expose password hashes");
        assertEquals("alice", output.get("account").asText());
    }

    @Test
    void followingFollowersAndBothRecommendationBranchesExposeOnlyPublicProfiles() {
        User user = privateUser();
        Follow follow = new Follow();
        follow.setUserId(2L);
        follow.setFollowUserId(2L);
        Page<Follow> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(follow));
        when(follows.selectPage(any(), any())).thenReturn(page);
        when(users.selectBatchIds(any())).thenReturn(List.of(user));
        when(users.selectList(any())).thenReturn(List.of(user));
        when(interests.getVector(1L)).thenReturn(Map.of(), Map.of("music", 1.0));
        when(interests.getVector(2L)).thenReturn(Map.of("music", 1.0));
        FollowServiceImpl service = new FollowServiceImpl(follows, users, interests);

        assertPublic(json.valueToTree(service.getFollowingList(1L, 1, 20).getRecords().get(0)));
        assertPublic(json.valueToTree(service.getFollowersList(1L, null, 1, 20).getRecords().get(0)));
        assertPublic(json.valueToTree(service.recommendUsers(1L).get(0)));
        assertPublic(json.valueToTree(service.recommendUsers(1L).get(0)));
        assertEquals("13800138000", user.getPhone(), "Projection must not mutate the stored user");
    }

    @Test
    void publicProfileIsRestrictedEvenWhenViewingSelfButMeRetainsPrivateFields() throws Exception {
        when(userService.getById(2L)).thenReturn(privateUser());
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        String anonymous = mvc.perform(get("/api/user/2")).andReturn().getResponse().getContentAsString();
        assertPublic(json.readTree(anonymous).get("data"));
        UserContext.set(new LoginUser(2L, "alice", 0));
        String self = mvc.perform(get("/api/user/2")).andReturn().getResponse().getContentAsString();
        assertPublic(json.readTree(self).get("data"));
        String me = mvc.perform(get("/api/user/me")).andReturn().getResponse().getContentAsString();
        JsonNode personal = json.readTree(me).get("data");
        assertEquals("13800138000", personal.get("phone").asText());
        assertTrue(personal.hasNonNull("birthday"));
        assertTrue(personal.hasNonNull("interestTags"));
        assertFalse(personal.has("password"));
    }

    @Test
    void profileUpdatePreservesTheExistingWhitelistAndUsesContextIdentity() throws Exception {
        UserContext.set(new LoginUser(2L, "alice", 0));
        when(userService.update(any(User.class))).thenAnswer(call -> call.getArgument(0));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/user")
                .contentType("application/json")
                .content("{\"id\":99,\"nickname\":\"Updated\",\"role\":2,\"status\":1,\"phone\":\"secret\",\"password\":\"injected\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        var updated = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userService).update(updated.capture());
        assertEquals(2L, updated.getValue().getId());
        assertEquals("Updated", updated.getValue().getNickname());
        assertNull(updated.getValue().getRole());
        assertNull(updated.getValue().getStatus());
        assertNull(updated.getValue().getPhone());
        assertNull(updated.getValue().getPassword());
    }

    static User privateUser() {
        User user = new User();
        user.setId(2L);
        user.setNickname("Alice");
        user.setAvatar("/avatar.png");
        user.setBio("hello");
        user.setAccount("alice");
        user.setPassword("bcrypt-hash");
        user.setPhone("13800138000");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        user.setInterestTags("music");
        user.setAiInterestVector("{\"music\":1}");
        user.setInterestVector(Map.of("music", 1));
        user.setStatus(1);
        user.setCancelStatus(0);
        user.setDeleted(0);
        user.setRole(2);
        return user;
    }

    static void assertPublic(JsonNode profile) {
        assertEquals("Alice", profile.get("nickname").asText());
        for (String field : List.of("password", "phone", "birthday", "account", "interestTags",
                "aiInterestVector", "interestVector", "role", "status", "cancelStatus", "deleted", "lastLoginTime")) {
            assertFalse(profile.has(field), "Public response contains " + field);
        }
    }
}
