package com.cffreedom.apps;

import java.io.IOException;
import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.beans.DbConn;
import com.cffreedom.exceptions.FileSystemException;
import com.cffreedom.exceptions.InfrastructureException;
import com.cffreedom.utils.Convert;
import com.cffreedom.utils.SystemUtils;
import com.cffreedom.utils.Utils;
import com.cffreedom.utils.db.ConnectionManager;
import com.cffreedom.utils.db.DbUtils;
import com.cffreedom.utils.file.FileUtils;

/**
 * @author markjacobsen.net (http://mjg2.net/code)
 * Copyright: Communication Freedom, LLC - http://www.communicationfreedom.com
 * 
 * Free to use, modify, redistribute.  Must keep full class header including 
 * copyright and note your modifications.
 * 
 * If this helped you out or saved you time, please consider...
 * 1) Donating: http://www.communicationfreedom.com/go/donate/
 * 2) Shoutout on twitter: @MarkJacobsen or @cffreedom
 * 3) Linking to: http://visit.markjacobsen.net
 * 
 * Changes:
 * 2013-04-12 	markjacobsen.net	Using DbConn bean, added getDbConn(), and getConnection() 
 * 2013-05-06 	markjacobsen.net 	Just an UI into the util class ConnectionManager
 * 2013-05-09 	markjacobsen.net 	Added constructor to pass in default user/pass
 * 2013-05-23 	markjacobsen.net 	Added getDbConnWithUserInfo()
 * 2013-06-25 	markjacobsen.net 	Added option to cache connections
 */
public class DbConnManager extends ConnectionManager
{
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.apps.DbConnManager");
	private String lastType = null;
	private String lastDb = null;
	private String lastHost = null;
	private String lastPort = null;
	private String defaultUsername = SystemUtils.getUsername();
	private String defaultPassword = null;
	
	public DbConnManager() throws FileSystemException, IOException, InfrastructureException
	{
		super();
	}
	
	public DbConnManager(String file) throws FileSystemException, IOException, InfrastructureException
	{
		super(file);
	}
	
	public DbConnManager(String file, String defaultUsername, String defaultPassword) throws FileSystemException, IOException, InfrastructureException
	{
		super(file);
		logger.debug("Initializing with username and password");
		this.defaultUsername = defaultUsername;
		this.defaultPassword = defaultPassword;
	}
	
	public DbConnManager(String file, String defaultUsername, String defaultPassword, boolean cacheConnections) throws FileSystemException, IOException, InfrastructureException
	{
		super(file, cacheConnections);
		logger.debug("Initializing with username, password, and caching");
		this.defaultUsername = defaultUsername;
		this.defaultPassword = defaultPassword;
	}
	
	public static void main(String[] args) throws FileSystemException, IOException, InfrastructureException
	{
		DbConnManager dcm = new DbConnManager();
		dcm.run();
	}
	
	public Connection getConnection(String key)
	{
		while ((key == null) || (key.length() == 0) || (super.keyExists(key) == false))
		{
			super.printKeys();
			key = Utils.prompt("Key");
		}
		
		String user = Utils.prompt("Username", this.defaultUsername);
		String pass = promptForPassword();
			
		return super.getConnection(key, user, pass);
	}
	
	public DbConn getDbConnWithUserInfo(String key)
	{
		while ((key == null) || (key.length() == 0) || (super.keyExists(key) == false))
		{
			super.printKeys();
			key = Utils.prompt("Key");
		}
		
		DbConn dbconn = super.getDbConn(key);
		dbconn.setUser(Utils.prompt("Username", this.defaultUsername));
		dbconn.setPassword(promptForPassword());
			
		return dbconn;
	}
	
