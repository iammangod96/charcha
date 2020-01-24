package com.charcha;

import com.charcha.constants.MessageType;
import com.charcha.entity.Message;
import com.charcha.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    private Set<WebSocket> conns;
    private HashMap<WebSocket, User> users;

    ChatServer(int port) {
        super(new InetSocketAddress(port));
        conns = new HashSet<>();
        users = new HashMap<>();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        conns.add(webSocket);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        conns.remove(webSocket);
        try {
            removeUser(webSocket);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        System.out.println("INSIDE ONMESSAGE:" + s.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Message message = objectMapper.readValue(s, Message.class);
            System.out.println("Message: " + message.getMessageType() + " " + message.getUser().getName() + " " + message.getUser().getId() + " " + message.getData());
            switch (message.getMessageType()) {
                case USER_JOINED:
                    System.out.println("Inside USER_JOINED CASE");
                    addUser(new User(message.getUser().getName()),webSocket);
                    break;
                case USER_LEFT:
                    removeUser(webSocket);
                    break;
                case TEXT_MESSAGE:
                    broadcastMessage(message);
                    break;
                default:
                    System.out.println("Default case.");
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        if(webSocket != null) {
            conns.remove(webSocket);
        }
    }

    @Override
    public void onStart() {

    }

    private void addUser(User user, WebSocket webSocket) throws JsonProcessingException {
        System.out.println("Inside addUser method");
        users.put(webSocket, user);
        acknowledgeUserJoined(user, webSocket);
        broadcastUserActivityMessage(MessageType.USER_JOINED);
    }

    private void acknowledgeUserJoined(User user, WebSocket webSocket) throws JsonProcessingException {
        System.out.println("Inside acknowledgement method");
        Message message = new Message();
        message.setMessageType(MessageType.USER_JOINED_ACK);
        message.setUser(user);
        ObjectMapper objectMapper = new ObjectMapper();
        String messageJson = objectMapper.writeValueAsString(message);
        System.out.println("Inside acknowledgement: " + messageJson);
        webSocket.send(messageJson);
    }

    private void removeUser(WebSocket webSocket) throws JsonProcessingException {
        users.remove(webSocket);
        broadcastUserActivityMessage(MessageType.USER_LEFT);
    }

    private void broadcastMessage(Message message) {
        System.out.println("Inside broadcast message");
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            String messageJson = objectMapper.writeValueAsString(message);
            for(WebSocket webSocket: conns) {
                webSocket.send(messageJson);
            }
        } catch(JsonProcessingException e) {
            System.out.println(e);
        }
    }

    private void broadcastUserActivityMessage(MessageType messageType) throws JsonProcessingException {
        System.out.println("broadcast user activity");
        Message message = new Message();
        message.setMessageType(messageType);
        ObjectMapper objectMapper = new ObjectMapper();
        message.setData(objectMapper.writeValueAsString(users.values()));
        broadcastMessage(message);
    }

    public static void main(String args[]) {
        int port;
        try{
            port = Integer.parseInt(System.getenv("PORT"));
        } catch (NumberFormatException e) {
            port = 9000;
        }
        new ChatServer(port).run();
    }

}
