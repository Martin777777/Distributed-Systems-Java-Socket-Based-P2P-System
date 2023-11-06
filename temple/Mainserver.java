import sun.applet.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class data
{
    public volatile int signal;
    public volatile int count;
}

public class Mainserver extends JFrame implements Runnable, ActionListener {
    Thread t;
    JButton a,b;
    JTextField jtf2;
    JLabel jtf;
    TextArea ta;


    ServerSocket server;
    Message msg = new Message();
    data flag = new data();

    Socket count[] = new Socket[5];
    int cj = 5;
    int cl = 0;




    Mainserver(String s)
    {
        super(s);

        flag.signal = 0;
        flag.count = 0;


        JLabel l3 = new JLabel("Enter Port No. : ");
        add(l3);

        jtf2 = new JTextField(7);
        jtf2.setText("5000");
        add(jtf2);

        JLabel l1 = new JLabel("Start the Server");
        add(l1);

        a = new JButton("Start");
        a.addActionListener(this);
        add(a);

        JLabel l2 = new JLabel("Stop the Server");
        add(l2);

        b = new JButton("Stop");
        b.addActionListener(this);
        add(b);
        b.setEnabled(false);

        JLabel l4 = new JLabel("Status : ");
        add(l4);

        add(new JLabel("    "));

        jtf = new JLabel("Server is not running...");
        add(jtf);

        ta = new TextArea("",15,70);
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setFont(Font.getFont("verdana"));
        add(ta);


        Mywindowadapter a = new Mywindowadapter(this);
        addWindowListener(a);


    }

    public void actionPerformed(ActionEvent ae)
    {
        try{
            String str = ae.getActionCommand();

            if(str.equals("Start"))
            {
                String str2 = jtf2.getText();
                if(!str2.equals(""))
                {
                    try
                    {
                        server = new ServerSocket(Integer.parseInt(str2));
                        jtf.setText("Server is running....");
                        jtf2.setEnabled(false);

                        a.setEnabled(false);
                        b.setEnabled(true);

                        flag.count = 0;

                        count = new Socket[5];
                        cj = 5;
                        cl = 0;

                        t = new Thread(this,"Running");
                        t.start();

                    }
                    catch(Exception e)
                    {
                        jtf.setText("Either the port no. is invalid or is in use");
                    }
                }
                else
                    jtf.setText("Enter port no.");

            }
            if(str.equals("Stop"))
            {
                try{
                    server.close();
                }
                catch(Exception ee)
                {
                    jtf.setText("Error closing server");
                }
                jtf.setText("Server is closed");
                jtf2.setEnabled(true);
                a.setEnabled(true);
                b.setEnabled(false);
                server = null;
                t = null;

                for(int i=0;i<flag.count;i++)
                {
                    try{
                        count[i].close();
                    }
                    catch(Exception e)
                    {
                    }
                }

            }
        }
        catch(Exception ex)
        {
        }
    }

    public void run() {


        while (true) {
            if (server.isClosed())
                return;

            try {

                Socket client = server.accept();

                ObjectInputStream obj = new ObjectInputStream(client.getInputStream());
                msg = (Message) obj.readObject();

                ta.append(msg.senderID + " >> " + msg.msgText + "\n");

                if (cl < cj) {
                    count[cl] = client;
                    cl++;
                } else {
                    Socket temp[] = new Socket[cj];
                    for (int i = 0; i < cj; i++) {
                        temp[i] = count[i];
                    }

                    count = new Socket[cj + 5];
                    for (int i = 0; i < cj; i++) {
                        count[i] = temp[i];
                    }
                    count[cj] = client;
                    cj = cj + 5;
                    cl++;
                }

                flag.count = cl;

                for (int i = 0; i < flag.count; i++) {
                    try {
                        ObjectOutputStream objw = new ObjectOutputStream(count[i].getOutputStream());
                        objw.writeObject(msg);
                    } catch (Exception e) {
                    }
                }

                new newthread(client, msg, flag, count, this, server);

            } catch (Exception e) {
                jtf.setText("Server is stopped");
                jtf2.setEnabled(true);
                try {
                    server.close();
                } catch (Exception ey) {
                    jtf.setText("Error closing server");
                }

            }


        }
    }

        class newthread implements Runnable
        {
            Thread t;
            Socket client;
            Message msg;
            data flag;
            Socket count[];
            Mainserver f;
            ServerSocket server;

            newthread(Socket client, Message msg, data flag, Socket count[], Mainserver f, ServerSocket server)
            {
                t = new Thread(this,"Client");
                this.server = server;
                this.client = client;
                this.msg = msg;
                this.f = f;
                this.flag = flag;
                this.count = count;
                t.start();
            }

            public void run()
            {
                String name = msg.senderID;
                try
                {
                    while(server.isClosed()!=true)
                    {
                        ObjectInputStream obj = new ObjectInputStream(client.getInputStream());
                        msg = (Message)obj.readObject();
                        if(msg.senderID!=null && msg.msgText!=null)
                        {
                            f.ta.append(msg.senderID+" >> "+msg.msgText+"\n");
                        }
                        name = msg.senderID;

                        for(int i=0;i<flag.count;i++)
                        {
                            try{
                                ObjectOutputStream objw = new ObjectOutputStream(count[i].getOutputStream());
                                objw.writeObject(msg);
                            }catch(Exception e)
                            {}
                        }
                    }

                    if(server.isClosed())
                    {
                        for(int i=0;i<flag.count;i++)
                        {
                            try{
                                count[i].close();
                            }
                            catch(Exception e)
                            {
                            }
                        }
                    }

                }
                catch(Exception e)
                {
                    f.ta.append(name+" is offline\n");
                    try
                    {
                        msg.msgText = " is offline\n";
                        for(int i=0;i<flag.count;i++)
                        {
                            try{
                                ObjectOutputStream objw = new ObjectOutputStream(count[i].getOutputStream());
                                objw.writeObject(msg);
                            }catch(Exception ex)
                            {}
                        }
                        client.close();
                    }
                    catch(Exception ex)
                    {

                    }
                }
            }
        }



class Mywindowadapter extends WindowAdapter
{
    Mainserver f;
    public Mywindowadapter(Mainserver j)
    {
        f = j;
    }
    public void windowClosing(WindowEvent we)
    {
        f.setVisible(false);
        try{
            f.server.close();
        }
        catch(Exception e)
        {
        }
        f.dispose();
        System.exit(0);
    }
}

    }

