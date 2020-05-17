package finalproject.db;

import java.sql.*;
import java.util.ArrayList;

import finalproject.entities.Person;

public class DBInterface {

	/* implementing or using this class isn't strictly required, but
	 * you might want to abstract some of the interactions with and queries
	 * to the database to a separate class.
	 */
	
	Connection conn;
	private static String url;
	
	public DBInterface(String dbFileName) throws SQLException {
		url = "jdbc:sqlite:" + dbFileName;
//		setConnection();
//		conn.close();
	}
	
	public Connection getConn() {
		return conn;
	}
	
	public void setConnection() throws SQLException {
		conn = DriverManager.getConnection(url);
	}
	
//	public static void main(String[] args) throws SQLException {
//		
//		new DBInterface();
//		
//	}
	
}


