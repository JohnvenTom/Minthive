package com.minthive.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.RedisConstants;
import com.minthive.security.JwtUtils;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 服务端
 *
 * <p>功能描述：维护在线用户会话，提供消息推送能力，支持评论/私信/通知实时推送</p>
 * <p>注意事项：连接时携带 token 校验并绑定 userId；禁止使用轮询</p>
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{token}")
public class WebSocketServer {

    /** 在线会话集合: userId -> Session */
    private static final ConcurrentHashMap<Long, Session> SESSIONS = new ConcurrentHashMap<>();

    /** userId -> 在线标记(用于多实例场景，本实例使用本地 Map) */
    private static final ConcurrentHashMap<Long, Boolean> ONLINE_MAP = new ConcurrentHashMap<>();

    private static JwtUtils jwtUtils;

    private static StringRedisTemplate stringRedisTemplate;

    private static ObjectMapper objectMapper;

    /**
     * 注入 JwtUtils(静态字段注入)
     *
     * @param jwtUtils JWT 工具
     */
    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        WebSocketServer.jwtUtils = jwtUtils;
    }

    /**
     * 注入 StringRedisTemplate
     *
     * @param stringRedisTemplate Redis 模板
     */
    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        WebSocketServer.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 注入 ObjectMapper
     *
     * @param objectMapper JSON 工具
     */
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        WebSocketServer.objectMapper = objectMapper;
    }

    /**
     * 连接建立回调：校验 token 并绑定 userId
     *
     * @param session 会话
     * @param token   JWT Token
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        Long userId;
        try {
            userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                session.close();
                return;
            }
        } catch (Exception e) {
            log.warn("WebSocket 连接 token 校验失败: {}", e.getMessage());
            try {
                session.close();
            } catch (IOException ignored) {
            }
            return;
        }
        SESSIONS.put(userId, session);
        ONLINE_MAP.put(userId, true);
        // Redis 记录在线状态
        stringRedisTemplate.opsForValue().set(RedisConstants.WS_ONLINE_PREFIX + userId, "1");
        log.info("用户 {} WebSocket 连接建立，当前在线人数: {}", userId, SESSIONS.size());
    }

    /**
     * 连接关闭回调
     *
     * @param session 会话
     * @param token   JWT Token
     */
    @OnClose
    public void onClose(Session session, @PathParam("token") String token) {
        Long userId = resolveUserId(token);
        if (userId != null) {
            SESSIONS.remove(userId);
            ONLINE_MAP.remove(userId);
            stringRedisTemplate.delete(RedisConstants.WS_ONLINE_PREFIX + userId);
            log.info("用户 {} WebSocket 连接关闭，当前在线人数: {}", userId, SESSIONS.size());
        }
    }

    /**
     * 收到消息回调
     *
     * @param message 消息内容
     * @param session 会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到 WebSocket 消息: {}", message);
    }

    /**
     * 异常回调
     *
     * @param session 会话
     * @param error   异常
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.warn("WebSocket 连接异常，准备清理会话: {}", error.getMessage());
        Long userId = SESSIONS.entrySet().stream()
                .filter(e -> e.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (userId != null) {
            SESSIONS.remove(userId);
            ONLINE_MAP.remove(userId);
            stringRedisTemplate.delete(RedisConstants.WS_ONLINE_PREFIX + userId);
            log.warn("用户 {} WebSocket 会话已清理，当前在线人数: {}", userId, SESSIONS.size());
        }
        try {
            if (session.isOpen()) session.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * 向指定用户推送消息
     *
     * @param userId  目标用户ID
     * @param message 消息体
     */
    public static void sendToUser(Long userId, WsMessage message) {
        Session session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(message));
            } catch (Exception e) {
                log.error("推送消息失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 全员广播
     *
     * @param message 消息体
     */
    public static void broadcast(WsMessage message) {
        SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(objectMapper.writeValueAsString(message));
                } catch (Exception e) {
                    log.error("广播消息失败: userId={}", userId, e);
                }
            }
        });
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return true 在线
     */
    public static boolean isOnline(Long userId) {
        return ONLINE_MAP.containsKey(userId);
    }

    /**
     * 获取当前在线人数
     *
     * @return 在线人数
     */
    public static int onlineCount() {
        return SESSIONS.size();
    }

    /**
     * 从 token 解析用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    private Long resolveUserId(String token) {
        try {
            return jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}
