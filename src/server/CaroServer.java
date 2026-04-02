/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import database.MatchDAO;
import database.UserDAO;
import database.UserDAO.UserInfo;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.ArrayList;
import database.DbConnection; 

import java.sql.Connection;        
import java.sql.PreparedStatement;
import java.sql.ResultSet;        
import java.sql.SQLException;
import java.util.List;

// Đã xóa các import liên quan đến SSL/KeyStore

/**
 *
 * @author Admin
 */
public class CaroServer extends WebSocketServer {
    private UserDAO userDAO = new UserDAO();
    private MatchDAO matchDAO = new MatchDAO(); 
    private Queue<WebSocket> waitingQueue = new LinkedList<>();

    private Map<WebSocket, String> userMap = new HashMap<>();
    private Map<String, GameRoom> roomMap = new HashMap<>();
    private Map<String, String> avatarCache = new HashMap<>(); 
    private GameRequestHandler gameRequestHandler;
    private ProfileHandler profileHandler;

    public CaroServer(int port) {
        super(new InetSocketAddress(port));
        this.gameRequestHandler = new GameRequestHandler(matchDAO, userDAO, roomMap, userMap);
        
        this.profileHandler = new ProfileHandler(userDAO, avatarCache, userMap);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client mới kết nối: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("=== DEBUG onClose ===");
        System.out.println("Client đã thoát: " + conn.getRemoteSocketAddress());

        // 1. Dọn dẹp hàng chờ và cache
        waitingQueue.remove(conn);
        String disconnectedUsername = userMap.get(conn);
        userMap.remove(conn);
        if (disconnectedUsername != null) {
            avatarCache.remove(disconnectedUsername);
        }

        String roomToRemove = null;

        // 2. Tìm phòng chứa user này
        for (GameRoom room : roomMap.values()) {
            if (room.players.contains(conn)) {
                System.out.println("Tìm thấy room: " + room.roomId);

                // TRƯỜNG HỢP 1: Game ĐÃ KẾT THÚC
                if (room.isGameEnded()) {
                    System.out.println("Game đã kết thúc. User " + disconnectedUsername + " rời phòng.");
                    room.players.remove(conn); 
                    if (room.players.isEmpty()) {
                        roomToRemove = room.roomId;
                    }
                    break; 
                }

                // TRƯỜNG HỢP 2: Game ĐANG DIỄN RA -> XỬ THUA
                System.out.println("User " + disconnectedUsername + " thoát khi đang chơi -> XỬ THUA.");
                room.players.remove(conn); 

                if (!room.players.isEmpty()) {
                    WebSocket winnerSocket = room.players.get(0);
                    String winnerUsername = userMap.get(winnerSocket);

                    if (winnerUsername == null) winnerUsername = "Unknown";
                    System.out.println("Người ở lại thắng: " + winnerUsername);

                    // Cập nhật Database
                    if (disconnectedUsername != null) {
                        matchDAO.saveMatch(winnerUsername, disconnectedUsername, room.moveCount);
                        userDAO.updateScore(winnerUsername, 5, "WIN");
                        userDAO.updateScore(disconnectedUsername, -5, "LOSE");
                        System.out.println("--> Đã lưu history (Opponent Disconnected)");
                    }

                    // Gửi thông báo cho người ở lại
                    String winMsg = String.format(
                            "{\"type\": \"GAME_OVER\", \"winner\": \"%s\", \"reason\": \"OPPONENT_DISCONNECTED\"}",
                            winnerUsername
                    );
                    if (winnerSocket.isOpen()) {
                        winnerSocket.send(winMsg);
                    }
                    
                    room.setGameEnded(true); 
                    room.stopTimer();
                }

                if (room.players.isEmpty()) {
                    roomToRemove = room.roomId;
                }
                break;
            }
        }
        
        // 3. Xóa phòng nếu cần
        if (roomToRemove != null) {
            roomMap.remove(roomToRemove);
            System.out.println("Đã xóa phòng trống: " + roomToRemove);
        }
        System.out.println("=== KẾT THÚC onClose ===");
    }


    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.contains("\"password\"") || message.contains("LOGIN") || message.contains("REGISTER")) {
            System.out.println("Nhận từ Web: [LOGIN/REGISTER DATA - ĐÃ ẨN BẢO MẬT]");
        } else {
            System.out.println("Nhận từ Web: " + message);
        }
       
        if (message.contains("\"type\":\"LOGIN\"")) {
            handleLogin(conn, message);
        } else if (message.contains("\"type\":\"REGISTER\"")) {
            handleRegister(conn, message);
        } else if (message.contains("\"type\":\"FIND_MATCH\"")) {
            handleFindMatch(conn, message);
        } else if (message.contains("\"type\":\"CANCEL_FIND_MATCH\"")) {
            waitingQueue.remove(conn);
        } else if (message.contains("\"type\":\"PLAY_WITH_BOT\"")) {
            handlePlayWithBot(conn, message);
        } else if (message.contains("\"type\":\"JOIN_GAME\"")) {
            handleJoinGame(conn, message);
        } else if (message.contains("\"type\":\"MOVE\"")) {
            handleMove(conn, message);
        } else if (message.contains("\"type\":\"SURRENDER\"")) {
            handleSurrender(conn, message);
        } else if (message.contains("\"type\":\"LEAVE_ROOM\"")) {
            handleLeaveRoom(conn, message);
        } else if (message.contains("\"type\":\"GET_HISTORY\"")) {
            handleGetHistory(conn, message);
        } else if (message.contains("\"type\":\"GET_RANKING\"")) {
            handleGetRanking(conn, message);
        } else if (message.contains("\"type\":\"CHAT\"")) {
            handleChat(conn, message);
        } else if (message.contains("\"type\":\"DRAW_REQUEST\"")) {
            handleDrawRequest(conn, message);
        } else if (message.contains("\"type\":\"DRAW_RESPONSE\"")) {
            handleDrawResponse(conn, message);
        }else if (message.contains("\"type\":\"UNDO_REQUEST\"")) {
            handleUndoRequest(conn, message);
        } else if (message.contains("\"type\":\"UNDO_RESPONSE\"")) {
            handleUndoResponse(conn, message);
        } else if (message.contains("\"type\":\"REMATCH_REQUEST\"")) {
            handleRematchRequest(conn, message);
        } else if (message.contains("\"type\":\"REMATCH_RESPONSE\"")) {
            handleRematchResponse(conn, message);
        } else if (message.contains("\"type\":\"CHANGE_AVATAR\"")) {
            handleChangeAvatar(conn, message);
        } else if (message.contains("\"type\":\"GET_AVATAR\"")) {
            handleGetAvatar(conn, message);
        } else if (message.contains("\"type\":\"GET_USER_INFO\"")) {
            String username = extractValue(message, "username");
            profileHandler.handleGetUserInfo(username, conn);
        } else if (message.contains("\"type\":\"CHANGE_PASSWORD\"")) {
             String username = extractValue(message, "username");
             String currentPass = extractValue(message, "currentPassword");
             String newPass = extractValue(message, "newPassword");
             profileHandler.handleChangePassword(username, currentPass, newPass, conn);
        } else if (message.contains("\"type\":\"CHANGE_EMAIL\"")) {
            String username = extractValue(message, "username");
            String newEmail = extractValue(message, "newEmail");
            profileHandler.handleChangeEmail(username, newEmail, conn);
        } else if (message.contains("\"type\":\"GET_ELO_HISTORY\"")) {
            String username = extractValue(message, "username");
            String range = extractValue(message, "range");
            profileHandler.handleGetEloHistory(username, range, conn);
        }else if (message.contains("\"type\":\"CHANGE_USERNAME\"")) {
            String username = extractValue(message, "username");
            String newUsername = extractValue(message, "newUsername");
            profileHandler.handleChangeUsername(username, newUsername, conn);
        }
    }

    private void handleGetHistory(WebSocket conn, String message) {
        String username = extractValue(message, "username");
        System.out.println("Đang lấy lịch sử cho user: " + username);

        String sql = "SELECT * FROM matches WHERE winner_username = ? OR loser_username = ? ORDER BY played_at DESC LIMIT 10";
        StringBuilder jsonList = new StringBuilder("[");

        try (Connection dbConn = DbConnection.getConnection(); 
             PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, username);
            
            ResultSet rs = stmt.executeQuery();
            boolean first = true;

            while (rs.next()) {
                if (!first) {
                    jsonList.append(","); 
                }
                
                String winner = rs.getString("winner_username");
                String loser = rs.getString("loser_username");
                String time = rs.getTimestamp("played_at").toString();
                int moves = rs.getInt("moves_count");
                
                String result;
                String opponent;

                if (username.equals(winner)) {
                    result = "WIN";
                    opponent = loser;
                } else {
                    result = "LOSE";
                    opponent = winner;
                }

                String matchJson = String.format(
                    "{\"opponent\": \"%s\", \"result\": \"%s\", \"time\": \"%s\", \"moves\": %d}",
                    opponent, result, time, moves
                );
                
                jsonList.append(matchJson);
                first = false;
            }
            
            jsonList.append("]");
            String response = "{\"type\": \"HISTORY_DATA\", \"historyList\": " + jsonList.toString() + "}";
            conn.send(response);

        } catch (SQLException e) {
            e.printStackTrace();
            conn.send("{\"type\": \"ERROR\", \"message\": \"Lỗi database khi lấy lịch sử\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMove(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        
        try {
            int row = Integer.parseInt(extractValue(message, "row"));
            int col = Integer.parseInt(extractValue(message, "col"));
            String symbol = extractValue(message, "symbol");
            String username = extractValue(message, "username"); 

            GameRoom room = roomMap.get(roomId);
            if (room != null) {
                int result = room.makeMove(row, col, symbol);
                
                if (result == 0) {
                    System.out.println("Nước đi không hợp lệ từ " + username);
                } else {
                    List<WebSocket> deadSockets = new ArrayList<>();

                    for (WebSocket player : room.players) {
                        try {
                            if (player.isOpen()) {
                                player.send(message);
                            } else {
                                deadSockets.add(player);
                            }
                        } catch (Exception e) {
                            deadSockets.add(player); 
                        }
                    }
                    room.players.removeAll(deadSockets);

                    if (result == 2) { 
                        System.out.println("CHIẾN THẮNG: " + username);
                        String winMsg = "{\"type\": \"GAME_OVER\", \"winner\": \"" + username + "\"}";
                        for (WebSocket player : room.players) {
                            player.send(winMsg);
                        }
                    } 
                    else if (result == 3) { 
                        System.out.println("Bàn cờ đầy - ĐÃ GỬI THÔNG BÁO HÒA");
                    }
                    else { 
                        if (room.isBotMode && result == 1) {
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                    room.makeBotMove();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Lỗi parse tọa độ: " + e.getMessage());
        }
    }

    private void handlePlayWithBot(WebSocket conn, String message) {
        String username = extractValue(message, "username");
        String roomId = "BOT_ROOM_" + System.currentTimeMillis();

        GameRoom room = new GameRoom(roomId);
        room.players.add(conn);       
        room.isBotMode = true;       

        userMap.put(conn, username);
        roomMap.put(roomId, room);

        System.out.println("Tạo phòng Bot cho: " + username);
      
        String startMsg = "{\"type\": \"MATCH_FOUND\", \"roomId\": \"" + roomId 
                        + "\", \"opponent\": \"BOT_SIEU_CAP\", \"yourSymbol\": \"X\"}";
        conn.send(startMsg);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server đã khởi động tại cổng: " + getPort());
    }

    private void handleLogin(WebSocket conn, String message) {
        String username = extractValue(message, "username");
        String password = extractValue(message, "password");
        
        UserDAO.UserInfo userInfo = userDAO.checkLogin(username, password);
        if (userInfo != null) {
            String avatar = userInfo.avatar != null ? userInfo.avatar : "1.jpg";
            avatarCache.put(userInfo.username, avatar);
            double winRate = userInfo.getWinRate();
            
            String response = String.format(
                "{\"type\": \"LOGIN_SUCCESS\", \"username\": \"%s\", \"avatar\": \"%s\", \"email\": \"%s\", \"elo\": %d, \"wins\": %d, \"losses\": %d, \"winRate\": %.2f}",
                userInfo.username, 
                avatar,
                userInfo.email,
                userInfo.elo,
                userInfo.wins,
                userInfo.losses,
                winRate
            );
            conn.send(response);
            userMap.put(conn, userInfo.username);
            System.out.println("Đăng nhập thành công: " + userInfo.username);
        } else {
            conn.send("{\"type\": \"LOGIN_FAIL\", \"message\": \"Sai tài khoản hoặc mật khẩu\"}");
        }
    }

    private void handleRegister(WebSocket conn, String message) {
        String username = extractValue(message, "username");
        String password = extractValue(message, "password");
        String email = extractValue(message, "email");

        boolean isOk = userDAO.register(username, password, email);
        if (isOk) {
            avatarCache.put(username, "1.jpg");
            conn.send("{\"type\": \"REGISTER_SUCCESS\"}");
        } else {
            conn.send("{\"type\": \"REGISTER_FAIL\", \"message\": \"Tên đăng nhập đã tồn tại\"}");
        }
    }
    
    private void handleFindMatch(WebSocket conn, String message) {
        try {
            String username = extractValue(message, "username");
            if (username == null || username.isEmpty()) {
                conn.send("{\"type\": \"ERROR\", \"message\": \"Không tìm thấy username\"}");
                return;
            }
            
            userMap.put(conn, username);
            conn.send("{\"type\": \"WAITING\", \"message\": \"Đang tìm đối thủ...\"}");
            
            if (!waitingQueue.contains(conn)) {
                waitingQueue.add(conn);
            }
            
            if (waitingQueue.size() >= 2) {
                WebSocket player1 = waitingQueue.poll(); 
                WebSocket player2 = waitingQueue.poll(); 
                
                String roomId = "ROOM_" + System.currentTimeMillis(); 
                GameRoom newRoom = new GameRoom(roomId); 
                
                newRoom.players.add(player1);
                newRoom.players.add(player2);
                roomMap.put(roomId, newRoom);
                
                String player1Name = userMap.get(player1);
                String player2Name = userMap.get(player2);
                
                String messageToPlayer1 = String.format(
                    "{\"type\": \"MATCH_FOUND\", \"roomId\": \"%s\", \"opponent\": \"%s\", \"opponentAvatar\": \"%s\", \"yourSymbol\": \"X\"}",
                    roomId, player2Name, getAvatarFromCacheOrDB(player2Name)
                );
                
                String messageToPlayer2 = String.format(
                    "{\"type\": \"MATCH_FOUND\", \"roomId\": \"%s\", \"opponent\": \"%s\", \"opponentAvatar\": \"%s\", \"yourSymbol\": \"O\"}",
                    roomId, player1Name, getAvatarFromCacheOrDB(player1Name)
                );
                
                player1.send(messageToPlayer1);
                player2.send(messageToPlayer2);
                
                newRoom.moveCount = 0; 
                newRoom.startTimer();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            conn.send("{\"type\": \"ERROR\", \"message\": \"Lỗi server khi tìm trận\"}");
        }
    }
    
    private void handleJoinGame(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");

        if (!roomMap.containsKey(roomId)) {
            GameRoom newRoom = new GameRoom(roomId);
            if (roomId.startsWith("BOT_ROOM_")) {
                newRoom.isBotMode = true;
            }
            roomMap.put(roomId, newRoom);
        }

        GameRoom room = roomMap.get(roomId);

        if (!room.players.contains(conn)) {
            room.players.add(conn);
            userMap.put(conn, username);
        }
    }
    
    private void handleSurrender(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");
        gameRequestHandler.handleSurrender(roomId, username, conn);
    }
    
    private void handleChat(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String sender = extractValue(message, "sender");
        String text = extractValue(message, "text");
        
        GameRoom room = roomMap.get(roomId);
        if (room != null) {
            String chatMsg = String.format(
                "{\"type\": \"CHAT\", \"sender\": \"%s\", \"text\": \"%s\", \"roomId\": \"%s\"}", 
                sender, text, roomId
            );
            
            for (WebSocket player : room.players) {
                if (player != null && !player.isClosed()) {
                    player.send(chatMsg);
                }
            }
        }
    }
    
    private void handleLeaveRoom(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");
        gameRequestHandler.handleLeaveAsSurrender(roomId, username, conn);
    }
    
    private String extractValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return "";
            startIndex += searchKey.length();
            while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '\"')) {
                startIndex++;
            }
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == '\"' || c == ',' || c == '}') break;
                endIndex++;
            }
            return json.substring(startIndex, endIndex).replace("\"", "");
        } catch (Exception e) {
            return "";
        }
    }

    private void handleGetRanking(WebSocket conn, String message) {
        String sql = "SELECT username, score, wins, losses FROM users ORDER BY score DESC LIMIT 10"; 
        StringBuilder jsonList = new StringBuilder("[");

        try (Connection dbConn = DbConnection.getConnection(); 
             PreparedStatement stmt = dbConn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            boolean first = true;
            while (rs.next()) {
                if (!first) jsonList.append(",");
                
                String username = rs.getString("username");
                int score = rs.getInt("score"); 
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");

                String playerJson = String.format(
                    "{\"username\": \"%s\", \"score\": %d, \"wins\": %d, \"losses\": %d}",
                    username, score, wins, losses
                );
                
                jsonList.append(playerJson);
                first = false;
            }
            
            jsonList.append("]");
            String response = "{\"type\": \"RANKING_DATA\", \"rankingList\": " + jsonList.toString() + "}";
            conn.send(response);

        } catch (SQLException e) {
            e.printStackTrace();
            conn.send("{\"type\": \"ERROR\", \"message\": \"Lỗi lấy bảng xếp hạng\"}");
        }
    }

    private void handleDrawRequest(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");
        gameRequestHandler.handleDrawRequest(roomId, username, conn);
    }

    private void handleDrawResponse(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");
        String accept = extractValue(message, "accept");
        gameRequestHandler.handleDrawResponse(roomId, username, accept, conn);
    }

    private void handleUndoRequest(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        GameRoom room = roomMap.get(roomId);
        for (WebSocket p : room.players) {
            if (p != conn) p.send("{\"type\": \"UNDO_REQUEST\"}");
        }
    }

    private void handleUndoResponse(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String accept = extractValue(message, "accept");
        
        GameRoom room = roomMap.get(roomId);
        if ("true".equals(accept)) {
            room.undoLastMove();
            String msg = "{\"type\": \"UNDO_SUCCESS\"}";
            for (WebSocket p : room.players) p.send(msg);
        }
    }

    private void handleRematchRequest(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");

        GameRoom room = roomMap.get(roomId);
        if (room == null || !room.isGameEnded()) {
            conn.send("{\"type\": \"REMATCH_ERROR\", \"message\": \"Không thể gửi yêu cầu chơi lại\"}");
            return;
        }

        WebSocket opponent = null;
        for (WebSocket player : room.players) {
            String playerUsername = userMap.get(player);
            if (playerUsername != null && !playerUsername.equals(username)) {
                opponent = player;
                break;
            }
        }

        if (opponent != null) {
            String requestMsg = String.format(
                "{\"type\": \"REMATCH_REQUEST\", \"sender\": \"%s\", \"roomId\": \"%s\"}", 
                username, roomId
            );
            opponent.send(requestMsg);
            conn.send("{\"type\": \"REMATCH_REQUEST_SENT\", \"message\": \"Đã gửi lời mời chơi lại!\"}");
        } else {
            conn.send("{\"type\": \"REMATCH_ERROR\", \"message\": \"Đối thủ không còn trong phòng\"}");
        }
    }

    private void handleRematchResponse(WebSocket conn, String message) {
        String roomId = extractValue(message, "roomId");
        String username = extractValue(message, "username");
        String accept = extractValue(message, "accept");

        GameRoom oldRoom = roomMap.get(roomId);
        if (oldRoom == null) return;

        WebSocket requester = null;
        String requesterUsername = null;

        for (WebSocket player : oldRoom.players) {
            String playerUsername = userMap.get(player);
            if (playerUsername != null && !playerUsername.equals(username)) {
                requester = player;
                requesterUsername = playerUsername;
                break;
            }
        }

        if (requester != null && requesterUsername != null) {
            if ("true".equals(accept)) {
                String newRoomId = "ROOM_" + System.currentTimeMillis();
                GameRoom newRoom = new GameRoom(newRoomId);

                newRoom.players.add(requester);
                newRoom.players.add(conn);
                roomMap.put(newRoomId, newRoom);

                userMap.put(requester, requesterUsername);
                userMap.put(conn, username);

                String msgToRequester = String.format(
                    "{\"type\": \"REMATCH_ACCEPTED\", \"roomId\": \"%s\", \"opponent\": \"%s\", \"yourSymbol\": \"X\"}",
                    newRoomId, username
                );
                String msgToAccepter = String.format(
                    "{\"type\": \"REMATCH_ACCEPTED\", \"roomId\": \"%s\", \"opponent\": \"%s\", \"yourSymbol\": \"O\"}",
                    newRoomId, requesterUsername
                );

                requester.send(msgToRequester);
                conn.send(msgToAccepter);

                newRoom.startTimer();

            } else {
                String rejectMsg = String.format(
                    "{\"type\": \"REMATCH_REJECTED\", \"rejecter\": \"%s\"}", 
                    username
                );
                requester.send(rejectMsg);
            }
        }
    }
    
    private String getAvatarFromCacheOrDB(String username) {
        String avatar = avatarCache.get(username);
        if (avatar == null) {
            avatar = "1.jpg";
            avatarCache.put(username, avatar);
        }
        return avatar;
    }
    
    private void handleChangeAvatar(WebSocket conn, String message) {
        String username = extractValue(message, "username");
        String avatar = extractValue(message, "avatar");
        
        if (!avatar.matches("^[1-5]\\.jpg$")) {
            conn.send("{\"type\": \"ERROR\", \"message\": \"Avatar không hợp lệ. Chọn từ 1.jpg đến 5.jpg\"}");
            return;
        }
        
        avatarCache.put(username, avatar);
        String response = String.format("{\"type\": \"AVATAR_CHANGED\", \"avatar\": \"%s\"}", avatar);
        conn.send(response);
    }
    
    private void handleGetAvatar(WebSocket conn, String message) {
        String username = extractValue(message, "username");
        String avatar = getAvatarFromCacheOrDB(username);
        String response = String.format("{\"type\": \"AVATAR_INFO\", \"username\": \"%s\", \"avatar\": \"%s\"}", username, avatar);
        conn.send(response);
    }
    
    
    public static void main(String[] args) {
        int port = 8081;
        
       
        CaroServer server = new CaroServer(port);
        
        
        server.start();
        
        System.out.println("----------------------------------------------");
        System.out.println("Caro Server đang chạy trên port: " + port);
        System.out.println("Chế độ: Dễ dàng kết nối LAN");
        System.out.println("Địa chỉ kết nối Client: ws://<IP_MAY_SERVER>:8081");
        System.out.println("----------------------------------------------");
    }
}