package main;

import java.awt.Dimension;
import java.awt.Color;
import javax.swing.JPanel;
import entity.Player;
import entity.RemotePlayer;
import object.SuperObject;
import tile.tileManager;
import network.NetworkManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class gamePanel extends JPanel implements Runnable {
    
    Sound sound = new Sound();
    final int fps = 60;
    tileManager tileM = new tileManager(this);
    
    final int originalTileSize = 16;
    final int scale = 3;
    final public int tileSize = originalTileSize * scale;
    
    final public int maxScreenCol = 16;
    final public int maxScreenRow = 12;
    final public int screenWidth = tileSize * maxScreenCol;
    final public int screenHeight = tileSize * maxScreenRow;
    
    // WORLD SETTINGS
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    
    keyHandler keyH = new keyHandler();
    Thread gameThread;
    public CollisionChecker cChecker = new CollisionChecker(this);
    
    // ENTITY AND OBJECT 
    public Player player = new Player(this, keyH);
    public SuperObject obj[] = new SuperObject[10];
    public AssetSetter aSetter = new AssetSetter(this);
    
    // NETWORK
    private NetworkManager networkManager;
    private long lastNetworkUpdate = 0;
    private static final long NETWORK_UPDATE_INTERVAL = 50; // milliseconds
    
    // REMOTE PLAYERS
    private Map<Integer, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    
    public gamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
    }
    
    public void setNetworkManager(NetworkManager manager) {
        this.networkManager = manager;
    }
    
    public void setUpGame() {
        aSetter.setObject();
        playMusic(0);
    }
    
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }
    
    @Override
    public void run() {
        double drawInterval = 1000000000 / fps;
        double nextDrawTime = System.nanoTime() + drawInterval;
        
        while (gameThread != null) {
            long currentTime = System.nanoTime();
            update();
            repaint();
            
            // Send game state to other players periodically
            if (networkManager != null) {
                long now = System.currentTimeMillis();
                if (now - lastNetworkUpdate > NETWORK_UPDATE_INTERVAL) {
                    broadcastGameState();
                    lastNetworkUpdate = now;
                }
            }
            
            double remainingTime = nextDrawTime - System.nanoTime();
            remainingTime = remainingTime / 1000000;
            
            try {
                if (remainingTime < 0) {
                    remainingTime = 0;
                }
                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void update() {
        player.update();
        
        // Remove inactive remote players
        remotePlayers.entrySet().removeIf(entry -> !entry.getValue().isActive());
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        // tile
        tileM.draw(g2);
        
        // OBJECT
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] != null) {
                obj[i].draw(g2, this);
            }
        }
        
        // Draw remote players BEFORE local player
        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.draw(g2, player.screenX, player.screenY);
        }
        
        // Draw local player
        player.draw(g2);
        
        // Draw player count
        g2.setColor(Color.WHITE);
        g2.drawString("Players: " + (remotePlayers.size() + 1), 10, 20);
        
        g2.dispose();
    }
    
    // Broadcast current game state to all connected players
    private void broadcastGameState() {
        String gameState = serializeGameState();
        networkManager.broadcastGameState(gameState);
    }
    
    // Serialize game state to string
    private String serializeGameState() {
        StringBuilder sb = new StringBuilder();
        sb.append("PLAYER:");
        sb.append(networkManager.getPlayerId()).append(",");
        sb.append(player.worldX).append(",");
        sb.append(player.worldY).append(",");
        sb.append(player.direction).append(",");
        sb.append(player.spriteNum);
        
        // Add object states
        sb.append("|OBJECTS:");
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] != null) {
                sb.append(i).append(":").append(obj[i].name).append(";");
            } else {
                sb.append(i).append(":null;");
            }
        }
        
        return sb.toString();
    }
    
    // Handle incoming game state from remote players
    public void handleRemoteGameState(String data) {
        if (data == null || data.isEmpty()) return;
        
        try {
            String[] parts = data.split("\\|");
            for (String part : parts) {
                if (part.startsWith("PLAYER:")) {
                    String playerData = part.substring(7);
                    String[] values = playerData.split(",");
                    
                    int remotePlayerId = Integer.parseInt(values[0]);
                    int worldX = Integer.parseInt(values[1]);
                    int worldY = Integer.parseInt(values[2]);
                    String direction = values[3];
                    int spriteNum = Integer.parseInt(values[4]);
                    
                    // Don't create a remote player for ourselves
                    if (networkManager != null && remotePlayerId == networkManager.getPlayerId()) {
                        continue;
                    }
                    
                    // Get or create remote player
                    RemotePlayer remotePlayer = remotePlayers.get(remotePlayerId);
                    if (remotePlayer == null) {
                        remotePlayer = new RemotePlayer(this, remotePlayerId);
                        remotePlayers.put(remotePlayerId, remotePlayer);
                        System.out.println("New remote player connected: " + remotePlayerId);
                    }
                    
                    // Update remote player position
                    remotePlayer.updatePosition(worldX, worldY, direction, spriteNum);
                    
                } else if (part.startsWith("OBJECTS:")) {
                    // Handle object synchronization
                    String objectData = part.substring(8);
                    if (!objectData.isEmpty()) {
                        String[] objects = objectData.split(";");
                        for (String objInfo : objects) {
                            if (!objInfo.isEmpty()) {
                                String[] objParts = objInfo.split(":");
                                int index = Integer.parseInt(objParts[0]);
                                String objName = objParts[1];
                                
                                // Sync object state (simple version)
                                if (objName.equals("null")) {
                                    obj[index] = null;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing game state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Use critical section for important game events (like picking up items)
    public void pickUpObjectWithLock(int index) {
        if (networkManager != null) {
            networkManager.requestCriticalSection(() -> {
                // This code runs in critical section (mutual exclusion)
                player.pickUpObject(index);
                // Broadcast the change immediately
                broadcastGameState();
            });
        } else {
            // Single player mode
            player.pickUpObject(index);
        }
    }
    
    public void playMusic(int i) {
        sound.setFile(i);
        sound.play();
        sound.loop();
    }
    
    public void stopMusic() {
        sound.stop();
    }
    
    public void playSE(int i) {
        sound.setFile(i);
        sound.play();
    }
}