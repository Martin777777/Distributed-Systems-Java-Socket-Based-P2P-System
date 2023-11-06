import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 *
 * This class represents the main server in this system.
 *
 * The main server handles several types of messages sent from peers
 *
 * More details about the types of messages received and the ways to handle them is shown below
 *
 * @author Ma Zixiao
 *
 */

public class MainServer extends JFrame implements Runnable {
    private HashMap<String, ArrayList<String>> uhpt;
    private HashMap<String, ArrayList<String>> uhrt;
    private ServerSocket serverSocket;
    public static final String IP = "127.0.0.1";
    public static final int PORT = 77;
    private TextArea textArea;
    public MainServer() {
        uhpt = new HashMap<>();
        uhrt = new HashMap<>();
        this.setSize(700, 600);
        textArea = new TextArea(25, 70);
        textArea.setBackground(Color.yellow);
        this.add(textArea);
        this.initServer();
        this.setTitle("Main Server");
        this.setVisible(true);
    }

    private void initServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            new Thread(this).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (serverSocket.isClosed()) {
            return;
        }

        while (true){
            try {
                Socket client = serverSocket.accept();
                DataInputStream inputStream = new DataInputStream(client.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
                String[] sentArray = inputStream.readUTF().split("\\|\\|");
                switch (Integer.parseInt(sentArray[0])) {
                    //In this case, the main server handles the message message which is to inform server opening
                    case 0:
                        String peerID = GUIDGeneration.generatePeerGUID();
                        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(sentArray).subList(1, 3));
                        arrayList.add(Integer.toString(sentArray.length));
                        for (int i = 3; i < sentArray.length; i++) {
                            String resourceID = sentArray[i];
                            if (!uhrt.containsKey(resourceID)) {
                                uhrt.put(resourceID, new ArrayList<>(Collections.singletonList(peerID)));
                                Resources.add(resourceID);
                            }
                            else {
                                uhrt.get(resourceID).add(peerID);
                            }
                        }
                        uhpt.put(peerID, arrayList);
                        outputStream.writeUTF("Agree Open||" + peerID);
                        textArea.append(">>> " + peerID + " opened." + "\n");
                        textArea.append(">>> " + "new UHPT: " + uhpt.toString() + "\n");
                        textArea.append(">>> " + "new UHRT: " + uhrt.toString() + "\n");


                        client.shutdownInput();
                        client.shutdownOutput();
                        client.close();

                        break;
                    //In this case, the main server handles the message which is to ask for the contact information of the owner of a resource
                    case 1:

                        String resourceID = sentArray[1];
                        ArrayList<String> peers = uhrt.get(resourceID);
                        ArrayList<String> sortedPeers = new ArrayList<>();
                        if (peers.size() > 1){
                            peers.parallelStream().sorted((o1, o2) -> {
                                int a = Integer.parseInt(uhpt.get(o1).get(2));
                                int b = Integer.parseInt(uhpt.get(o2).get(2));
                                return Integer.compare(a, b);
                            }).forEachOrdered(sortedPeers::add);
                        }
                        else {
                            sortedPeers = peers;
                        }
                        ArrayList<String> peerInformation = uhpt.get(sortedPeers.get(0));
                        outputStream.writeUTF(peerInformation.get(0) + "||" + peerInformation.get(1));
                        textArea.append(">>> " + "The best resource owner is chosen for a resource ask\n");
                        client.shutdownInput();
                        client.shutdownOutput();
                        client.close();
                        break;

                    //In this case, the main server is informed of a resource addition
                    case 3:
                        //add to uhrt
                        String newPeerGUID = sentArray[1];
                        String newResourceGUID = sentArray[2];
                        if (!uhrt.containsKey(newResourceGUID)) {
                            uhrt.put(newResourceGUID, new ArrayList<>(Collections.singletonList(newPeerGUID)));
                            Resources.add(newResourceGUID);
                        }
                        else {
                            uhrt.get(newResourceGUID).add(newPeerGUID);
                        }
                        textArea.append(">>> " + "Informed new resource added by a peer.\n");
                        textArea.append(">>> " + "new UHRT: " + uhrt.toString() + "\n");
                        client.shutdownInput();
                        client.shutdownOutput();
                        client.close();
                        break;

                    //In this case, the main server is informed of a resource removal
                    case 4:
                        //remove resource
                        String removePeerGUID = sentArray[1];
                        String removeResourceGUID = sentArray[2];
                        if (uhrt.get(removeResourceGUID).size() > 1) {
                            uhrt.get(removeResourceGUID).remove(removePeerGUID);
                        }
                        else {
                            uhrt.remove(removeResourceGUID);
                            Resources.remove(removeResourceGUID);
                        }
                        System.out.println(uhrt);
                        textArea.append(">>> " + "Informed a resource removedcfdcvfdfdca exzswvbgtrfhdcfd by a peer.\n");
                        textArea.append(">>> " + "new UHRT: " + uhrt.toString() + "\n");
                        client.shutdownInput();
                        client.shutdownOutput();
                        client.close();
                        break;

                    //In this case, the main server is informed that a peer closes its server
                    case 5:
                        //inform close
                        String closePeerGUID = sentArray[1];
                        uhpt.remove(closePeerGUID);
                        for(Iterator<String> iterator = uhrt.keySet().iterator(); iterator.hasNext(); ) {
                            String key = iterator.next();
                            ArrayList<String> resourceOwners = uhrt.get(key);
                            if (resourceOwners.contains(closePeerGUID)) {
                                if (resourceOwners.size() == 1) {
                                    iterator.remove();
                                    Resources.remove(key);
                                }
                                else {
                                    resourceOwners.remove(closePeerGUID);
                                }
                            }
                        }
                        textArea.append(">>> " + "A peer closed its server.\n");
                        textArea.append(">>> " + "New UHPT: " + uhpt.toString() + "\n");
                        textArea.append(">>> " + "New UHRT: " + uhrt.toString() + "\n");
                        client.shutdownInput();
                        client.shutdownOutput();
                        client.close();
                        break;


                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
