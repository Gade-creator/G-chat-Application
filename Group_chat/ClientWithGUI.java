package Group_chat;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClientWithGUI {
    private JFrame frame;
    private JTextPane chatArea;  // To style sent and received messages
    private JTextField inputField;
    private JButton sendButton;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    public ClientWithGUI(Socket socket, String username) {
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            createGUI(username);

            // Start a thread to listen for incoming messages
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            closeResources();
        }
    }

    private void createGUI(String username) {
        frame = new JFrame("Chat Client - " + username);

        chatArea = new JTextPane();
        chatArea.setEditable(false);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        sendButton.addActionListener(e -> sendMessage());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);

        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                appendMessage("You: " + message, true); // Show sent message aligned right
                inputField.setText("");  // Clear the input field
            } catch (IOException e) {
                closeResources();
            }
        }
    }

    private void listenForMessages() {
        String message;
        try {
            while ((message = bufferedReader.readLine()) != null) {
                appendMessage(message, false); // Show received message aligned left
            }
        } catch (IOException e) {
            closeResources();
        }
    }

    private void appendMessage(String message, boolean isSent) {
        try {
            StyledDocument doc = chatArea.getStyledDocument();
            SimpleAttributeSet attributes = new SimpleAttributeSet();

            // Align text based on whether it's sent or received
            StyleConstants.setAlignment(attributes, isSent ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setForeground(attributes, isSent ? Color.BLUE : Color.BLACK); // Blue for sent, black for received
            StyleConstants.setFontSize(attributes, 14);

            doc.insertString(doc.getLength(), message + "\n", attributes);
            doc.setParagraphAttributes(doc.getLength(), 1, attributes, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void closeResources() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 7800);
            String username = JOptionPane.showInputDialog("Enter your username:");
            if (username != null && !username.trim().isEmpty()) {
                new ClientWithGUI(socket, username);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
