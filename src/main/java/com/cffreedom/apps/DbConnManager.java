package com.cffreedom.apps;

import java.sql.Connection;

import com.cffreedom.beans.DbConn;
import com.cffreedom.exceptions.DbException;
import com.cffreedom.utils.ConversionUtils;
import com.cffreedom.utils.LoggerUtil;
import com.cffreedom.utils.SystemUtils;
import com.cffreedom.utils.Utils;
import com.cffreedom.utils.db.BaseDAO;
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
 */
public class DbConnManager extends ConnectionManager
{
	private final LoggerUtil logger = new LoggerUtil(LoggerUtil.FAMILY_UTIL, this.getClass().getPackage().getName() + "." + this.getClass().getSimpleName());
	private String lastType = null;
	private String lastDb = null;
	private String lastHost = null;
	private String lastPort = null;
	private String defaultUsername = SystemUtils.getUsername();
	private String defaultPassword = null;
	
	public DbConnManager() throws DbException
	{
		super();
	}
	
	public DbConnManager(String file) throws DbException
	{
		super(file);
	}
	
	public DbConnManager(String file, String defaultUsername, String defaultPassword) throws DbException
	{
		super(file);
		this.defaultUsername = defaultUsername;
		this.defaultPassword = defaultPassword;
	}
	
	public static void main(String[] args) throws DbException
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
	
	public void run() throws DbException
	{
		boolean foundMenuItem = true;
		String lastMenuChoice = null;
				
		if (FileUtils.fileExists(super.getConnectionFile()) == false)
		{
			String temp = Utils.prompt("File", ConnectionManager.DEFAULT_FILE);
			super.loadConnectionFile(temp);
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
		super.printConnInfo(key);
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
			if (BaseDAO.isOdbc(type) == true)
			{
				db = Utils.prompt("ODBC Name", this.lastDb);
				host = "na";
				port = "0";
			}
			else
			{
				db = Utils.prompt("DB", this.lastDb);
				host = Utils.prompt("Host", this.lastHost);
				
				String defaultPort = ConversionUtils.toString(BaseDAO.getDefaultPort(type));
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
			if (BaseDAO.isOdbc(currInfo.getType()) == true)
			{
				db = Utils.prompt("ODBC Name", currInfo.getDb());
				host = "na";
				port = "0";
			}
			else
			{
				db = Utils.prompt("DB", currInfo.getDb());
				host = Utils.prompt("Host", currInfo.getHost());
				port = Utils.prompt("Port", ConversionUtils.toString(currInfo.getPort()));
			}
		}
		
		retConn = new DbConn(type, host, db, ConversionUtils.toInt(port));
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
		Utils.output("1) " + BaseDAO.TYPE_DB2_JCC);
		Utils.output("2) " + BaseDAO.TYPE_MYSQL);
		Utils.output("3) " + BaseDAO.TYPE_SQL_SERVER);
		Utils.output("4) " + BaseDAO.TYPE_ODBC);
		if (defaultVal == null)
		{
			choice = Utils.prompt("Type");
		}
		else
		{
			if (BaseDAO.isDb2JCC(defaultVal) == true) { defaultVal = "1"; }
			else if (BaseDAO.isMySql(defaultVal) == true) { defaultVal = "2"; }
			else if (BaseDAO.isSqlServer(defaultVal) == true) { defaultVal = "3"; }
			else if (BaseDAO.isOdbc(defaultVal) == true) { defaultVal = "4"; }
			choice = Utils.prompt("Type", defaultVal);
		}
		
		if (choice.equalsIgnoreCase("1") == true) { ret = BaseDAO.TYPE_DB2_JCC; }
		else if (choice.equalsIgnoreCase("2") == true) { ret = BaseDAO.TYPE_MYSQL; }
		else if (choice.equalsIgnoreCase("3") == true) { ret = BaseDAO.TYPE_SQL_SERVER; }
		else if (choice.equalsIgnoreCase("4") == true) { ret = BaseDAO.TYPE_ODBC; }
	
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
