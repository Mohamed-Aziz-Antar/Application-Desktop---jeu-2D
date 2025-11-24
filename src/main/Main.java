package main;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import network.NetworkManager;

public class Main {

    public static void main(String[] args) {
        // Get network configuration from user
        String[] options = {"Host Game", "Join Game", "Single Player"};
        int choice = JOptionPane.showOptionDialog(null,
                "Choose game mode:",
                "Network Setup",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        NetworkManager networkManager = null;
        
        if (choice == 0) {
            // Host game
            String portStr = JOptionPane.showInputDialog("Enter port to host on:", "5000");
            int port = Integer.parseInt(portStr);
            String playerIdStr = JOptionPane.showInputDialog("Enter your player ID:", "1");
            int playerId = Integer.parseInt(playerIdStr);
            
            try {
                JFrame window = new JFrame();
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.setResizable(false);
                window.setTitle("2D Adventure - Host (Player " + playerId + ")");
                
                gamePanel gamePanel = new gamePanel();
                
                // Create network manager with callback
                networkManager = new NetworkManager(playerId, port, (data) -> {
                    gamePanel.handleRemoteGameState(data);
                });
                networkManager.start();
                
                gamePanel.setNetworkManager(networkManager);
                
                window.add(gamePanel);
                window.pack();
                window.setLocationRelativeTo(null);
                window.setVisible(true);
                gamePanel.setUpGame();
                gamePanel.startGameThread();
                
                JOptionPane.showMessageDialog(null, 
                    "Hosting on port " + port + "\nShare this port with other players to connect!");
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting host: " + e.getMessage());
            }
            
        } else if (choice == 1) {
            // Join game
            String playerIdStr = JOptionPane.showInputDialog("Enter your player ID:", "2");
            int playerId = Integer.parseInt(playerIdStr);
            String portStr = JOptionPane.showInputDialog("Enter your port:", "5001");
            int port = Integer.parseInt(portStr);
            
            String hostAddress = JOptionPane.showInputDialog("Enter host address:", "localhost");
            String hostPortStr = JOptionPane.showInputDialog("Enter host port:", "5000");
            int hostPort = Integer.parseInt(hostPortStr);
            String hostIdStr = JOptionPane.showInputDialog("Enter host player ID:", "1");
            int hostId = Integer.parseInt(hostIdStr);
            
            try {
                JFrame window = new JFrame();
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.setResizable(false);
                window.setTitle("2D Adventure - Client (Player " + playerId + ")");
                
                gamePanel gamePanel = new gamePanel();
                
                // Create network manager with callback
                networkManager = new NetworkManager(playerId, port, (data) -> {
                    gamePanel.handleRemoteGameState(data);
                });
                networkManager.start();
                
                // Connect to host
                networkManager.connectToPeer(hostAddress, hostPort, hostId);
                
                gamePanel.setNetworkManager(networkManager);
                
                window.add(gamePanel);
                window.pack();
                window.setLocationRelativeTo(null);
                window.setVisible(true);
                gamePanel.setUpGame();
                gamePanel.startGameThread();
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error joining game: " + e.getMessage());
            }
            
        } else {
            // Single player (original behavior)
            JFrame window = new JFrame();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.setTitle("2D Adventure - Single Player");
            gamePanel gamePanel = new gamePanel();
            window.add(gamePanel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            gamePanel.setUpGame();
            gamePanel.startGameThread();
        }
        
        // Add shutdown hook to clean up network connections
        final NetworkManager finalNetworkManager = networkManager;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (finalNetworkManager != null) {
                finalNetworkManager.shutdown();
            }
        }));
    }
}