	public void run() throws FileSystemException, IOException, InfrastructureException
	{
		boolean foundMenuItem = true;
		String lastMenuChoice = null;
				
		if (FileUtils.fileExists(super.getConnectionFile()) == false)
		{
			String temp = Utils.prompt("File", ConnectionManager.DEFAULT_FILE);
			super.loadFile(temp, true);
		}
				
		while (foundMenuItem == true)
		{
			lastMenuChoice = menu(lastMenuChoice);
			Utils.output("");
			if (lastMenuChoice.equalsIgnoreCase("1") == true)
			{
				super.printKeys();
			}
			else if (lastMenuChoice.equalsIgnoreCase("2") == true)
			{
				menuAdd();
			}
			else if (lastMenuChoice.equalsIgnoreCase("3") == true)
			{
				menuUpdate();
			}
			else if (lastMenuChoice.equalsIgnoreCase("4") == true)
			{
				menuDelete();
			}
			else if (lastMenuChoice.equalsIgnoreCase("5") == true)
			{
				menuGetEntry();
			}
			else if (lastMenuChoice.equalsIgnoreCase("6") == true)
			{
				menuTestConnection();
			}
			else
			{
				foundMenuItem = false;
				Utils.output("Goodbye");
			}
			Utils.output("\n\n");
		}
		
		super.close();
	}
	
	private String menu(String defaultChoice)
	{
		Utils.output("");
		Utils.output("=================================");
		Utils.output("    CF DB Connection Manager     ");
		Utils.output("                                 ");
		Utils.output("Blame: M Jacobsen (mjg2.net/code)");
		Utils.output("=================================");
		Utils.output("");
		Utils.output("1) List keys");
		Utils.output("2) Add entry");
		Utils.output("3) Update entry");
		Utils.output("4) Delete entry");
		Utils.output("5) Get entry");
		Utils.output("6) Test entry");
		return Utils.prompt("Choice", defaultChoice);
	}
	
	private void menuAdd()
	{
		String key = Utils.prompt("Key");
		DbConn value = getValues(null);
		
		if ( (key.length() > 0) && (value != null) )
		{
			if (super.addConnection(key, value) == true)
			{
				Utils.output("Added: " + key);
			}
			else
			{
				Utils.output("ERROR adding: " + key);
			}
		}
	}
	
	private void menuUpdate()
	{
		String key = Utils.prompt("Key");
		
		if (super.keyExists(key) == true)
		{
			DbConn value = this.getValues(key);
			
			if ( (key.length() > 0) && (value != null) )
			{
				if (super.updateConnection(key, value) == true)
				{
					Utils.output("Updated: " + key);
				}
				else
				{
					Utils.output("ERROR updating: " + key);
				}
			}
		}
		else
		{
			Utils.output("ERROR: Invalid Key: " + key);
		}
	}
	
	private void menuDelete()
	{
		String key = Utils.prompt("Key");
		if (key.length() > 0)
		{
			if (super.deleteConnection(key) == true)
			{
				Utils.output("Removed: " + key);
			}
			else
			{
				Utils.output("ERROR removing: " + key);
			}
		}
	}
	
	private void menuGetEntry()
	{
		String key = Utils.prompt("Key");
		super.printKey(key);
	}
	
	private void menuTestConnection()
	{
		String key = Utils.prompt("Key");
		
		if (super.keyExists(key) == true)
		{
			String user = Utils.prompt("Username", this.defaultUsername);
			String pass = promptForPassword();
			super.testConnection(key, user, pass);
		}
		else
		{
			Utils.output("ERROR: Invalid Key: " + key);
		}
	}
	
	public boolean testConnection(String key, String user, String pass)
	{
		boolean success = super.testConnection(key, user, pass);
		if (success == true)
		{
			Utils.output("Test SQL succeeded for " + key);
		}
		else
		{
			Utils.output("ERROR: Running test SQL for " + key);
		}
		return success;
	}
	
