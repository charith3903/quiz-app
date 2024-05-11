package quiz_java;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class QClientGUI {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private JFrame frame;
    private JPanel questionPanel;
    private JButton submitButton;
    private PrintWriter out;
    private BufferedReader in;
    private ButtonGroup choicesGroup;
    

    public QClientGUI(int x, int y) {
        frame = new JFrame("Question Client");
        questionPanel = new JPanel();
        questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
        submitButton = new JButton("Submit Answer");

        frame.getContentPane().add(questionPanel, BorderLayout.CENTER);
        frame.getContentPane().add(submitButton, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //EXIT_ON_CLOSE
        frame.pack();
        frame.setLocation(x, y);
        frame.setVisible(true);

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ButtonModel selectedChoice = choicesGroup.getSelection();
                if (selectedChoice != null) {
                    out.println(selectedChoice.getActionCommand()); // Send answer to server
                    choicesGroup.clearSelection();
                }
            }
        });
    }

    public void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                    	if ("END".equals(fromServer)) {
                    	    String scoreAndTotal = in.readLine(); // Read the score and total questions from the server
                    	    JOptionPane.showMessageDialog(frame, "Quiz finished. Your score is " + scoreAndTotal);
                    	    frame.dispose();
                    	    break;
                    	} else if ("Correct".equals(fromServer) || "Incorrect".equals(fromServer)) {
                            JOptionPane.showMessageDialog(frame, "Your answer is " + fromServer);
                        } else {
                            String question = fromServer;
                            String[] choices = in.readLine().split(","); // Read choices from server

                            // Update question and choices
                            SwingUtilities.invokeLater(() -> {
                                questionPanel.removeAll();
                                questionPanel.add(new JLabel(question));
                                choicesGroup = new ButtonGroup();
                                for (String choice : choices) {
                                    JRadioButton radioButton = new JRadioButton(choice);
                                    radioButton.setActionCommand(choice);
                                    choicesGroup.add(radioButton);
                                    questionPanel.add(radioButton);
                                }
                                questionPanel.revalidate();
                                questionPanel.repaint();
                                
                                frame.pack();
                            });
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Number of client windows you want to open
        int numberOfClients = 5;

        for (int i = 0; i < numberOfClients; i++) {
        	int x = 300 * i; 
            int y = 300;
            SwingUtilities.invokeLater(() -> {
                QClientGUI client = new QClientGUI(x, y);
                client.connectToServer();
            });
        }
    }

}