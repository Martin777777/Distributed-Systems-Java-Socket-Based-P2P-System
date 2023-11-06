import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class Peer extends JFrame implements ActionListener, Runnable {
    Socket socket = null;
    JLabel l1,l2,l3,l4,l5,l6,jtf3;
    JTextField jtf1,jtf2,jtf4,jtf5;
    TextArea ta;
    InetAddress host;
    Message msg = new Message();
    int port = 5000;
    Thread t = null;
    JButton jb,jb2,jb3;

    public Peer(String s)
    {
        super(s);

        Myadapter myadapter = new Myadapter(this);
        addWindowListener(myadapter);

        this.setLayout(new FlowLayout());
        this.setSize(600,425);
        this.setResizable(false);
        this.setVisible(true);

        l5 = new JLabel("Enter IP : ");
        add(l5);
        jtf4 = new JTextField(15);
        add(jtf4);
        jtf4.setText("127.0.0.1");

        add(new JLabel("                          "));

        l6 = new JLabel("Enter Port : ");
        add(l6);
        jtf5 = new JTextField(15);
        add(jtf5);
        jtf5.setText("5000");

        l1 = new JLabel("Nick Name :  ");
        add(l1);
        jtf1 = new JTextField(15);
        add(jtf1);

        jb2 = new JButton("Connect");
        add(jb2);
        jb2.addActionListener(this);

        jb3 = new JButton("Disconnect");
        add(jb3);
        jb3.addActionListener(this);
        jb3.setEnabled(false);

        add(new JLabel("                                        "));

        l2 = new JLabel("Message : ");
        add(l2);
        jtf2 = new JTextField(34);
        add(jtf2);
        jtf2.setEditable(false);

        jb = new JButton("Send Message");
        add(jb);
        jb.addActionListener(this);
        jb.setEnabled(false);

        l3 = new JLabel("Status : ");
        add(l3);
        jtf3 = new JLabel("Not connected to the server...");
        add(jtf3);

        add(new JLabel("                                                                            "));

        l4 = new JLabel("Recieved Messages : ");
        add(l4);
        ta = new TextArea("",15,80);
        add(ta);
        ta.setFont(Font.getFont("verdana"));
        ta.setBackground(Color.ORANGE);
        ta.setEditable(false);

        jtf3.setText("Not connected to Server, click connect");
    }

    public void actionPerformed(ActionEvent ae)
    {
        try{
            String str = ae.getActionCommand();

            if(str.equals("Disconnect"))
            {
                try
                {
                    jb.setEnabled(false);
                    jtf2.setEditable(false);
                    jb2.setEnabled(true);
                    jb3.setEnabled(false);
                    jtf4.setEditable(true);
                    jtf5.setEditable(true);
                    jtf1.setEditable(true);
                    socket.close();
                    socket = null;
                }
                catch(Exception e)
                {}
            }

            if(str.equals("Send Message"))
            {
                msg.senderID = jtf1.getText();

                msg.msgText = jtf2.getText();
                jtf2.setText("");

                if(!msg.senderID.equals("") && !msg.msgText.equals(""))
                {
                    sendData();
                }
                else
                    jtf3.setText("Message was not sent, type a message");
            }

            if(str.equals("Connect"))
            {
                try{
                    host = InetAddress.getByName(jtf4.getText());
                    String p = jtf5.getText();

                    try{
                        if(socket!=null)
                        {
                            socket.close();
                            socket = null;
                        }
                    }
                    catch(Exception e)
                    {}

                    if(!jtf1.getText().equals(""))
                    {
                        socket = new Socket(host,Integer.parseInt(p));

                        ObjectOutputStream obj = new ObjectOutputStream(socket.getOutputStream());
                        msg.senderID = jtf1.getText();
                        msg.msgText = " is now online at "+new Date().toString();
                        obj.writeObject(msg);

                        jtf2.setEditable(true);
                        jb.setEnabled(true);
                        jb2.setEnabled(false);
                        jb3.setEnabled(true);
                        jtf4.setEditable(false);
                        jtf5.setEditable(false);
                        jtf1.setEditable(false);

                        jtf3.setText("Connection established with Server, start chatting");

                        t = new Thread(this,"Reading");
                        t.start();
                    }
                }
                catch(Exception e)
                {
                    jtf3.setText("Could not connect to Server, connect again");
                }
            }
        }
        catch(Exception e)
        {
            jtf3.setText("Action Error");
        }
    }

    public void sendData()
    {
        try
        {
            ObjectOutputStream obj = new ObjectOutputStream(socket.getOutputStream());
            if(!msg.senderID.equals("") && !msg.msgText.equals(""))
            {
                obj.writeObject(msg);
                jtf3.setText("Message was sent successfully");
            }
            msg.senderID = "";
            msg.msgText = "";
        }
        catch(Exception e)
        {
            jtf3.setText("Error occured while sending message");
        }
    }


    public void run()
    {
        try
        {

            while(true)
            {
                ObjectInputStream obj = new ObjectInputStream(socket.getInputStream());
                Message msg = new Message();
                msg = (Message) obj.readObject();
                if(msg.senderID!=null && msg.msgText!=null)
                    ta.append(msg.senderID+" >> "+msg.msgText+"\n");

            }

        }
        catch(Exception e)
        {
            jtf2.setEditable(false);
            jb2.setEnabled(true);
            jb.setEnabled(false);
            jb3.setEnabled(false);
            jtf4.setEditable(true);
            jtf5.setEditable(true);
            jtf1.setEditable(true);
            jtf3.setText("Connection Lost");
        }
    }

}

class Message implements Serializable {
    public volatile String senderID;
    public volatile String msgText;
}

class Myadapter extends WindowAdapter
{
    Peer f;
    public Myadapter(Peer j)
    {
        f = j;
    }
    public void windowClosing(WindowEvent we)
    {
        f.setVisible(false);
        try{
            f.socket.close();
            f.dispose();
        }
        catch(Exception e)
        {
        }
        System.exit(0);
    }
}
