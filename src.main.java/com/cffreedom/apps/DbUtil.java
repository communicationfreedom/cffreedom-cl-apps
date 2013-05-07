package com.cffreedom.apps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import com.cffreedom.apps.DbConnManager;
import com.cffreedom.exceptions.DbException;
import com.cffreedom.utils.db.ConnectionManager;
import com.cffreedom.utils.db.DbUtils;
import com.cffreedom.utils.ConversionUtils;
import com.cffreedom.utils.file.FileUtils;
import com.cffreedom.utils.LoggerUtil;
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
 */
public class DbUtil
{
	private final static String SCRIPT_HIST = SystemUtils.getMyCFConfigDir() + SystemUtils.getPathSeparator() + "dbutils.script.hist";
	private final static String SQL_HIST = SystemUtils.getMyCFConfigDir() + SystemUtils.getPathSeparator() + "dbutils.sql.hist";
	private final LoggerUtil logger = new LoggerUtil(LoggerUtil.FAMILY_UTIL, this.getClass().getPackage().getName() + "." + this.getClass().getSimpleName());
	DbConnManager dcm = null;
	
	public DbUtil() throws DbException
	{
		this.dcm = new DbConnManager();
	}
	
	public DbUtil(String connectionFile) throws DbException
	{
		this.dcm = new DbConnManager(connectionFile);
	}
	
	public static void main(String[] args) throws DbException
	{
		DbUtil dbu = new DbUtil();
		dbu.run();
	}
	
	public void run()
	{
		final String METHOD = "run";
		String lastMenuChoice = null;
		String lastConnKey = null;
		
		try
		{
			if (FileUtils.fileExists(this.dcm.getConnectionFile()) == false)
			{
				String temp = Utils.prompt("File", ConnectionManager.DEFAULT_FILE);
				this.dcm.loadConnectionFile(temp);
			}
			
			boolean foundMenuItem = true;
			
			while (foundMenuItem == true)
			{
				lastMenuChoice = menu(lastMenuChoice);
				
				Utils.output("");
				if (lastMenuChoice.equalsIgnoreCase("1") == true)
				{
					// Run DB Connection Manager
					this.dcm.run();
				}
				else if (lastMenuChoice.equalsIgnoreCase("2") == true)
				{
					// Run SQL
					this.dcm.printKeys();
					Utils.output("");
					
					String key = Utils.prompt("Key", lastConnKey);
					if (this.dcm.keyExists(key) == true)
					{
						lastConnKey = key;
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
				else if (lastMenuChoice.equalsIgnoreCase("3") == true)
				{
					// Run SQL Script
					this.dcm.printKeys();
					Utils.output("");
					
					String key = Utils.prompt("Key", lastConnKey);
					if (this.dcm.keyExists(key) == true)
					{
						lastConnKey = key;
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
								logger.logWarn(METHOD, "Script file does NOT exist: " + scriptFile);
							}
						}
					}
					else
					{
						Utils.output("ERROR: Unknown connection: " + key);
					}
				}
				else if (lastMenuChoice.equalsIgnoreCase("80") == true)
				{
					// Show SQL Hist
					if (FileUtils.fileExists(DbUtil.SQL_HIST) == true){
						Utils.output(FileUtils.getFileContents(DbUtil.SQL_HIST));
					}else{
						Utils.output("No history yet.");
					}
				}
				else if (lastMenuChoice.equalsIgnoreCase("81") == true)
				{
					// Show Script Hist
					if (FileUtils.fileExists(DbUtil.SCRIPT_HIST) == true){
						Utils.output(FileUtils.getFileContents(DbUtil.SCRIPT_HIST));
					}else{
						Utils.output("No history yet.");
					}
				}
				else if (lastMenuChoice.equalsIgnoreCase("90") == true)
				{
					// Delete SQL Hist
					if (Utils.prompt("Are you sure you want to delete your history?", "N").equalsIgnoreCase("Y") == true)
					{
						if (FileUtils.deleteFile(DbUtil.SQL_HIST) == true){
							Utils.output("Script history deleted successfully");
						}
					}
				}
				else if (lastMenuChoice.equalsIgnoreCase("91") == true)
				{
					// Delete Script Hist
					if (Utils.prompt("Are you sure you want to delete your history?", "N").equalsIgnoreCase("Y") == true)
					{
						if (FileUtils.deleteFile(DbUtil.SCRIPT_HIST) == true){
							Utils.output("Script history deleted successfully");
						}
					}
				}
				else if (lastMenuChoice.equalsIgnoreCase("92") == true)
				{
					// Delete missing files from Script Hist
					if (Utils.prompt("Are you sure you want to remove missing files from your history?", "N").equalsIgnoreCase("Y") == true)
					{
						int counter = 0;
						ArrayList<String> files = FileUtils.getFileLines(SCRIPT_HIST);
						if (files.size() > 0)
						{
							for (String file : files)
							{
								if (FileUtils.fileExists(file) == false)
								{
									if (FileUtils.stripLinesInFileContaining(SCRIPT_HIST, file) == true)
									{
										Utils.output("Removed from hist: " + file);
										counter++;
									}
								}
							}
							Utils.output("Removed " + counter + " files from your history");
						}
					}
				}
				else
				{
					foundMenuItem = false;
					Utils.output("Goodbye");
				}
				Utils.output("\n\n");
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		catch (DbException e)
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
		Utils.output("80) View SQL Hist");
		Utils.output("81) View Script Hist");
		Utils.output("90) Delete SQL Hist");
		Utils.output("91) Delete Script Hist");
		Utils.output("92) Delete Missing Scripts from Script Hist");
		return Utils.prompt("Choice", lastChoice);
	}
	
	private String getScriptFile()
	{
		int counter = 0;
		String retVal = null;
		
		try
		{
			if (FileUtils.fileExists(SCRIPT_HIST) == true)
			{
				ArrayList<String> files = FileUtils.getFileLines(SCRIPT_HIST);
				if (files.size() > 0)
				{
					for (String file : files)
					{
						counter++;
						Utils.output(counter + ") " + file);
					}
					String choice = Utils.prompt("Option (Choose 0 for a new script)", ConversionUtils.toString(counter));
					if (Utils.isInt(choice) == true)
					{
						int item = ConversionUtils.toInt(choice);
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
	
	public static boolean addToScriptHist(String scriptFile)
	{
		FileUtils.stripLinesInFileContaining(SCRIPT_HIST, scriptFile);
		return FileUtils.appendLine(scriptFile, SCRIPT_HIST);
	}
	
	public static boolean addToSqlHist(String sql)
	{
		FileUtils.stripLinesInFileContaining(SQL_HIST, sql);
		return FileUtils.appendLine(sql, SQL_HIST);
	}
}
