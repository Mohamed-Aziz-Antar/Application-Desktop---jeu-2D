package network;

public class RemoteState {
    public float x, y;
    public long lamport;

    public RemoteState(float x, float y, long lamport) {
        this.x = x;
        this.y = y;
        this.lamport = lamport;
    }
}
