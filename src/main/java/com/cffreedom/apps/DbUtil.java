package com.cffreedom.apps;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.apps.DbConnManager;
import com.cffreedom.beans.DbConn;
import com.cffreedom.exceptions.FileSystemException;
import com.cffreedom.exceptions.InfrastructureException;
import com.cffreedom.utils.db.ConnectionManager;
import com.cffreedom.utils.db.DbUtils;
import com.cffreedom.utils.Convert;
import com.cffreedom.utils.file.FileUtils;
import com.cffreedom.utils.SystemUtils;
import com.cffreedom.utils.Utils;

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
 * 2013-05-06 	markjacobsen.net 	Created
 * 2013-05-09 	markjacobsen.net 	Added constructor to pass in default user/pass
 * 2013-05-23 	markjacobsen.net 	Added option to list tables
 */
public class DbUtil
{
	private final static String SCRIPT_HIST = SystemUtils.getDirConfig() + SystemUtils.getPathSeparator() + "dbutils.script.hist";
	private final static String SQL_HIST = SystemUtils.getDirConfig() + SystemUtils.getPathSeparator() + "dbutils.sql.hist";
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.apps.DbUtil");
	private DbConnManager dcm = null;
	private String lastConnKey = null;
	private String lastMenuChoice = null;
	
	public DbUtil() throws FileSystemException, IOException, InfrastructureException
	{
		this.dcm = new DbConnManager();
	}
	
	public DbUtil(String connectionFile) throws FileSystemException, IOException, InfrastructureException
	{
		this.dcm = new DbConnManager(connectionFile);
	}
	
	public DbUtil(String connectionFile, String defaultUsername, String defaultPassword) throws FileSystemException, IOException, InfrastructureException
	{
		this.dcm = new DbConnManager(connectionFile, defaultUsername, defaultPassword);
	}
	
	public static void main(String[] args) throws FileSystemException, IOException, InfrastructureException
	{
		DbUtil dbu = new DbUtil();
		dbu.run();
	}
	