	private DbConn getValues(String key)
	{
		DbConn retConn = null;
		String type = null;
		String db = null;
		String host = null;
		String port = null;
		String testConn = null;
		
		if (key == null)
		{
			type = getTypeMenu(this.lastType);
			if (DbUtils.isOdbc(type) == true)
			{
				db = Utils.prompt("ODBC Name", this.lastDb);
				host = "na";
				port = "0";
			}
			else
			{
				db = Utils.prompt("DB", this.lastDb);
				host = Utils.prompt("Host", this.lastHost);
				
				String defaultPort = Convert.toString(DbUtils.getDefaultPort(type));
				if ((defaultPort == null) || (defaultPort.equalsIgnoreCase("0") == true)) { defaultPort = this.lastPort; }
				port = Utils.prompt("Port", defaultPort);
				
				this.lastHost = host;
				this.lastPort = port;
			}
			
			this.lastType = type;
			this.lastDb = db;
		}
		else
		{
			DbConn currInfo = super.getDbConn(key);
			if (DbUtils.isOdbc(currInfo.getType()) == true)
			{
				db = Utils.prompt("ODBC Name", currInfo.getDb());
				host = "na";
				port = "0";
			}
			else
			{
				db = Utils.prompt("DB", currInfo.getDb());
				host = Utils.prompt("Host", currInfo.getHost());
				port = Utils.prompt("Port", Convert.toString(currInfo.getPort()));
			}
		}
		
		int intPort = Convert.toInt(port);
		String driver = DbUtils.getDriver(type);
		String url = DbUtils.getUrl(type, host, db, intPort);
		retConn = new DbConn(driver, url, type, host, db, intPort);
		testConn = Utils.prompt("Test connection before adding?", "Y");
		
		if (testConn.equalsIgnoreCase("Y") == true)
		{
			String user = Utils.prompt("Username", this.defaultUsername);
			String pass = promptForPassword();
			try
			{
				boolean success = DbUtils.testConnection(retConn.getType(), retConn.getHost(), retConn.getDb(), retConn.getPort(), user, pass);
				if (success == false)
				{
					Utils.output("The connection did not test successfully so will NOT be added");
					retConn = null;
				}
			}
			catch (Exception e)
			{
				Utils.output("The connection did not test successfully so will NOT be added");
				retConn = null;
			}
		}
		
		return retConn;
	}
	
	private String getTypeMenu(String defaultVal)
	{
		String choice = null;
		String ret = null;
		Utils.output("1) " + DbUtils.TYPE_DB2_JCC);
		Utils.output("2) " + DbUtils.TYPE_MYSQL);
		Utils.output("3) " + DbUtils.TYPE_SQL_SERVER);
		Utils.output("4) " + DbUtils.TYPE_ODBC);
		if (defaultVal == null)
		{
			choice = Utils.prompt("Type");
		}
		else
		{
			if (DbUtils.isDb2JCC(defaultVal) == true) { defaultVal = "1"; }
			else if (DbUtils.isMySql(defaultVal) == true) { defaultVal = "2"; }
			else if (DbUtils.isSqlServer(defaultVal) == true) { defaultVal = "3"; }
			else if (DbUtils.isOdbc(defaultVal) == true) { defaultVal = "4"; }
			choice = Utils.prompt("Type", defaultVal);
		}
		
		if (choice.equalsIgnoreCase("1") == true) { ret = DbUtils.TYPE_DB2_JCC; }
		else if (choice.equalsIgnoreCase("2") == true) { ret = DbUtils.TYPE_MYSQL; }
		else if (choice.equalsIgnoreCase("3") == true) { ret = DbUtils.TYPE_SQL_SERVER; }
		else if (choice.equalsIgnoreCase("4") == true) { ret = DbUtils.TYPE_ODBC; }
	
		return ret;
	}
	
	private String promptForPassword()
	{
		String pass = Utils.promptPassword();
		if ((pass == null) || (pass.length() == 0))
		{
			pass = this.defaultPassword;
		}
		return pass;
	}
}
