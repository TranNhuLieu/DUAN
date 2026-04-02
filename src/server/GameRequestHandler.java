/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import database.MatchDAO;
import database.UserDAO;
import java.util.Map;
import org.java_websocket.WebSocket;
public class GameRequestHandler {
    private final DrawHandler drawHandler;
    private final SurrenderHandler surrenderHandler;
    
    public GameRequestHandler(MatchDAO matchDAO, UserDAO userDAO,
                            Map<String, GameRoom> roomMap, Map<WebSocket, String> userMap) {
        this.drawHandler = new DrawHandler(matchDAO, userDAO, roomMap, userMap);
        this.surrenderHandler = new SurrenderHandler(matchDAO, userDAO, roomMap, userMap);
    }
    
    public void handleDrawRequest(String roomId, String username, WebSocket conn) {
        drawHandler.handleDrawRequest(roomId, username, conn);
    }
    
    public void handleDrawResponse(String roomId, String username, String accept, WebSocket conn) {
        drawHandler.handleDrawResponse(roomId, username, accept, conn);
    }
    
    public void handleSurrender(String roomId, String username, WebSocket conn) {
        surrenderHandler.handleSurrender(roomId, username, conn);
    }
    
    public void handleLeaveAsSurrender(String roomId, String username, WebSocket conn) {
        surrenderHandler.handleLeaveAsSurrender(roomId, username, conn);
    }
}
