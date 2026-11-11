package com.minthive.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.RedisConstants;
import com.minthive.security.JwtUtils;
import com.minthive.util.RedisUtil;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 在线会话集合: userId -> 该用户全部设备的 Session 集合（多端登录） */
    private static final ConcurrentHashMap<Long, Set<Session>> SESSIONS = new ConcurrentHashMap<>();

    /** userId -> 在线标记(用于多实例场景，本实例使用本地 Map) */
    private static final ConcurrentHashMap<Long, Boolean> ONLINE_MAP = new ConcurrentHashMap<>();

    private static JwtUtils jwtUtils;

    private static StringRedisTemplate stringRedisTemplate;

    private static ObjectMapper objectMapper;

    private static RedisUtil redisUtil;

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
     * 注入 RedisUtil
     *
     * @param redisUtil Redis 工具（用于确认连接携带的 Token 仍为有效登录态）
     */
    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        WebSocketServer.redisUtil = redisUtil;
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
            // 已登出/被撤销的 Token 不允许再建立长连接
            if (userId == null || redisUtil == null || !redisUtil.hasLoginToken(userId, token)) {
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
        // 同一用户可同时存在多个设备连接，全部保留
        SESSIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        ONLINE_MAP.put(userId, true);
        // Redis 记录在线状态
        stringRedisTemplate.opsForValue().set(RedisConstants.WS_ONLINE_PREFIX + userId, "1");
        log.info("用户 {} WebSocket 连接建立，当前在线连接数: {}", userId, onlineCount());
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
            removeSession(userId, session);
            log.info("用户 {} WebSocket 连接关闭，当前在线连接数: {}", userId, onlineCount());
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
                .filter(e -> e.getValue().contains(session))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (userId != null) {
            removeSession(userId, session);
            log.warn("用户 {} WebSocket 异常会话已清理，当前在线连接数: {}", userId, onlineCount());
        }
        try {
            if (session.isOpen()) session.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * 移除用户的单个设备连接，并在该用户无剩余连接时清理在线标记
     *
     * @param userId  用户ID
     * @param session 待移除的会话
     * @return 无返回值
     */
    private static void removeSession(Long userId, Session session) {
        Set<Session> sessions = SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty() && SESSIONS.remove(userId, sessions)) {
            ONLINE_MAP.remove(userId);
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete(RedisConstants.WS_ONLINE_PREFIX + userId);
            }
        }
    }

    /**
     * 向单个会话发送文本消息
     *
     * @param session 目标会话
     * @param text    已序列化的消息体
     * @return 无返回值
     * @note 同一会话的并发发送通过 session 锁串行化，避免 BasicRemote 并发写异常
     */
    private static void sendText(Session session, String text) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(text);
            }
        } catch (Exception e) {
            log.error("推送消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 向指定用户推送消息
     *
     * @param userId  目标用户ID
     * @param message 消息体
     */
    public static void sendToUser(Long userId, WsMessage message) {
        Set<Session> sessions = SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String text = objectMapper.writeValueAsString(message);
            // 多端登录：推送到该用户全部在线设备
            sessions.forEach(session -> sendText(session, text));
        } catch (Exception e) {
            log.error("推送消息失败: userId={}", userId, e);
        }
    }

    /**
     * 全员广播
     *
     * @param message 消息体
     */
    public static void broadcast(WsMessage message) {
        try {
            String text = objectMapper.writeValueAsString(message);
            SESSIONS.forEach((userId, sessions) -> sessions.forEach(session -> sendText(session, text)));
        } catch (Exception e) {
            log.error("广播消息失败", e);
        }
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
        return SESSIONS.values().stream().mapToInt(Set::size).sum();
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
