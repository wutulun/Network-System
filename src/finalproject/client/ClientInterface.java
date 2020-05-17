package finalproject.client;

import java.util.ArrayList;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import finalproject.client.ClientInterface.ComboBoxItem;
import finalproject.db.DBInterface;
import finalproject.entities.Person;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.Charset;

public class ClientInterface extends JFrame {

	private static final long serialVersionUID = 1L;

//	public static final int DEFAULT_PORT ;
	String host = "localhost";
	Socket socket;
	int port = 8001;
	
	private static final int FRAME_WIDTH = 600;
	private static final int FRAME_HEIGHT = 450;
	final int AREA_ROWS = 10;
	final int AREA_COLUMNS = 40;

	JFileChooser jFileChooser;
	JTextArea textQueryArea;
	JLabel dbName;
	JLabel connName;
	JButton openBtn;
	JButton closeBtn;
	JButton sendBtn;
	JButton queryBtn;
	JComboBox peopleSelect;
	
	private Connection conn;
	DataInputStream fromServer;
	ObjectOutputStream toServer;
	
	String columnNames = "";
	int numColumns;
	
	public ClientInterface() {
		
		JMenuBar menuBar = new JMenuBar();     
	    setJMenuBar(menuBar);
	    menuBar.add(createFileMenu());
	    
	    add(createControlPanel(), BorderLayout.NORTH);
	    add(createTextPanel());
	    
	    setSize(FRAME_WIDTH, FRAME_HEIGHT);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

    public JMenu createFileMenu() {
      JMenu menu = new JMenu("File");
      menu.add(createFileOpenItem());
      menu.add(createFileExitItem());
      return menu;
    }
   
    private JMenuItem createFileOpenItem() {
	   
	   JMenuItem item = new JMenuItem("Open DB");
	   
	   class OpenDBListener implements ActionListener {	
		   
	         public void actionPerformed(ActionEvent event) {   
	        	 
	        	 jFileChooser = new JFileChooser("/Users/apple/myDesktop/task/java/FinalProject");    
	        	 
	 			 int returnVal = jFileChooser.showOpenDialog(getParent());
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
					String dbFilePath = jFileChooser.getSelectedFile().getAbsolutePath();
					String dbFileName = dbFilePath.substring(dbFilePath.lastIndexOf("/")+1);
					System.out.println("You chose to open this file: " + dbFileName);
					try {
						connectToDB(dbFileName);
						dbName.setText(dbFileName);
						queryBtn.addActionListener(new QueryButtonListener());
						sendBtn.addActionListener(new SendButtonListener());
						fillComboBox();
						
					} catch (Exception e ) {
						System.err.println("error connection to db: "+ e.getMessage());
						e.printStackTrace();
						dbName.setText("<None>");
//						clearComboBox();
					}
					
				}
	        }
	   }
	   item.addActionListener(new OpenDBListener());
	   return item;
    }
   
    private void connectToDB(String dbFileName) throws SQLException {
    	
    	DBInterface db = new DBInterface(dbFileName);	
    	db.setConnection();
    	conn = db.getConn();
    	
    }
    
    private void fillComboBox() throws SQLException {
	   
	   List<ComboBoxItem> l = getNames();
	   peopleSelect.setModel(new DefaultComboBoxModel(l.toArray()));
		 
    }
   
    private JMenuItem createFileExitItem() {
	   
	   JMenuItem item = new JMenuItem("Exit");
	   item.addActionListener((e) -> System.exit(0));
	   return item;
	   
    }
   
