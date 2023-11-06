/**
 *
 * This class sets up the main server and the peers in this system
 *
 * @author Ma Zixiao
 *
 */

public class Main {
    public static void main(String[] args) {
        MainServer server = new MainServer();
        Peer peer1 = new Peer("127.0.0.1", 500, 1);

        Peer peer2 = new Peer("127.0.0.1", 700, 2);

        Peer peer3 = new Peer("127.0.0.1", 900, 3);

        Peer peer4 = new Peer("127.0.0.1", 1200, 4);
    }
}
