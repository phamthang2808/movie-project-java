package com.example.thangcachep.movie_project_be.websocket;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity;
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
import java.time.LocalDateTime;
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
        // Trích xuất token từ query string hoặc headers
        String token = extractToken(session);
        if (token == null) {
            try {
                session.close(CloseStatus.BAD_DATA.withReason("No token provided"));
            } catch (IOException e) {
            }
            return;
        }

        // Xác thực user
        UserInfo user = authenticateUser(token);
        if (user == null) {
            try {
                session.close(CloseStatus.BAD_DATA.withReason("Invalid token"));
            } catch (IOException e) {
            }
            return;
        }

        sessionToUser.put(session.getId(), user);
        sendMessage(session, "connection", Map.of("connected", true));
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
                case "change-episode":
                    handleChangeEpisode(session, roomId, payload);
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
                    break;
            }
        } catch (Exception e) {
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
        if (roomId == null || roomId.isEmpty()) {
            return;
        }

        // Xác minh room tồn tại (chỉ check exists, không cần load full data)
        boolean roomExists = roomRepository.findActiveRoomById(roomId).isPresent();
        if (!roomExists) {
            return;
        }

        // Xóa khỏi room cũ nếu có
        String previousRoomId = sessionToRoom.get(session.getId());
        if (previousRoomId != null && !previousRoomId.equals(roomId)) {
            handleLeaveRoom(session, previousRoomId);
        }

        // Thêm vào room mới
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);

        UserInfo user = sessionToUser.get(session.getId());
        if (user == null) {
            return;
        }

        // Thêm user vào room trong database
        try {
            watchTogetherService.addUserToRoom(roomId, user.getId());
        } catch (Exception e) {
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

        // Gửi thông tin creator để frontend biết ai là host
        Optional<com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity> roomOpt =
                roomRepository.findActiveRoomById(roomId);
        if (roomOpt.isPresent()) {
            com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity room = roomOpt.get();
            if (room.getCreatedBy() != null) {
                sendMessage(session, "room-info", Map.of(
                        "roomId", roomId,
                        "creatorId", room.getCreatedBy().getId(),
                        "isHost", room.getCreatedBy().getId().equals(user.getId())
                ));
            }
            // Update lastActivityAt khi có user join
            updateRoomActivity(roomId);
        }
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
            }

            broadcastToRoom(roomId, null, "user-left", Map.of(
                    "userId", user.getId()
            ));
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
        if (roomId == null || !data.containsKey("message")) {
            return;
        }

        String message = (String) data.get("message");
        UserInfo user = sessionToUser.get(session.getId());

        if (user == null) {
            return;
        }

        // Kiểm tra xem session đã join room chưa
        String currentRoomId = sessionToRoom.get(session.getId());
        if (currentRoomId == null || !currentRoomId.equals(roomId)) {
            // Tự động join room
            handleJoinRoom(session, roomId, data);
            // Kiểm tra lại sau khi join
            currentRoomId = sessionToRoom.get(session.getId());
            if (currentRoomId == null || !currentRoomId.equals(roomId)) {
                return;
            }
        }

        Map<String, Object> messageData = Map.of(
                "id", System.currentTimeMillis(),
                "userId", user.getId(),
                "username", user.getUsername(),
                "message", message,
                "timestamp", new Date()
        );

        broadcastToRoom(roomId, null, "chat-message", Map.of("message", messageData));

        // Update lastActivityAt khi có chat message
        updateRoomActivity(roomId);
    }

    /**
     * Xử lý đồng bộ playback (thời gian phát, trạng thái play/pause)
     * - Chỉ người tạo phòng (creator/host) mới được gửi sync
     * - Kiểm tra roomId hợp lệ và user có phải creator không
     * - Trích xuất currentTime và isPlaying từ data
     * - Broadcast playback state đến tất cả users khác trong room (không gửi lại cho người gửi)
     */
    private void handleSyncPlayback(WebSocketSession session, String roomId, Map<String, Object> data) {
        if (roomId == null) {
            return;
        }

        // Kiểm tra user có phải là creator của room không
        UserInfo user = sessionToUser.get(session.getId());
        if (user == null) {
            return;
        }

        // Lấy thông tin room từ database để kiểm tra creator
        Optional<com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity> roomOpt =
                roomRepository.findActiveRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return;
        }

        com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity room = roomOpt.get();
        if (room.getCreatedBy() == null || !room.getCreatedBy().getId().equals(user.getId())) {
            // Không phải creator, không cho phép sync
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

        // Update lastActivityAt khi có sync playback
        updateRoomActivity(roomId);
    }

    /**
     * Xử lý thay đổi tập phim (chỉ host mới được phép)
     * - Kiểm tra user có phải là creator của room không
     * - Broadcast episode change đến tất cả users khác trong room
     * - Kèm theo playback state nếu có
     */
    private void handleChangeEpisode(WebSocketSession session, String roomId, Map<String, Object> data) {
        if (roomId == null) {
            return;
        }

        UserInfo user = sessionToUser.get(session.getId());
        if (user == null) {
            return;
        }

        Optional<com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity> roomOpt =
                roomRepository.findActiveRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return;
        }

        com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity room = roomOpt.get();
        if (room.getCreatedBy() == null || !room.getCreatedBy().getId().equals(user.getId())) {
            return;
        }

        Map<String, Object> episodeData = new HashMap<>();
        if (data.containsKey("episodeId")) {
            episodeData.put("episodeId", data.get("episodeId"));
        }
        if (data.containsKey("episodeNumber")) {
            episodeData.put("episodeNumber", data.get("episodeNumber"));
        }
        if (data.containsKey("currentTime")) {
            episodeData.put("currentTime", data.get("currentTime"));
        }
        if (data.containsKey("isPlaying")) {
            episodeData.put("isPlaying", data.get("isPlaying"));
        }

        broadcastToRoom(roomId, session, "episode-change", episodeData);

        updateRoomActivity(roomId);
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
    }

    /**
     * Xử lý request lấy danh sách messages trong room
     * - Kiểm tra roomId hợp lệ
     * - Hiện tại trả về danh sách rỗng vì messages chưa được lưu trữ
     * - Nếu muốn lưu trữ messages, cần query database ở đây
     */
    private void handleGetRoomMessages(WebSocketSession session, String roomId) {
        if (roomId == null) {
            return;
        }

        // Hiện tại trả về danh sách rỗng vì messages chưa được lưu trữ
        // Nếu muốn lưu trữ messages, cần query database ở đây
        List<Map<String, Object>> messages = new ArrayList<>();

        sendMessage(session, "get-room-messages", Map.of("messages", messages));
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

        Map<String, Object> response = new HashMap<>();
        response.put("event", "test-connection-response");
        response.put("testId", testId);
        response.put("roomId", roomId);
        response.put("timestamp", new Date().toInstant().toString());
        response.put("serverTime", System.currentTimeMillis());

        sendMessage(session, "test-connection-response", response);
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
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null) {
            return;
        }

        Map<String, Object> message = Map.of("event", event, "data", data);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }

        for (WebSocketSession session : roomSessions) {
            if (session == exclude) {
                continue;
            }

            if (!session.isOpen()) {
                continue;
            }

            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
            }
        }
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
        String roomId = sessionToRoom.remove(session.getId());

        if (roomId != null) {
            handleLeaveRoom(session, roomId);
        }
        sessionToUser.remove(session.getId());
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
                return null;
            }

            UserEntity user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return null;
            }

            // Xác minh token hợp lệ
            if (!jwtService.validateToken(token, user)) {
                return null;
            }

            return new UserInfo(user.getId(), user.getName());
        } catch (ExpiredJwtException e) {
            return null;
        } catch (JwtException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cập nhật lastActivityAt của room khi có activity
     * - Gọi khi có user join, chat message, hoặc sync playback
     */
    private void updateRoomActivity(String roomId) {
        try {
            Optional<WatchTogetherRoomEntity> roomOpt =
                    roomRepository.findActiveRoomById(roomId);
            if (roomOpt.isPresent()) {
                WatchTogetherRoomEntity room = roomOpt.get();
                room.setLastActivityAt(LocalDateTime.now());
                roomRepository.save(room);
            }
        } catch (Exception e) {
            // Ignore errors khi update activity
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