    private JPanel createControlPanel() {
    	
    	JPanel panel = new JPanel();
    	
    	panel.setLayout(new GridLayout(5, 1));
    	
    	JPanel subPanel1 = new JPanel();
    	JLabel lbl1 = new JLabel("DB: ");
    	dbName = new JLabel("<None>");
    	subPanel1.add(lbl1);
    	subPanel1.add(dbName);
    	
    	JPanel subPanel2 = new JPanel();
    	JLabel lbl2 = new JLabel("Connection: ");
    	connName = new JLabel("<None>");
    	subPanel2.add(lbl2);
    	subPanel2.add(connName);
    	
    	JPanel subPanel3 = new JPanel();
    	peopleSelect = new JComboBox();
    	peopleSelect.addItem("<Empty>");
    	subPanel3.add(peopleSelect);
    	
    	JPanel subPanel4 = new JPanel();
    	openBtn = new JButton("Open Connection");
    	closeBtn = new JButton("Close Connection");
    	subPanel4.add(openBtn);
    	subPanel4.add(closeBtn);
    	openBtn.addActionListener(new OpenButtonListener());
    	closeBtn.addActionListener(new CloseButtonListener());
    	
    	JPanel subPanel5 = new JPanel();
    	sendBtn = new JButton("Send");
    	queryBtn = new JButton("Query");
    	subPanel5.add(sendBtn);
    	subPanel5.add(queryBtn);
    	
    	panel.add(subPanel1);
    	panel.add(subPanel2);
    	panel.add(subPanel3);
    	panel.add(subPanel4);
    	panel.add(subPanel5);
    	
    	return panel;
    	
    }
    
    private JPanel createTextPanel() {
    	
    	JPanel panel = new JPanel();
    	
    	textQueryArea = new JTextArea(AREA_ROWS, AREA_COLUMNS);
    	textQueryArea.setEditable(false);
    	panel.add(textQueryArea);
    	
    	return panel;
    	
    }
    
    class OpenButtonListener implements ActionListener {
    	
    	public void actionPerformed(ActionEvent e) {
    		
    		try {
				socket = new Socket(host, port);
				fromServer = new DataInputStream(socket.getInputStream());
	        	toServer = new ObjectOutputStream(socket.getOutputStream());	
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		
    		connName.setText(host + ":" + port);
    		
    	}
    	
    }
    
    class CloseButtonListener implements ActionListener {
    	
    	public void actionPerformed(ActionEvent e) {
    		
    		try {
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		
    	}
    	
    }
    
    class SendButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {

	        try {
	        	// responses are going to come over the input as text
	        	// BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));		
				
				// now, get the person on the object dropdownbox we've selected
				ComboBoxItem personEntry = (ComboBoxItem)peopleSelect.getSelectedItem();
				
				// get a "Person" object
				String queryString = "select first, last, age, city from people where id = ?";
				PreparedStatement preparedStatement = conn.prepareStatement(queryString);
				preparedStatement.setInt(1, personEntry.getId());
				ResultSet rset = preparedStatement.executeQuery();
				Person p = new Person(rset.getString("first"), rset.getString("last"), rset.getInt("age"), 
									  	rset.getString("city"), personEntry.getId());
				
				// send the person object
				toServer.writeObject(p);
				toServer.flush();
				
				// read response from server
				String response = fromServer.readUTF();

				if (response.contains("Success")) {
					System.out.println("Success");
					// update 'sent' of that person to be 1
					Statement updateStmt = conn.createStatement();
					updateStmt.executeUpdate("update people set sent = 1 where id = " + personEntry.getId());
					fillComboBox();
				} else {
					System.out.println("Failed");
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        
			
		}
		
	}
	
    class QueryButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			
			try {			
				textQueryArea.setText("");
				
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
				
				textQueryArea.setText(rowString);
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
    		
    }
    
    private List<ComboBoxItem> getNames() throws SQLException {
    	
    	List<ComboBoxItem> cbis = new ArrayList<>();
    	
    	Statement selectName = conn.createStatement();
		ResultSet rset = selectName.executeQuery("select first, last, id from people where sent = 0");
		
		while (rset.next()) {
			ComboBoxItem cbi = new ComboBoxItem(rset.getInt(3), rset.getString(1)+" "+rset.getString(2));  
			cbis.add(cbi);
		}
		
	    return cbis;
    }
	
	// a JComboBox will take a bunch of objects 
    // and use the "toString()" method of those objects to print out what's in there. 
    class ComboBoxItem {
		private int id;
		private String name;
		
		public ComboBoxItem(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public int getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String toString() {
			return this.name;
		}
	}
	
    public static void main(String[] args) {
		ClientInterface ci = new ClientInterface();
		ci.setVisible(true);
    }
}
