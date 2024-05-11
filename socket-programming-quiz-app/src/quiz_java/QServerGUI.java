package quiz_java;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.*;


public class QServerGUI {
    private static final int PORT = 12345;
    private static final String[] QUESTIONS = {
        "Who was the first person to walk on the moon?",
        "What is the capital of France?",
        "What is the largest ocean on Earth?",
        "Who wrote the novel '1984'?",
        "What is the square root of 81?"
    };

    private static final String[][] CHOICES = {
        {"Neil Armstrong", "Buzz Aldrin", "Yuri Gagarin", "Alan Shepard"},
        {"London", "Berlin", "Paris", "Madrid"},
        {"Atlantic", "Indian", "Arctic", "Pacific"},
        {"George Orwell", "Ernest Hemingway", "F. Scott Fitzgerald", "J.D. Salinger"},
        {"9", "6", "7", "8"}
    };

    private static final String[] ANSWERS = {
        "Neil Armstrong",
        "Paris",
        "Pacific",
        "George Orwell",
        "9"
    };
    private static int clientCounter = 0;
    private JFrame frame;
    private JPanel clientPanel; // Panel to hold client text areas
    private Map<Integer, JTextArea> clientTextAreas; // Map to track text areas for each client
    private JTextArea serverTextArea;

    public QServerGUI() {
        frame = new JFrame("Question Server");
        clientPanel = new JPanel();
        clientPanel.setLayout(new BoxLayout(clientPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(clientPanel);
        serverTextArea = new JTextArea(6, 50);
        serverTextArea.setEditable(false);
        serverTextArea.setBorder(BorderFactory.createTitledBorder("Server"));
        frame.getContentPane().add(new JScrollPane(serverTextArea), BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(600, 800)); // Set preferred size for the frame
        frame.pack();
        frame.setVisible(true);
        clientTextAreas = new HashMap<>();
    }

    public void startServer() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                    publish("Server started. Waiting for clients...\n");
                    while (true) { // Keep listening for new clients
                        Socket clientSocket = serverSocket.accept(); // Accept a new client
                        new Thread(new ClientHandler(clientSocket)).start(); // Handle the client in a new thread
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
            	for (String text : chunks) {
                    serverTextArea.append(text);
                }
            }
        };
        worker.execute();
    }

    // Inner class to handle each client connection
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int clientId;
        private JTextArea clientTextArea;
        private int totalQuestions = ANSWERS.length;
        private int score = 0;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientId = ++clientCounter;
            // Create a text area for this client
            clientTextArea = new JTextArea(6, 40);
            clientTextArea.setEditable(false);
            clientTextArea.setBorder(BorderFactory.createTitledBorder("Client " + clientId));
            // Add the text area to the client panel
            SwingUtilities.invokeLater(() -> {
                clientPanel.add(clientTextArea);
                clientPanel.revalidate();
            });
            // Store the text area in the map
            clientTextAreas.put(clientId, clientTextArea);
        }

        @Override
        public void run() {
        	
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                publish("Client connected.\n");

                for (int i = 0; i < QUESTIONS.length; i++) {
                    out.println(QUESTIONS[i]); // Send question to client
                    out.println(String.join(",", CHOICES[i])); // Send choices to client
                    String answer = in.readLine(); // Read answer from client
                    publish("Client answered: " + answer + "\n");
                    if (ANSWERS[i].equalsIgnoreCase(answer)) {
                        out.println("Correct");
                        score++; // Increment the score if the answer is correct
                        publish("The answer is correct.");
                    } else {
                        out.println("Incorrect");
                        publish("The answer is incorrect.");
                    }
                }

                out.println("END"); // Signal the end of the quiz
                out.println(score + "/" + totalQuestions);
                publish("Total correct answers: " + score);
                clientSocket.close(); // Close the client socket
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Helper method to publish updates to the SwingWorker
        private void publish(String text) {
            String message = text + "\n";
            SwingUtilities.invokeLater(() -> clientTextArea.append(message));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QServerGUI server = new QServerGUI();
            server.startServer();
        });
    }
}