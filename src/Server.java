

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
	private EncryptionKeys keys;
	private boolean keysSent = false;
	private JScrollPane chatPane;
	private JScrollPane textPane;
	private JButton send;
	
	//constructor
	public Server(){
		super("Instant Messenger Server");

		initComponents();
		keys = new EncryptionKeys(1024);
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
                sendEncryptedMessage(userText.getText());
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
			output.writeObject("n" + keys.nLocal);
			output.flush();
		}catch(IOException IOEx){
			chatWindow.append("\n ERROR - Message sending error.");
		}
		try{
			output.writeObject("e" + keys.eLocal);
			output.flush();
		}catch(IOException IOEx){
			chatWindow.append("\n ERROR - Message sending error.");
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
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("\nOnline");
	}
	//during conversation
	private void whileChatting()throws IOException{
		ableToType(true);
		String message = "";
		do{
			try{
				message = (String) input.readObject();
				if(!keysSent){
					if(message.substring(0, 1).equals("n")){
						try{
						keys.nForeign = new BigInteger(message.substring(1, message.length()));
						}catch(NumberFormatException nfEx){
							showMessage("\nError sending keys");
						}
					}
					else if(message.substring(0, 1).equals("e")){
						try{
						keys.eForeign = new BigInteger(message.substring(1, message.length()));
						keysSent = true;
						}catch(NumberFormatException nfEx){
							showMessage("\nError sending keys");
						}
					}
					
					continue;
				}
				showEncryptedMessage(message);
			}catch(ClassNotFoundException cnfEx){
				showMessage("\nUser sending error. ");
			}catch(SocketException sockEx){
				showMessage("\nLost connection");
				return;
			}
		}while(!message.equals("END"));
	}
	
	//close streams and sockets after done
	private void closeDown(){
		showMessage("\nClosing connections... \n");
		ableToType(false);
		try{
			output.close();
			input.close();
			connection.close();
		}catch(IOException ioEx){
			ioEx.printStackTrace();
		}
	}

	//send a message to client

	private void sendEncryptedMessage(String message){
		if(message.equals("")){
			return;
		}
		try{
			
			String x = RSA.encrypt(message, keys.nForeign, keys.eForeign);
			output.writeObject(x);
			output.flush();
			showMessage("\nSERVER - " + message);
		}catch(IOException IOEx){
			chatWindow.append("\n ERROR - Message sending error.");
		}catch(Exception e){
			chatWindow.append("\nEncryption error");
			e.printStackTrace();
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
	private void showEncryptedMessage(final String text){
		SwingUtilities.invokeLater(
				new Runnable(){
					public void run(){
						try{
							chatWindow.append("\nCLIENT - " + RSA.decrypt(text, keys.nLocal, keys.d));
						}catch(Exception e){
							chatWindow.append("Decryption error");
						}
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
