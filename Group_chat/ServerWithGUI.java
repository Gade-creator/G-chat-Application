package Group_chat;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerWithGUI {
    private JFrame frame;
    private JTextArea textArea;
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clientHandlers;

    public ServerWithGUI(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.clientHandlers = new ArrayList<>();
        createGUI();
    }

    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                textArea.append("A new client connected!\n");

                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            textArea.append("Server error: " + e.getMessage() + "\n");
        } finally {
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("Chat Server");
        textArea = new JTextArea();
        textArea.setEditable(false);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(7800);
            ServerWithGUI server = new ServerWithGUI(serverSocket);
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class for handling client connections
    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String clientUsername;

        public ClientHandler(Socket socket) {
            try {
                this.socket = socket;
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                this.clientUsername = bufferedReader.readLine();
                broadcastMessage("SERVER: " + clientUsername + " has joined the chat!");
                textArea.append(clientUsername + " joined the chat.\n");
            } catch (IOException e) {
                closeResources();
            }
        }

        @Override
        public void run() {
            String message;
            while (socket.isConnected()) {
                try {
                    message = bufferedReader.readLine();
                    if (message != null) {
                        broadcastMessage(clientUsername + ": " + message);
                    }
                } catch (IOException e) {
                    closeResources();
                    break;
                }
            }
        }

        private void broadcastMessage(String message) {
            textArea.append(message + "\n");
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    if (!clientHandler.equals(this)) {
                        clientHandler.bufferedWriter.write(message);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeResources();
                }
            }
        }

        private void closeResources() {
            clientHandlers.remove(this);
            broadcastMessage("SERVER: " + clientUsername + " has left the chat.");
            try {
                if (bufferedReader != null) bufferedReader.close();
                if (bufferedWriter != null) bufferedWriter.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
