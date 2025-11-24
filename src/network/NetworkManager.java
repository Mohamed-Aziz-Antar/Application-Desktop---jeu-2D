package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkManager {
    private int playerId;
    private int port;
    private List<PeerConnection> peers;
    private ServerSocket serverSocket;
    private LamportClock lamportClock;
    private RequestQueue requestQueue;
    private boolean running;
    private GameStateCallback callback;
    
    public NetworkManager(int playerId, int port, GameStateCallback callback) {
        this.playerId = playerId;
        this.port = port;
        this.callback = callback;
        this.peers = new CopyOnWriteArrayList<>();
        this.lamportClock = new LamportClock();
        this.requestQueue = new RequestQueue();
        this.running = true;
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        new Thread(this::acceptConnections).start();
        System.out.println("Player " + playerId + " started on port " + port);
    }
    
    public void connectToPeer(String host, int peerPort, int peerId) {
        try {
            Socket socket = new Socket(host, peerPort);
            PeerConnection peer = new PeerConnection(socket, peerId, this);
            peers.add(peer);
            peer.start();
            
            // Send introduction
            Message intro = new Message(MessageType.CONNECT, playerId, lamportClock.increment(), null);
            peer.sendMessage(intro);
            
            System.out.println("Connected to peer " + peerId);
        } catch (IOException e) {
            System.err.println("Failed to connect to peer: " + e.getMessage());
        }
    }
    
    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                PeerConnection peer = new PeerConnection(socket, -1, this);
                peers.add(peer);
                peer.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
    
    public void requestCriticalSection(CriticalSectionTask task) {
        int timestamp = lamportClock.increment();
        Request request = new Request(playerId, timestamp);
        requestQueue.addRequest(request);
        
        // Broadcast REQUEST to all peers
        Message msg = new Message(MessageType.REQUEST, playerId, timestamp, null);
        broadcastMessage(msg);
        
        // Wait for replies from all peers
        new Thread(() -> {
            waitForReplies(request);
            // Execute critical section
            task.execute();
            // Release critical section
            releaseCriticalSection();
        }).start();
    }
    
    private void waitForReplies(Request request) {
        while (!canEnterCriticalSection(request)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private boolean canEnterCriticalSection(Request request) {
        // Check if we have replies from all peers
        if (request.getRepliesCount() < peers.size()) {
            return false;
        }
        // Check if our request has the earliest timestamp
        return requestQueue.isEarliest(request);
    }
    
    private void releaseCriticalSection() {
        requestQueue.removeRequest(playerId);
        Message msg = new Message(MessageType.RELEASE, playerId, lamportClock.increment(), null);
        broadcastMessage(msg);
    }
    
    public void handleMessage(Message msg, PeerConnection sender) {
        lamportClock.update(msg.getTimestamp());
        
        switch (msg.getType()) {
            case CONNECT:
                sender.setPeerId(msg.getSenderId());
                System.out.println("Peer " + msg.getSenderId() + " connected");
                break;
                
            case REQUEST:
                Request req = new Request(msg.getSenderId(), msg.getTimestamp());
                requestQueue.addRequest(req);
                // Send REPLY
                Message reply = new Message(MessageType.REPLY, playerId, lamportClock.increment(), null);
                sender.sendMessage(reply);
                break;
                
            case REPLY:
                requestQueue.addReply(msg.getSenderId());
                break;
                
            case RELEASE:
                requestQueue.removeRequest(msg.getSenderId());
                break;
                
            case GAME_STATE:
                if (callback != null) {
                    callback.onGameStateUpdate(msg.getData());
                }
                break;
        }
    }
    
    public void broadcastGameState(String gameState) {
        Message msg = new Message(MessageType.GAME_STATE, playerId, lamportClock.increment(), gameState);
        broadcastMessage(msg);
    }
    
    private void broadcastMessage(Message msg) {
        for (PeerConnection peer : peers) {
            peer.sendMessage(msg);
        }
    }
    
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (PeerConnection peer : peers) {
                peer.close();
            }
        } catch (IOException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public interface GameStateCallback {
        void onGameStateUpdate(String data);
    }
    
    public interface CriticalSectionTask {
        void execute();
    }
}

class LamportClock {
    private int time;
    
    public LamportClock() {
        this.time = 0;
    }
    
    public synchronized int increment() {
        return ++time;
    }
    
    public synchronized void update(int receivedTime) {
        time = Math.max(time, receivedTime) + 1;
    }
    
    public synchronized int getTime() {
        return time;
    }
}

class Request implements Comparable<Request> {
    private int processId;
    private int timestamp;
    private Set<Integer> replies;
    
    public Request(int processId, int timestamp) {
        this.processId = processId;
        this.timestamp = timestamp;
        this.replies = ConcurrentHashMap.newKeySet();
    }
    
    public void addReply(int peerId) {
        replies.add(peerId);
    }
    
    public int getRepliesCount() {
        return replies.size();
    }
    
    public int getProcessId() {
        return processId;
    }
    
    public int getTimestamp() {
        return timestamp;
    }
    
    @Override
    public int compareTo(Request other) {
        if (this.timestamp != other.timestamp) {
            return Integer.compare(this.timestamp, other.timestamp);
        }
        return Integer.compare(this.processId, other.processId);
    }
}

class RequestQueue {
    private PriorityQueue<Request> queue;
    private Map<Integer, Request> requestMap;
    
    public RequestQueue() {
        this.queue = new PriorityQueue<>();
        this.requestMap = new ConcurrentHashMap<>();
    }
    
    public synchronized void addRequest(Request request) {
        queue.add(request);
        requestMap.put(request.getProcessId(), request);
    }
    
    public synchronized void removeRequest(int processId) {
        Request req = requestMap.remove(processId);
        if (req != null) {
            queue.remove(req);
        }
    }
    
    public synchronized void addReply(int peerId) {
        for (Request req : requestMap.values()) {
            req.addReply(peerId);
        }
    }
    
    public synchronized boolean isEarliest(Request request) {
        return !queue.isEmpty() && queue.peek().equals(request);
    }
}

enum MessageType {
    CONNECT, REQUEST, REPLY, RELEASE, GAME_STATE
}

class Message implements Serializable {
    private MessageType type;
    private int senderId;
    private int timestamp;
    private String data;
    
    public Message(MessageType type, int senderId, int timestamp, String data) {
        this.type = type;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.data = data;
    }
    
    public MessageType getType() { return type; }
    public int getSenderId() { return senderId; }
    public int getTimestamp() { return timestamp; }
    public String getData() { return data; }
}

class PeerConnection {
    private Socket socket;
    private int peerId;
    private NetworkManager manager;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean running;
    
    public PeerConnection(Socket socket, int peerId, NetworkManager manager) throws IOException {
        this.socket = socket;
        this.peerId = peerId;
        this.manager = manager;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.running = true;
    }
    
    public void start() {
        new Thread(this::receiveMessages).start();
    }
    
    private void receiveMessages() {
        while (running) {
            try {
                Message msg = (Message) in.readObject();
                manager.handleMessage(msg, this);
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    System.err.println("Error receiving message: " + e.getMessage());
                    running = false;
                }
            }
        }
    }
    
    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
    
    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }
    
    public int getPeerId() {
        return peerId;
    }
    
    public void close() throws IOException {
        running = false;
        socket.close();
    }
}