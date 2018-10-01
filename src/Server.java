

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class Server extends JFrame{
    /**
     *
     */
    private static final long serialVersionUID = -3018500266653593178L;
    private JTextArea userText;
    private JTextArea chatWindow;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;
    private int keyLength = 256;
    private boolean keysSent = false;
    private JScrollPane chatPane;
    private JScrollPane textPane;
    private JButton send;

    private RSA rsa;

    //constructor
    public Server(){
        super("Instant Messenger Server");

        initComponents();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        setVisible(true);
    }
    private void initComponents() {

        chatPane = new javax.swing.JScrollPane();
        textPane = new javax.swing.JScrollPane();

        chatWindow = new javax.swing.JTextArea();
        send = new javax.swing.JButton();
        userText = new javax.swing.JTextArea();
        userText.setLineWrap(true);
        userText.setEditable(false);
        textPane.setViewportView(userText);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        chatWindow.setEditable(false);
        chatWindow.setColumns(20);
        chatWindow.setRows(5);
        chatPane.setViewportView(chatWindow);

        send.setText("Send");
        send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendMessage(userText.getText());
                userText.setText("");
            }
        });

        userText.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send.doClick();


                }

            }

        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(chatPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(textPane)
                                .addGap(18, 18, 18)
                                .addComponent(send)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(chatPane, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(textPane, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(send, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(4, 23, Short.MAX_VALUE))
        );
        pack();
        send.setEnabled(false);
    }


    //set up server
    public void startRunning(){
        try{
            server = new ServerSocket(6789, 100);
            while(true){
                try{
                    //connect and have conversation

                    waitForConnection();
                    setupStreams();
                    exchangeKeys();
                    whileChatting();
                }catch(EOFException eofEx){
                    showMessage("\nServer ended the connection. ");
                }finally{
                    closeDown();
                }
            }
        }catch(IOException ioEx){
            ioEx.printStackTrace();
        }
    }
    //send keys
    private void exchangeKeys(){
        try{
            output.writeObject("n" + rsa.nLocal);
            output.flush();
        }catch(IOException IOEx){
            chatWindow.append("\nERROR - Message sending error.");
        }
        try{
            output.writeObject("e" + rsa.eLocal);
            output.flush();
        }catch(IOException IOEx){
            chatWindow.append("\nERROR - Message sending error.");
        }
    }

    //wait for connection, then display connection info
    private void waitForConnection() throws IOException{
        showMessage("Waiting for someone to connect...");
        connection = server.accept();
        showMessage("\nConnected to " + connection.getInetAddress().getHostName());
    }


    //get stream to send and receive data
    private void setupStreams() throws IOException{
        rsa = new RSA(keyLength);
        output = new ObjectOutputStream(connection.getOutputStream());
        output.flush();
        input = new ObjectInputStream(connection.getInputStream());
        showMessage("\nOnline");
    }
    //during conversation
    private void whileChatting()throws IOException{
        ableToType(true);
        boolean connected = true;

        do{
            try{
                if(!keysSent){
                    String message = (String) input.readObject();
                    if(message.substring(0, 1).equals("n")){
                        try{
                            rsa.nForeign = new BigInteger(message.substring(1, message.length()));
                        }catch(NumberFormatException nfEx){
                            showMessage("\nError sending keys");
                        }
                    }
                    else if(message.substring(0, 1).equals("e")){
                        try{
                            rsa.eForeign = new BigInteger(message.substring(1, message.length()));
                            keysSent = true;
                        }catch(NumberFormatException nfEx){
                            showMessage("\nError sending keys");
                        }
                    }
                    continue;
                }
                else{
                    ArrayList<String> message = (ArrayList<String>) input.readObject();

                    if(message.isEmpty()){
                        continue;
                    }

                    String decryptedMessage = rsa.decrypt(message);

                    if(decryptedMessage.equals("END")){
                        connected = false;
                    }
                    showMessage("\nCLIENT - " + decryptedMessage);
                }




            }catch(ClassNotFoundException cnfEx){
                showMessage("\nUser sending error. ");
            }catch(SocketException sockEx){
                showMessage("\nLost connection");
                return;
            }
        }while(connected);
    }

    //close streams and sockets after done
    private void closeDown(){
        showMessage("\nClosing connections... \n");
        ableToType(false);
        try{
            output.close();
            input.close();
            connection.close();
            keysSent = false;
        }catch(IOException ioEx){
            ioEx.printStackTrace();
        }
    }

    private String trimString(String s){
        String ret = s;
        ret.trim();
        while(ret.length() > 1 && ret.charAt(0) == '\n'){
            ret = ret.substring(1);
        }

        if(ret.length() == 1 && ret.charAt(0) == '\n'){
            ret = "";
        }
        return ret;

    }

    //send a message to client

    private void sendMessage(String message){
        message = trimString(message);
        if(message.equals("")){
            return;
        }

        try{


            output.writeObject(rsa.encrypt(message));
            output.flush();
            showMessage("\nSERVER - " + message);
        }catch(IOException IOEx){
            chatWindow.append("\n ERROR - Message sending error.");
        }

    }

    //updates chatWindow
    private void showMessage(final String text){
        SwingUtilities.invokeLater(
                new Runnable(){
                    public void run(){
                        chatWindow.append(text);

                    }
                }
        );
    }



    //let user type
    private void ableToType(final boolean b){
        SwingUtilities.invokeLater(
                new Runnable(){
                    public void run(){
                        send.setEnabled(b);
                        userText.setEditable(b);
                    }
                }
        );
    }

}
