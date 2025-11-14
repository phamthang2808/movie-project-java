package com.example.thangcachep.movie_project_be.websocket;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchTogetherRoomRepository;
import com.example.thangcachep.movie_project_be.services.IWatchTogetherService;
import com.example.thangcachep.movie_project_be.services.impl.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchTogetherHandler extends TextWebSocketHandler {

    // Map roomId -> Set of WebSocket sessions (Danh sách các session trong mỗi room)
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // Map sessionId -> roomId (Mapping session với room mà nó đang tham gia)
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    // Map sessionId -> user info (Mapping session với thông tin user)
    private final Map<String, UserInfo> sessionToUser = new ConcurrentHashMap<>();

    private final IWatchTogetherService watchTogetherService;
    private final UserRepository userRepository;
    private final WatchTogetherRoomRepository roomRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Xử lý khi WebSocket connection được thiết lập
     * - Trích xuất token từ query string
     * - Xác thực user
     * - Lưu thông tin user vào sessionToUser map
     * - Gửi thông báo kết nối thành công về client
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket đã kết nối: {}", session.getId());

        // Trích xuất token từ query string hoặc headers
        String token = extractToken(session);
        if (token == null) {
            try {
                session.close(CloseStatus.BAD_DATA.withReason("No token provided"));
            } catch (IOException e) {
                log.error("Lỗi khi đóng session", e);
            }
            return;
        }

        // Xác thực user
        UserInfo user = authenticateUser(token);
        if (user == null) {
            try {
                session.close(CloseStatus.BAD_DATA.withReason("Invalid token"));
            } catch (IOException e) {
                log.error("Lỗi khi đóng session", e);
            }
            return;
        }

        sessionToUser.put(session.getId(), user);
        sendMessage(session, "connection", Map.of("connected", true));
        log.info("User đã được xác thực: {} ({})", user.getUsername(), user.getId());
    }

    /**
     * Xử lý message text từ client
     * - Parse JSON payload
     * - Phân loại event và gọi handler tương ứng
     * - Các event hỗ trợ: join-room, leave-room, chat-message, sync-playback, get-room-users, get-room-messages, test-connection
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String event = (String) payload.get("event");
            String roomId = (String) payload.get("roomId");

            log.debug("Nhận được event: {} cho room: {} từ session: {}", event, roomId, session.getId());

            switch (event) {
                case "join-room":
                    handleJoinRoom(session, roomId, payload);
                    break;
                case "leave-room":
                    handleLeaveRoom(session, roomId);
                    break;
                case "chat-message":
                    handleChatMessage(session, roomId, payload);
                    break;
                case "sync-playback":
                    handleSyncPlayback(session, roomId, payload);
                    break;
                case "get-room-users":
                    handleGetRoomUsers(session, roomId);
                    break;
                case "get-room-messages":
                    handleGetRoomMessages(session, roomId);
                    break;
                case "test-connection":
                    handleTestConnection(session, payload);
                    break;
                default:
                    log.warn("Event không xác định: {}", event);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý message", e);
        }
    }

    /**
     * Xử lý khi user join vào một room
     * - Kiểm tra roomId hợp lệ
     * - Xác minh room tồn tại trong database
     * - Xóa user khỏi room cũ nếu có
     * - Thêm session vào room mới
     * - Thêm user vào room trong database
     * - Thông báo cho các user khác trong room
     * - Gửi danh sách users hiện tại trong room cho user mới
     */
    private void handleJoinRoom(WebSocketSession session, String roomId, Map<String, Object> data) {
        log.info("handleJoinRoom được gọi - session: {}, roomId: {}", session.getId(), roomId);

        if (roomId == null || roomId.isEmpty()) {
            log.warn("roomId không hợp lệ");
            return;
        }

        // Xác minh room tồn tại (chỉ check exists, không cần load full data)
        boolean roomExists = roomRepository.findActiveRoomById(roomId).isPresent();
        if (!roomExists) {
            log.error("Không tìm thấy room: {}", roomId);
            return;
        }
        log.debug("Room {} tồn tại trong database", roomId);

        // Xóa khỏi room cũ nếu có
        String previousRoomId = sessionToRoom.get(session.getId());
        if (previousRoomId != null && !previousRoomId.equals(roomId)) {
            log.info("Session {} đang ở room cũ: {}, chuyển sang room mới: {}",
                    session.getId(), previousRoomId, roomId);
            handleLeaveRoom(session, previousRoomId);
        }

        // Thêm vào room mới
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);

        log.info("Đã thêm session {} vào room {}. Tổng số sessions trong room: {}",
                session.getId(), roomId, rooms.get(roomId).size());
        log.debug("Danh sách rooms hiện có: {}", rooms.keySet());

        UserInfo user = sessionToUser.get(session.getId());
        if (user == null) {
            log.error("Không tìm thấy user info cho session: {}", session.getId());
            return;
        }

        // Thêm user vào room trong database
        try {
            watchTogetherService.addUserToRoom(roomId, user.getId());
            log.debug("Đã thêm user {} vào room {} trong database", user.getId(), roomId);
        } catch (Exception e) {
            log.error("Lỗi khi thêm user vào room trong database", e);
        }

        // Thông báo cho các user khác
        broadcastToRoom(roomId, session, "user-joined", Map.of(
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername()
                )
        ));

        // Gửi danh sách users hiện tại trong room cho user mới
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        List<Map<String, Object>> users = new ArrayList<>();
        if (roomSessions != null) {
            for (WebSocketSession s : roomSessions) {
                UserInfo u = sessionToUser.get(s.getId());
                if (u != null) {
                    users.add(Map.of(
                            "id", u.getId(),
                            "username", u.getUsername()
                    ));
                }
            }
        }

        sendMessage(session, "room-users", Map.of("users", users));
        log.info("User {} đã join room {}. Tổng số users trong room: {}",
                user.getUsername(), roomId, users.size());
    }

    /**
     * Xử lý khi user rời khỏi room
     * - Xóa session khỏi room
     * - Xóa mapping session -> room
     * - Xóa user khỏi room trong database
     * - Thông báo cho các user khác trong room
     */
    private void handleLeaveRoom(WebSocketSession session, String roomId) {
        if (roomId == null) {
            return;
        }

        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            roomSessions.remove(session);
            if (roomSessions.isEmpty()) {
                rooms.remove(roomId);
            }
        }

        sessionToRoom.remove(session.getId());

        UserInfo user = sessionToUser.get(session.getId());
        if (user != null) {
            // Xóa user khỏi room trong database
            try {
                watchTogetherService.removeUserFromRoom(roomId, user.getId());
            } catch (Exception e) {
                log.error("Lỗi khi xóa user khỏi room trong database", e);
            }

            broadcastToRoom(roomId, null, "user-left", Map.of(
                    "userId", user.getId()
            ));
            log.info("User {} đã rời khỏi room {}", user.getUsername(), roomId);
        }
    }

    /**
     * Xử lý chat message từ client
     * - Kiểm tra roomId và message hợp lệ
     * - Tự động join room nếu session chưa join
     * - Lấy thông tin user từ session
     * - Tạo message data với id, userId, username, message, timestamp
     * - Broadcast message đến tất cả users trong room (bao gồm cả người gửi)
     */
    private void handleChatMessage(WebSocketSession session, String roomId, Map<String, Object> data) {
        log.info("handleChatMessage được gọi - session: {}, roomId: {}, data keys: {}",
                session.getId(), roomId, data != null ? data.keySet() : "null");

        if (roomId == null || !data.containsKey("message")) {
            log.warn("handleChatMessage: Request không hợp lệ - roomId: {}, hasMessage: {}",
                    roomId, data != null && data.containsKey("message"));
            return;
        }

        String message = (String) data.get("message");
        UserInfo user = sessionToUser.get(session.getId());

        if (user == null) {
            log.warn("handleChatMessage: Không tìm thấy user cho session: {}", session.getId());
            return;
        }

        // Kiểm tra xem session đã join room chưa
        String currentRoomId = sessionToRoom.get(session.getId());
        if (currentRoomId == null || !currentRoomId.equals(roomId)) {
            log.warn("handleChatMessage: Session {} chưa join room {}. Đang tự động join...",
                    session.getId(), roomId);
            // Tự động join room
            handleJoinRoom(session, roomId, data);
            // Kiểm tra lại sau khi join
            currentRoomId = sessionToRoom.get(session.getId());
            if (currentRoomId == null || !currentRoomId.equals(roomId)) {
                log.error("handleChatMessage: Không thể join room {} cho session {}",
                        roomId, session.getId());
                return;
            }
        }

        log.info("handleChatMessage: Đang xử lý message từ user {} ({}): {}",
                user.getUsername(), user.getId(), message);

        Map<String, Object> messageData = Map.of(
                "id", System.currentTimeMillis(),
                "userId", user.getId(),
                "username", user.getUsername(),
                "message", message,
                "timestamp", new Date()
        );

        log.info("handleChatMessage: Đang broadcast đến room: {}", roomId);
        broadcastToRoom(roomId, null, "chat-message", Map.of("message", messageData));
        log.info("handleChatMessage: Broadcast hoàn tất");
    }

    /**
     * Xử lý đồng bộ playback (thời gian phát, trạng thái play/pause)
     * - Kiểm tra roomId hợp lệ
     * - Trích xuất currentTime và isPlaying từ data
     * - Broadcast playback state đến tất cả users khác trong room (không gửi lại cho người gửi)
     */
    private void handleSyncPlayback(WebSocketSession session, String roomId, Map<String, Object> data) {
        if (roomId == null) {
            return;
        }

        // Broadcast playback state đến tất cả users khác trong room (không gửi lại cho người gửi)
        Map<String, Object> playbackState = new HashMap<>();
        if (data.containsKey("currentTime")) {
            playbackState.put("currentTime", data.get("currentTime"));
        }
        if (data.containsKey("isPlaying")) {
            playbackState.put("isPlaying", data.get("isPlaying"));
        }

        broadcastToRoom(roomId, session, "playback-sync", playbackState);
    }

    /**
     * Xử lý request lấy danh sách users trong room
     * - Kiểm tra roomId hợp lệ
     * - Lấy tất cả sessions trong room
     * - Thu thập thông tin user từ các sessions
     * - Gửi danh sách users về cho session yêu cầu
     */
    private void handleGetRoomUsers(WebSocketSession session, String roomId) {
        if (roomId == null) {
            log.warn("get-room-users: roomId không hợp lệ");
            return;
        }

        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        List<Map<String, Object>> users = new ArrayList<>();

        if (roomSessions != null) {
            for (WebSocketSession s : roomSessions) {
                UserInfo u = sessionToUser.get(s.getId());
                if (u != null) {
                    users.add(Map.of(
                            "id", u.getId(),
                            "username", u.getUsername()
                    ));
                }
            }
        }

        sendMessage(session, "get-room-users", Map.of("users", users));
        log.debug("Đã gửi danh sách users trong room đến session {}: {} users", session.getId(), users.size());
    }

    /**
     * Xử lý request lấy danh sách messages trong room
     * - Kiểm tra roomId hợp lệ
     * - Hiện tại trả về danh sách rỗng vì messages chưa được lưu trữ
     * - Nếu muốn lưu trữ messages, cần query database ở đây
     */
    private void handleGetRoomMessages(WebSocketSession session, String roomId) {
        if (roomId == null) {
            log.warn("get-room-messages: roomId không hợp lệ");
            return;
        }

        // Hiện tại trả về danh sách rỗng vì messages chưa được lưu trữ
        // Nếu muốn lưu trữ messages, cần query database ở đây
        List<Map<String, Object>> messages = new ArrayList<>();

        sendMessage(session, "get-room-messages", Map.of("messages", messages));
        log.debug("Đã gửi danh sách messages trong room đến session {}: {} messages", session.getId(), messages.size());
    }

    /**
     * Xử lý test connection để kiểm tra kết nối 2 chiều
     * - Nhận testId và roomId từ client
     * - Tạo response với testId, roomId, timestamp, serverTime
     * - Gửi response về cho session yêu cầu
     */
    private void handleTestConnection(WebSocketSession session, Map<String, Object> data) {
        String testId = (String) data.get("testId");
        String roomId = (String) data.get("roomId");

        log.info("Nhận được test connection từ session {}: testId={}, roomId={}",
                session.getId(), testId, roomId);

        Map<String, Object> response = new HashMap<>();
        response.put("event", "test-connection-response");
        response.put("testId", testId);
        response.put("roomId", roomId);
        response.put("timestamp", new Date().toInstant().toString());
        response.put("serverTime", System.currentTimeMillis());

        sendMessage(session, "test-connection-response", response);
        log.info("Đã gửi test connection response đến session {}", session.getId());
    }

    /**
     * Broadcast message đến tất cả sessions trong room
     * - Lấy danh sách sessions trong room
     * - Serialize message thành JSON
     * - Gửi message đến tất cả sessions (trừ session exclude nếu có)
     * - Bỏ qua các session đã đóng
     * - Log số lượng message đã gửi và bị bỏ qua
     */
    private void broadcastToRoom(String roomId, WebSocketSession exclude,
                                 String event, Map<String, Object> data) {
        log.info("broadcastToRoom được gọi - roomId: {}, event: {}, exclude: {}",
                roomId, event, exclude != null ? exclude.getId() : "null");

        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null) {
            log.warn("broadcastToRoom: Không tìm thấy room {} trong rooms map. Các rooms hiện có: {}",
                    roomId, rooms.keySet());
            return;
        }

        log.info("broadcastToRoom: Room {} có {} sessions", roomId, roomSessions.size());

        Map<String, Object> message = Map.of("event", event, "data", data);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
            log.info("broadcastToRoom: Message JSON: {}", json);
        } catch (Exception e) {
            log.error("broadcastToRoom: Lỗi khi serialize message", e);
            return;
        }

        int sentCount = 0;
        int skippedCount = 0;
        for (WebSocketSession session : roomSessions) {
            if (session == exclude) {
                log.debug("broadcastToRoom: Bỏ qua session bị exclude: {}", session.getId());
                skippedCount++;
                continue;
            }

            if (!session.isOpen()) {
                log.warn("broadcastToRoom: Session {} không mở, bỏ qua", session.getId());
                skippedCount++;
                continue;
            }

            try {
                session.sendMessage(new TextMessage(json));
                sentCount++;
                log.info("broadcastToRoom: Đã gửi message đến session: {}", session.getId());
            } catch (IOException e) {
                log.error("broadcastToRoom: Lỗi khi gửi message đến session: {}", session.getId(), e);
            }
        }

        log.info("broadcastToRoom: Hoàn tất - đã gửi: {}, bỏ qua: {}, tổng sessions: {}",
                sentCount, skippedCount, roomSessions.size());
    }

    /**
     * Gửi message đến một session cụ thể
     * - Tạo message object với event và data
     * - Serialize thành JSON
     * - Gửi qua WebSocket
     */
    private void sendMessage(WebSocketSession session, String event, Map<String, Object> data) {
        try {
            Map<String, Object> message = Map.of("event", event, "data", data);
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Lỗi khi gửi message", e);
        }
    }

    /**
     * Xử lý khi WebSocket connection bị đóng
     * - Xóa session khỏi room (nếu có)
     * - Xóa mapping session -> room
     * - Xóa mapping session -> user
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection đã đóng: {} với status: {}", session.getId(), status);

        String roomId = sessionToRoom.remove(session.getId());
        log.debug("Session {} đang ở room: {}", session.getId(), roomId);
        log.debug("Danh sách rooms trước khi xóa: {}", rooms.keySet());

        if (roomId != null) {
            handleLeaveRoom(session, roomId);
        }
        sessionToUser.remove(session.getId());

        log.debug("Danh sách rooms sau khi xóa: {}", rooms.keySet());
    }

    /**
     * Trích xuất token từ query string của WebSocket URI
     * - Parse query string
     * - Tìm parameter "token"
     * - Trả về token nếu tìm thấy, null nếu không
     */
    private String extractToken(WebSocketSession session) {
        // Thử lấy token từ query string
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    /**
     * Xác thực user từ JWT token
     * - Trích xuất email từ token
     * - Tìm user trong database
     * - Xác minh token hợp lệ
     * - Trả về UserInfo nếu thành công, null nếu thất bại
     */
    private UserInfo authenticateUser(String token) {
        try {
            String email = jwtService.extractUsername(token);
            if (email == null) {
                log.warn("Không thể trích xuất email từ token");
                return null;
            }

            UserEntity user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                log.warn("Không tìm thấy user với email: {}", email);
                return null;
            }

            // Xác minh token hợp lệ
            if (!jwtService.validateToken(token, user)) {
                log.warn("Token không hợp lệ cho user: {}", email);
                return null;
            }

            log.debug("Xác thực thành công cho user: {} ({})", user.getName(), user.getId());
            return new UserInfo(user.getId(), user.getName());
        } catch (ExpiredJwtException e) {
            log.warn("Token đã hết hạn - Expired at: {}, Current time: {}",
                    e.getClaims().getExpiration(), new Date());
            return null;
        } catch (JwtException e) {
            log.warn("Lỗi JWT: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Lỗi không xác định khi xác thực user", e);
            return null;
        }
    }

    /**
     * Inner class để lưu trữ thông tin user
     * - id: ID của user
     * - username: Tên user
     */
    private static class UserInfo {
        private final Long id;
        private final String username;

        public UserInfo(Long id, String username) {
            this.id = id;
            this.username = username;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }
    }
}

