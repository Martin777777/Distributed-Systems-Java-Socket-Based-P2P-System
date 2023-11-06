import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * This class represents the peer in this system.
 *
 * In this system, each peer is able to open its server, close its server, get resource from other peers, add new resources and remove new resources
 *
 * More details about the functions and operations of the peer is shown below
 *
 * @author Ma Zixiao
 *
 */

public class Peer extends JFrame implements Runnable {
    String ip;
    int port;
    int serialNumber;
    String guid;
    ServerSocket serverSocket;
    HashMap<String, String[]> dhrt;
    HashMap<String, String> ownedResources;
    boolean isOpen;
    boolean threadOver;
    MyThread serverThread;
    PeerAdapter adapter;

    JPanel panel;
    JButton open;
    JButton close;
    JButton requestResource;
    JButton addFile;
    JButton removeFile;
    TextArea textArea;

    public Peer(String ip, int port, int serialNumber) {
        this.port = port;
        this.ip = ip;
        this.serialNumber = serialNumber;
        dhrt = new HashMap<>();
        ownedResources = new HashMap<>();
        isOpen = false;
        this.initFrame();
        this.initTables();
        this.open();
        this.setVisible(true);
    }

    // add the resources owned by the resource into the table that records the owned resources
    private void initTables() {
        File dicFile = new File("resources\\resource_" + serialNumber);
        File[] files = dicFile.listFiles();
        if (files != null){
            Arrays.stream(files).forEach(file -> {
                String resourceGUID = GUIDGeneration.generateResourceGUID(file.getPath());
                ownedResources.put(resourceGUID, file.getAbsolutePath());

            });
        }
    }

//    initialize the window, buttons and the text area
    private void initFrame() {
        this.setSize(600, 500);
        this.setTitle("Peer" + serialNumber);

        panel = new JPanel();

        open = new JButton("Open");
        close = new JButton("Close");
        requestResource = new JButton("Get Resource");
        addFile = new JButton(("Add File"));
        removeFile = new JButton("Remove File");
        textArea = new TextArea(23, 70);
        textArea.setBackground(Color.CYAN);

        open.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open();
            }
        });

        close.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        addFile.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFileAndAdd();
            }
        });

        removeFile.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFileAndRemove();
            }
        });

        requestResource.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAndChooseResources();
            }
        });

        panel.add(open);
        panel.add(close);
        panel.add(addFile);
        panel.add(removeFile);
        panel.add(requestResource);
        panel.add(textArea);
        textArea.setEditable(false);

        this.add(panel);

        adapter = new PeerAdapter(this);
        this.addWindowListener(adapter);


    }

    //this method is to open the server of a peer
    private boolean open() {
        try {
            long beginTime = System.currentTimeMillis();

            Socket informSocket = new Socket(InetAddress.getByName(MainServer.IP), MainServer.PORT);
            DataOutputStream outputStream = new DataOutputStream(informSocket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(informSocket.getInputStream());

            //0 means informing connection
            //here the peer adds its ip address and port number as well as the GUID of all its resources to the informing message
            StringBuilder informData = new StringBuilder("0||" + ip + "||" + port + "||");
            Object[] ids = ownedResources.keySet().toArray();
            for (int i = 0; i < ids.length; i++) {
                String id = (String) ids[i];
                if (i != ownedResources.size()-1) {
                    informData.append(id).append("||");
                }
                else {
                    informData.append(id);
                }
            }

            //the peer is allowed to open its server, then it opens the server socket and remembers its GUID
            outputStream.writeUTF(informData.toString());
            String[] backArray = inputStream.readUTF().split("\\|\\|");
            if (backArray[0].equals("Agree Open")) {
                this.guid = backArray[1];
                isOpen = true;
                threadOver = false;
                serverSocket = new ServerSocket(port);
                serverThread = new MyThread(this);
                serverThread.start();
                informSocket.shutdownOutput();
                informSocket.shutdownInput();
                informSocket.close();

                textArea.append(">>> Open and inform the main server successfully! The GUID for this peer is :" + this.guid + "\n");

                long endTime = System.currentTimeMillis();
                textArea.append(">>> Time used: "+(int)((float)(endTime - beginTime)/1000) + " s " + (float)(endTime - beginTime)%1000 + " ms" +"\n");

                open.setEnabled(false);
                close.setEnabled(true);
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //this method is to show the list of resources that's in the system but not owned by the peer
    private void showAndChooseResources() {
        JFrame resourcesWindow = new JFrame();
        JPanel mainPanel = new JPanel(null);
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        resourcesWindow.setSize(600,800);
        resourcesWindow.setTitle("Choose the resource to get");
        resourcesWindow.setLocationRelativeTo(null);
        resourcesWindow.setResizable(false);
        resourcesWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buttonPanel.setBounds(10, 10, 560, 730);

        mainPanel.setBounds(0, 0, 600, 800);

        AtomicInteger num = new AtomicInteger();

        ArrayList<String> allResources = Resources.getAllResources();

        allResources.parallelStream().forEach(resource -> {
            if (!ownedResources.containsKey(resource)){
                JButton button = new JButton(resource);
                button.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getResource(button.getText());
                        button.setEnabled(false);
                    }
                });
                buttonPanel.add(button);
                num.addAndGet(1);
            }
        });



        if (num.get() != 0){
            mainPanel.add(buttonPanel);
        }
        else {
            JLabel label = new JLabel("All the resources have already in your dictionary!");
            label.setBounds(100, 300, 400, 200);
            mainPanel.add(label);
        }
        resourcesWindow.add(mainPanel);
        resourcesWindow.setVisible(true);
    }

    //this method is to send a request to the main server to get the contact information of a peer
    private String[] getResourceOwner(String resourceID) {
        Socket clientSocket = null;
        try {
            clientSocket = new Socket(MainServer.IP, MainServer.PORT);
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream.writeUTF("1||"+resourceID);
            return inputStream.readUTF().split("\\|\\|");
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (clientSocket != null) {
                    clientSocket.shutdownOutput();
                    clientSocket.shutdownInput();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //this method is to get the resource from another peer
    //this method will first check if the resource is recorded in its DHRT
    //if it is, the peer send the request to the owner peer automatically
    //otherwise, the peer first send a request to the main server to get the contact information of the owner peer
    //when a new resource is added, the main server will be informed
    public void getResource(String resourceID) {
        new Thread(() -> {
            String[] ownerArray;
            boolean inDHRT = false;
            if (!dhrt.containsKey(resourceID)){
                textArea.append(">>> The resource hasn't been recorded in DHRT. Sending message to the main server...\n");
                ownerArray = getResourceOwner(resourceID);
                textArea.append(">>> The resource owner's IP is " + ownerArray[0] + "." + "The port number is " + ownerArray[1] + "\n");
            }
            else {
                ownerArray = new String[]{dhrt.get(resourceID)[0], dhrt.get(resourceID)[1]};
                inDHRT = true;
                textArea.append(">>> The resource has been recorded in DHRT. Sending message to the resource owner...\n");
            }
            boolean finalInDHRT = inDHRT;
            new Thread(() -> {
                Socket requestSocket = null;
                FileOutputStream fileOutputStream = null;

                try {
                    requestSocket = new Socket(ownerArray[0], Integer.parseInt(ownerArray[1]));
                    DataOutputStream outputStream = new DataOutputStream(requestSocket.getOutputStream());
                    DataInputStream inputStream = new DataInputStream(requestSocket.getInputStream());
                    outputStream.writeUTF(resourceID);

                    String fileName = inputStream.readUTF();

                    if (!fileName.equals("Not Own")){
                        textArea.append(">>> The file name of the resource is " + fileName + ".\n");
                        long l = inputStream.readLong();
                        for (String key : ownedResources.keySet()) {
                            String ownedFileName = new File(ownedResources.get(key)).getName();
                            if (fileName.equals(ownedFileName)) {
                                String[] fileNameArray = fileName.split("\\.");
                                fileName = resourceID + "." + fileNameArray[fileNameArray.length - 1];
                                textArea.append(">>> One of the owned resources has the same name, the file name is changed to " + fileName + ".\n");
                                break;
                            }
                        }
                        File file = new File("resources\\resource_" + serialNumber + "\\" + fileName);

                        fileOutputStream = new FileOutputStream(file);

                        byte[] buffer = new byte[1024];

                        long progress = 0;
                        int len;
                        long beginTime = System.currentTimeMillis();
                        while ((len = inputStream.read(buffer, 0, buffer.length)) > 0) {
                            fileOutputStream.write(buffer, 0, len);
                            fileOutputStream.flush();
                            progress += len;
                        }

                        if (progress == l) {
                            long endTime = System.currentTimeMillis();
                            textArea.append(">>> " + fileName + " received successfully!\n");
                            textArea.append(">>> Time used: "+(int)((float)(endTime - beginTime)/1000) + " s " + (float)(endTime - beginTime)%1000 + " ms" +"\n");
                            if (!finalInDHRT) {
                                dhrt.put(resourceID, new String[]{ownerArray[0], ownerArray[1], fileName});
                                textArea.append(">>> DHRT updated successfully!\n");
                                textArea.append(">>> DHRT Updated\n");
                            }
                            informAdd(resourceID);

                            textArea.append(">>> The main server is informed successfully!\n");

                            ownedResources.put(resourceID, file.getAbsolutePath());
                        } else {
                            textArea.append(">>> " + fileName + " received error!\n");
                            file.delete();
                        }
                    }
                    else {
                        dhrt.remove(resourceID);
                        textArea.append(">>> The peer stored doesn't have that resource now. DHPT updated and sending request to main server...\n");
                        getResource(resourceID);
                    }

                } catch (IOException e) {
                    dhrt.remove(resourceID);
                    textArea.append(">>> The peer may have closed its server. DHPT updated and sending request to main server...\n");
                    getResource(resourceID);
                }
                finally {
                    if (requestSocket != null) {
                        try {
                            requestSocket.shutdownOutput();
                            requestSocket.shutdownInput();
                            requestSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

        }).start();
    }

//    this method shows the window and lets the peer choose the file to add
    public void chooseFileAndAdd() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            addFile(file);
        }

    }

    // in this method, the file chosen is copied into the peer's dictionary
    private void addFile(File file) {
        String fileGUID = GUIDGeneration.generateResourceGUID(file.getAbsolutePath());
        if (ownedResources.containsKey(fileGUID)) {
            textArea.append(">>> The file is already in your dictionary!\n");
        }
        else {
            String fileName = file.getName();
            textArea.append(">>> The file name of the resource is " + fileName + ".\n");
            for (String key: ownedResources.keySet()) {
                String ownedFileName = new File(ownedResources.get(key)).getName();
                if (fileName.equals(ownedFileName)) {
                    String[] fileNameArray = fileName.split("\\.");
                    fileName = fileGUID + "." + fileNameArray[fileNameArray.length-1];
                    textArea.append(">>> One of the owned resources has the same name, the file name is changed to " + fileName + ".\n");
                    break;
                }
            }


            File newFile = new File("resources\\resource_" + serialNumber + "\\" + fileName);
            FileOutputStream outputStream = null;
            FileInputStream inputStream = null;
            long l = file.length();
            try {
                outputStream = new FileOutputStream(newFile);
                inputStream = new FileInputStream(file);
                byte[] data = new byte[1024];
                long progress = 0;
                int length;
                long beginTime = System.currentTimeMillis();
                while ((length=inputStream.read(data, 0, data.length)) != -1) {
                    outputStream.write(data, 0, length);
                    outputStream.flush();
                    progress += length;
                }

                if (progress == l){
                    long endTime = System.currentTimeMillis();
                    textArea.append(">>> " + fileName + " received successfully!\n");
                    textArea.append(">>> Time used: "+(int)((float)(endTime - beginTime)/1000) + " s " + (float)(endTime - beginTime)%1000 + " ms" +"\n");

                    this.informAdd(fileGUID);
                    ownedResources.put(fileGUID, newFile.getName());
                }
                else {
                    textArea.append(">>> " + fileName + " received error!\n");
                    newFile.delete();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    //this method is to inform the main server that a new resource is added to the peer
    private void informAdd(String newGUID) {
        new Thread(() -> {
            try {
                Socket informSocket = new Socket(MainServer.IP, MainServer.PORT);
                DataOutputStream outputStream = new DataOutputStream(informSocket.getOutputStream());
                outputStream.writeUTF("3||"+guid+"||"+newGUID);
                informSocket.shutdownOutput();
                informSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    //this method shows a window and lets the peer choose the resource to remove from its dictionary
    private void chooseFileAndRemove() {
        JFileChooser fileChooser = new JFileChooser("resources\\resource_" + serialNumber);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            removeFile(file);
        }
    }

    //this method removes the file in a peer's dictionary
    private void removeFile(File file) {
        String fileGUID = GUIDGeneration.generateResourceGUID(file.getAbsolutePath());
        if (!(ownedResources.containsKey(fileGUID) && ownedResources.get(fileGUID).equals(file.getAbsolutePath()))) {
            textArea.append(">>> The file is not in your dictionary!\n");
        }
        else {
            if(file.delete()) {
                ownedResources.remove(fileGUID);
                textArea.append(">>> Delete successfully!\n");
                this.informRemove(fileGUID);
                textArea.append(">>> The main server is informed successfully!\n");
            }


        }
    }

    //this method is to inform the main server that a resource is removed form the peer
    private void informRemove(String removeGUID) {
        new Thread(() -> {
            try {
                Socket informSocket = new Socket(MainServer.IP, MainServer.PORT);
                DataOutputStream outputStream = new DataOutputStream(informSocket.getOutputStream());
                outputStream.writeUTF("4||"+guid+"||"+removeGUID);
                informSocket.shutdownOutput();
                informSocket.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    //this method close the server of the peer
    public void close() {
        new Thread(() -> {
            try {
                Socket closeSocket = new Socket(MainServer.IP, MainServer.PORT);
                DataOutputStream outputStream = new DataOutputStream(closeSocket.getOutputStream());
                outputStream.writeUTF("5||"+guid);
                closeSocket.shutdownOutput();
                closeSocket.close();

                if (serverSocket != null && !serverSocket.isClosed()) {
                    isOpen = false;

                    serverThread.stop();
                    serverSocket.close();
                    serverSocket = null;

                    close.setEnabled(false);
                    open.setEnabled(true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    //this method keeps listenning to the resource request
    @Override
    public void run() {
        if (serverSocket.isClosed()) {
            return;
        }

        while (isOpen){
            FileInputStream fileInputStream = null;
            try {
                Socket client = serverSocket.accept();
                textArea.append(">>> Received a request.\n");
                DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
                DataInputStream inputStream = new DataInputStream(client.getInputStream());
                File file;
                String requestedID = inputStream.readUTF();
                String filePath = "";
                if (ownedResources.containsKey(requestedID)) {

                    textArea.append(">>> Sending the resource...\n");

                    filePath = ownedResources.get(requestedID);
                    byte[] data = new byte[1024];
                    file = new File(filePath);

                    outputStream.writeUTF(file.getName());
                    outputStream.flush();

                    outputStream.writeLong(file.length());
                    outputStream.flush();

                    fileInputStream = new FileInputStream(file);
                    int length;
                    while ((length=fileInputStream.read(data, 0, data.length)) != -1) {
                        outputStream.write(data, 0, length);
                        outputStream.flush();
                    }

                    textArea.append(">>> Resource sent successfully!\n");
                }
                else {
                    outputStream.writeUTF("Not Own");
                    textArea.append(">>> Not own the resource!\n");
                }
                client.shutdownInput();
                client.shutdownOutput();
                client.close();
                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                textArea.append(">>> The server is closed\n");
            }
            finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            threadOver = true;
        }


    }
}
