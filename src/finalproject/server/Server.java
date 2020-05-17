package finalproject.server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import finalproject.db.DBInterface;
import finalproject.entities.Person;

public class Server extends JFrame implements Runnable {
	
	private Connection conn;
	ObjectInputStream FromClient;
	DataOutputStream ToClient;

	public static final int DEFAULT_PORT = 8001;
	private static final int FRAME_WIDTH = 700;
	private static final int FRAME_HEIGHT = 800;
	final int AREA_ROWS = 40;
	final int AREA_COLUMNS = 50;
	int port;
	String columnNames = "";
	int numColumns;
	
	JTextArea ta;
	JLabel dbName;
	
	private int clientNo = 0;

	public Server() throws IOException, SQLException {
		this(DEFAULT_PORT, "server.db");
	}
	
	public Server(String dbFile) throws IOException, SQLException {
		this(DEFAULT_PORT, dbFile);
	}

	public Server(int port, String dbFile) throws IOException, SQLException {
		
		this.port = port; 
		connectToDB(dbFile);
		
		JMenuBar menuBar = new JMenuBar();     
	    setJMenuBar(menuBar);
	    menuBar.add(createFileMenu());
	    
	    add(createControlPanel(), BorderLayout.NORTH);
	    add(createTextPanel());
	    
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Thread t = new Thread(this);
	    t.start();
		
	}
	
	private JMenu createFileMenu() {
		
		JMenu menu = new JMenu("File");
	      menu.add(createFileExitItem());
	      return menu;
		
	}
	
	private JMenuItem createFileExitItem() {
		   
		   JMenuItem item = new JMenuItem("Exit");
		   item.addActionListener((e) -> System.exit(0));
		   return item;
		   
	}

	private JPanel createControlPanel() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		
		JPanel subPanel1 = new JPanel();
    	JLabel lbl = new JLabel("DB: ");
    	subPanel1.add(lbl);
    	subPanel1.add(dbName);
    	
    	JPanel subPanel2 = new JPanel();
    	JButton btn  = new JButton("Query");
    	subPanel2.add(btn);
    	btn.addActionListener(new QueryButtonListener());
    	
    	panel.add(subPanel1);
    	panel.add(subPanel2);
    	
    	return panel;
		
	}
	
	private JPanel createTextPanel() {
    	
    	JPanel panel = new JPanel();
    	
    	ta = new JTextArea(AREA_ROWS, AREA_COLUMNS);
    	ta.setEditable(false);
    	JScrollPane sp = new JScrollPane(ta);
    	panel.add(sp);
    	
    	return panel;
    	
    }
	
	class QueryButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			
			try {			
				
				Statement selectAll = conn.createStatement();
				ResultSet rset = selectAll.executeQuery("select * from people");
				
				// get column nums and names
				if (columnNames == "") {
					ResultSetMetaData rsmd = rset.getMetaData();
					numColumns = rsmd.getColumnCount();	
					for (int i=1; i<=numColumns; i++) {
						columnNames += rsmd.getColumnName(i) + "\t";
				    }
				}
				
				String rowString = "" + columnNames + "\n";
				while (rset.next()) {
					for (int i=1; i<=numColumns; i++) {
						Object o = rset.getObject(i);
						rowString += o.toString() + "\t";
				}
					rowString += "\n";
				}
				
				ta.append(rowString);
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
    		
    }
	
	public static void main(String[] args) throws IOException, SQLException {

		Server sv = new Server();
		sv.setVisible(true);
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			ServerSocket serverSocket = new ServerSocket(port);
			ta.append("Listening on port " + port + "\n");
			
			while (true) {
				
				Socket socket = serverSocket.accept();
				
				clientNo++;
				
				ta.append("Starting thread for client " + clientNo + " at " + new Date() + '\n');

			    InetAddress inetAddress = socket.getInetAddress();
			    ta.append("Client " + clientNo + "'s host name is " + inetAddress.getHostName() + "\n");
			    ta.append("Client " + clientNo + "'s IP Address is " + inetAddress.getHostAddress() + "\n");
			    
			    
			    new Thread(new HandleAClient(socket, clientNo)).start();
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class HandleAClient implements Runnable {
		
		private Socket socket;
		private int clientNum;
		
		public HandleAClient(Socket socket, int clientNum) {
		      this.socket = socket;
		      this.clientNum = clientNum;
		}
		
		public void run() {
			 
			try {
				
				FromClient = new ObjectInputStream(socket.getInputStream());
				ToClient = new DataOutputStream(socket.getOutputStream());
				
				while (true) {
					
					Object object = FromClient.readObject();
					Person p = (Person)object;
					ta.append("Received " + p.toString() + " from client" + this.clientNum + "\n");
					
					// insert into db
					Statement insertstmt = conn.createStatement();
					insertstmt.executeUpdate("insert into people values (" 
											+ "'" + p.getFirst() + "'" + ", " + "'" + p.getLast() + "'" + ", " 
											+ p.getAge() + ", " + "'" + p.getCity() + "'" + ", " + "1" + ", "
											+ p.getID() + ")");
					ta.append("Insert Successfully!\n");
					
					ToClient.writeUTF("Success\n");
					ToClient.flush();
						
					Thread.sleep(1);
					
				}
				
			} catch (IOException e) {
				try {
					ToClient.writeUTF("Failed\n");
					ToClient.flush();
					ta.append("Connection Ended.\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				try {
					ToClient.writeUTF("Failed\n");
					ToClient.flush();
					ta.append("Receiving Failed.\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (InterruptedException e) {
				try {
					ToClient.writeUTF("Failed\n");
					ToClient.flush();
					ta.append("Error Occured.\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (SQLException e) {
				try {
					ToClient.writeUTF("Failed\n");
					ToClient.flush();
					ta.append("DB Error.\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			 
		 }
	}
	
	private void connectToDB(String dbFileName) throws SQLException {
    	
    	DBInterface db = new DBInterface(dbFileName);	
    	db.setConnection();
    	conn = db.getConn();
    	dbName = new JLabel(dbFileName);
    		
    }
}
