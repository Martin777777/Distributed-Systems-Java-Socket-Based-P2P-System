import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 *
 * This class is the adapter of the frame of a peer
 *
 * The adapter will close the server of the peer when the window of the peer is closed
 *
 * @author Ma Zixiao
 *
 */

public class PeerAdapter extends WindowAdapter {

    Peer peer;

    public PeerAdapter(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        peer.setVisible(false);

        peer.close();
        peer.dispose();

    }
}
