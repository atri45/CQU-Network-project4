package com.mq.myqq;

import org.springframework.boot.SpringApplication;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static final int PORT = 8888;
    private static Map<String, BufferedWriter> clientWriters = new HashMap<>();
    private static Map<String, List<String>> groupMembers = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(MyQqApplication.class, args);
        Server.startServer();
    }

    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("MyQQ> Server is running on port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        String clientName = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            clientName = reader.readLine();
            if (clientName != null) {
                clientWriters.put(clientName, writer);
                System.out.println("MyQQ> New client connected: " + clientName);
                // 发送当前客户端列表给新客户端
                sendClientList();
            }

            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("MyQQ> receive message: " + message);
                // 处理收到的消息
                if (message.startsWith("/openChatRoom")) {
                    // Format: /createGroup groupName member1 member2 ...
                    createGroup(message, clientName);
                } else if(message.startsWith("/file")){
                    handleFileTransferCommand(message,clientName);
                } else {
                    broadcastMessage(clientName, message);
                }
            }

        } catch (IOException e) {
            System.out.println("MyQQ> Client disconnected: " + clientName);
        } finally {
            if (clientName != null) {
                // 移除已断开连接的客户端
                clientWriters.remove(clientName);
                sendClientList();
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("MyQQ> Error closing socket: " + e.getMessage());
            }
        }
    }


    private static void handleFileTransferCommand(String command, String sender) {
        // 解析文件传输命令，格式："/file groupName fileName bytesRead base64EncodedData"
        String[] parts = command.split(" ");

        // 在访问数组元素之前确保parts数组有足够的元素
        if (parts.length >= 5) {
            String groupName = parts[1];
            String fileName = parts[2];
            int bytesRead = Integer.parseInt(parts[3]);
            String base64EncodedData = parts[4];

            // 将base64编码的数据解码为字节数组
            byte[] fileBytes = Base64.getDecoder().decode(base64EncodedData);

            // 广播文件传输消息给群组成员
            for (String member : groupMembers.get(groupName)) {
                if (!member.equals(sender)) {
                    sendMessage(clientWriters.get(member), "[文件已接收] " + fileName);
                    sendFileBytes(clientWriters.get(member), groupName, fileName, bytesRead, fileBytes);
                }
            }
        } else {
            // 处理parts数组元素不足的情况
            System.err.println("MyQQ> 无效的文件传输命令: " + command);
        }
    }


    private static void sendFileBytes(BufferedWriter writer, String groupName, String fileName, int bytesRead, byte[] fileBytes) {
        try {
            // 将文件内容发送给客户端
            writer.write("/file " + groupName + " " + fileName + " " + bytesRead + " " + Base64.getEncoder().encodeToString(fileBytes));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcastMessage(String sender, String message) {
        // Broadcast the message to all clients
        for (Map.Entry<String, BufferedWriter> entry : clientWriters.entrySet()) {
            String clientName = entry.getKey();
            if (!clientName.equals(sender)) {
                sendMessage(entry.getValue(), message);
            }
        }
    }

    private static void sendMessage(BufferedWriter writer, String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendClientList() {
        // Send the updated client list to all clients
        StringBuilder clientListMessage = new StringBuilder("/clientlist");
        for (String client : clientWriters.keySet()) {
            clientListMessage.append(" ").append(client);
        }

        for (BufferedWriter writer : clientWriters.values()) {
            sendMessage(writer, clientListMessage.toString());
        }
    }

    private static void createGroup(String message, String creator) {
        // Format: /createGroup groupName member1 member2 ...
        String[] parts = message.split(" ");
        String groupName = parts[1];
        List<String> members = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            members.add(parts[i]);
            System.out.println("MyQQ> new chatRoom member: " + parts[i]);
        }

        groupMembers.put(groupName, members);
        sendGroupList(groupName);
    }

    private static void sendGroupList(String groupName) {
        // Send the updated group list to all clients
        StringBuilder groupListMessage = new StringBuilder("/grouplist " + groupName);
        for (String member : groupMembers.get(groupName)) {
            groupListMessage.append(" ").append(member);
        }

        for (String member : groupMembers.get(groupName)) {
            if (clientWriters.containsKey(member)) {
                sendMessage(clientWriters.get(member), groupListMessage.toString());
            }
        }
    }
}