	public void run()
	{
		try
		{
			if (FileUtils.fileExists(this.dcm.getConnectionFile()) == false)
			{
				String temp = Utils.prompt("File", ConnectionManager.DEFAULT_FILE);
				this.dcm.loadFile(temp);
			}
			
			boolean foundMenuItem = true;
			
			while (foundMenuItem == true)
			{
				this.lastMenuChoice = menu(this.lastMenuChoice);
				
				Utils.output("");
				if (this.lastMenuChoice.equalsIgnoreCase("1") == true)
				{
					// Run DB Connection Manager
					this.dcm.run();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("2") == true)
				{
					this.menuRunSql();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("3") == true)
				{
					this.menuRunSqlScript();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("4") == true)
				{
					this.menuListTables();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("80") == true)
				{
					this.menuShowSqlHist();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("81") == true)
				{
					this.menuShowScriptHist();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("90") == true)
				{
					this.menuDeleteSqlHist();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("91") == true)
				{
					this.menuDeleteScriptHist();
				}
				else if (this.lastMenuChoice.equalsIgnoreCase("92") == true)
				{
					this.menuCleanupScriptHistFile();
				}
				else
				{
					foundMenuItem = false;
					Utils.output("Goodbye");
				}
				Utils.output("\n\n");
			}
		}
		catch (SQLException | FileSystemException | InfrastructureException | IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private String menu(String lastChoice)
	{
		Utils.output("");
		Utils.output("=================================");
		Utils.output("        CF DB Utilities          ");
		Utils.output("                                 ");
		Utils.output("Blame: M Jacobsen (mjg2.net/code)");
		Utils.output("=================================");
		Utils.output("");
		Utils.output("1) Connection Manager");
		Utils.output("2) Run SQL");
		Utils.output("3) Run Script");
		Utils.output("4) List tables");
		Utils.output("80) View SQL Hist");
		Utils.output("81) View Script Hist");
		Utils.output("90) Delete SQL Hist");
		Utils.output("91) Delete Script Hist");
		Utils.output("92) Delete Missing Scripts from Script Hist");
		return Utils.prompt("Choice", lastChoice);
	}
	
	private void menuRunSql() throws SQLException, FileSystemException
	{
		this.dcm.printKeys();
		Utils.output("");
		
		String key = Utils.prompt("Key", this.lastConnKey);
		if (this.dcm.keyExists(key) == true)
		{
			this.lastConnKey = key;
			Connection conn = this.dcm.getConnection(key);
			
			if (conn != null)
			{
				Utils.output("SQL (end with ;)");
				Utils.output("==========================");
				String sql = Utils.promptBare().trim();
				while (Utils.lastChar(sql).equalsIgnoreCase(";") == false)
				{
					sql += " " + Utils.promptBare().trim();
				}
				String sqlWoTerm = sql.substring(0, sql.length() - 1);
				boolean success = DbUtils.runSql(conn, sqlWoTerm);
				conn.close();
				
				if (success == true)
				{
					addToSqlHist(sql);
				}
			}
		}
		else
		{
			Utils.output("ERROR: Unknown connection: " + key);
		}
	}
	
	private void menuRunSqlScript() throws SQLException, FileSystemException
	{
		this.dcm.printKeys();
		Utils.output("");
		
		String key = Utils.prompt("Key", this.lastConnKey);
		if (this.dcm.keyExists(key) == true)
		{
			this.lastConnKey = key;
			Connection conn = this.dcm.getConnection(key);
			
			if (conn != null)
			{
				String scriptFile = getScriptFile();
				
				if (FileUtils.fileExists(scriptFile) == true)
				{
					String format = Utils.prompt("Format: XML,CSV,TAB", DbUtils.FORMAT.XML.toString()).toUpperCase();
					if (DbUtils.validFormat(format) == false) { format = DbUtils.FORMAT.XML.toString(); }
					int errors = DbUtils.runSqlScript(conn, scriptFile, DbUtils.FORMAT.valueOf(format));
					if (errors != 0)
					{
						Utils.output("There were errors running the script: " + scriptFile);
					}
					conn.close();
			
					addToScriptHist(scriptFile);
				}
				else
				{
					logger.warn("Script file does NOT exist: {}", scriptFile);
				}
			}
		}
		else
		{
			Utils.output("ERROR: Unknown connection: " + key);
		}
	}
	
	private void menuListTables()
	{
		this.dcm.printKeys();
		Utils.output("");
		
		String key = Utils.prompt("Key", this.lastConnKey);
		if (this.dcm.keyExists(key) == true)
		{
			this.lastConnKey = key;
			DbConn dbconn = this.dcm.getDbConnWithUserInfo(key);
			DbUtils.listTables(dbconn);
		}
	}
	
	private void menuShowSqlHist()
	{
		if (FileUtils.fileExists(DbUtil.SQL_HIST) == true){
			Utils.output(FileUtils.getFileContents(DbUtil.SQL_HIST));
		}else{
			Utils.output("No history yet.");
		}
	}
	
	private void menuShowScriptHist()
	{
		if (FileUtils.fileExists(DbUtil.SCRIPT_HIST) == true){
			Utils.output(FileUtils.getFileContents(DbUtil.SCRIPT_HIST));
		}else{
			Utils.output("No history yet.");
		}
	}
	
	private void menuDeleteSqlHist()
	{
		if (Utils.prompt("Are you sure you want to delete your history?", "N").equalsIgnoreCase("Y") == true)
		{
			if (FileUtils.deleteFile(DbUtil.SQL_HIST) == true){
				Utils.output("Script history deleted successfully");
			}
		}
	}
	
	private void menuDeleteScriptHist()
	{
		if (Utils.prompt("Are you sure you want to delete your history?", "N").equalsIgnoreCase("Y") == true)
		{
			if (FileUtils.deleteFile(DbUtil.SCRIPT_HIST) == true){
				Utils.output("Script history deleted successfully");
			}
		}
	}
	
	private void menuCleanupScriptHistFile() throws FileSystemException
	{
		if (Utils.prompt("Are you sure you want to remove missing files from your history?", "N").equalsIgnoreCase("Y") == true)
		{
			int counter = 0;
			List<String> files = FileUtils.getFileLines(SCRIPT_HIST);
			if (files.size() > 0)
			{
				for (String file : files)
				{
					if (FileUtils.fileExists(file) == false)
					{
						Utils.output("Removing from hist: " + file);
						FileUtils.stripLinesInFileContaining(SCRIPT_HIST, file);
						counter++;
					}
				}
				Utils.output("Removed " + counter + " files from your history");
			}
		}
	}
	
	private String getScriptFile()
	{
		int counter = 0;
		String retVal = null;
		
		try
		{
			if (FileUtils.fileExists(SCRIPT_HIST) == true)
			{
				List<String> files = FileUtils.getFileLines(SCRIPT_HIST);
				if (files.size() > 0)
				{
					for (String file : files)
					{
						counter++;
						Utils.output(counter + ") " + file);
					}
					String choice = Utils.prompt("Option (Choose 0 for a new script)", Convert.toString(counter));
					if (Utils.isInt(choice) == true)
					{
						int item = Convert.toInt(choice);
						if ((item > 0) && (item <= files.size()))
						{
							retVal = files.get(item - 1);
						}
					}
				}
			}
			else
			{
				FileUtils.touch(SCRIPT_HIST);
			}
			
			if ( (retVal == null) || (FileUtils.fileExists(retVal) == false) )
			{
				retVal = Utils.prompt("File");
			}
		}
		catch (Exception e) {}
		
		return retVal;
	}
	
	public static void addToScriptHist(String scriptFile) throws FileSystemException
	{
		FileUtils.stripLinesInFileContaining(SCRIPT_HIST, scriptFile);
		FileUtils.appendLine(scriptFile, SCRIPT_HIST);
	}
	
	public static void addToSqlHist(String sql) throws FileSystemException
	{
		FileUtils.stripLinesInFileContaining(SQL_HIST, sql);
		FileUtils.appendLine(sql, SQL_HIST);
	}
}
