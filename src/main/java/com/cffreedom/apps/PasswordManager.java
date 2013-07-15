package com.cffreedom.apps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.beans.PasswordEntry;
import com.cffreedom.utils.KeyValueFileMgr;
import com.cffreedom.utils.SystemUtils;
import com.cffreedom.utils.Utils;
import com.cffreedom.utils.security.EncryptDecryptProxy;
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
 * 2013-05-06 	markjacobsen.net 	Created
 */
public class PasswordManager
{
	public static final String DEFAULT_FILE = SystemUtils.getDirConfig() + SystemUtils.getPathSeparator() + "pwmgr.pw";
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.apps.PasswordManager");
	private String file = null;
	private String masterKey = null;
	KeyValueFileMgr pwm = null;
	EncryptDecryptProxy encDecProx = null;

	public PasswordManager()
	{
		this(null, null);
	}
	
	public PasswordManager(String file, String masterKey)
	{
		if (file != null)
		{
			logger.debug("Opening file: {}", file);
			this.file = file;
		}
		if (masterKey != null)
		{
			logger.debug("Master key passed in");
			this.masterKey = masterKey;
			this.encDecProx = new EncryptDecryptProxy(this.masterKey);
		}
		if (this.file != null)
		{
			this.pwm = new KeyValueFileMgr(this.file);  // init so can be used from other apps
		}
	}

	public static void main(String[] args)
	{
		PasswordManager pwm = new PasswordManager();
		pwm.run();
	}

	public void run()
	{
		boolean foundMenuItem = true;
		String lastMenuChoice = null;

		if (this.file == null)
		{
			this.file = Utils.prompt("File", PasswordManager.DEFAULT_FILE);
		}
		if (this.masterKey == null)
		{
			this.masterKey = Utils.promptPassword("Master Key");
		}
		
		this.pwm = new KeyValueFileMgr(this.file);
		this.encDecProx = new EncryptDecryptProxy(this.masterKey);
		
		String masterKeyFile = this.file + ".priv";
		if (FileUtils.fileExists(masterKeyFile) == false)
		{
			FileUtils.writeStringToFile(masterKeyFile, this.encDecProx.encrypt(this.masterKey), false);
		}
		
		try
		{
			if (this.encDecProx.decrypt(FileUtils.getFileContents(masterKeyFile)).compareTo(this.masterKey) != 0)
			{
				Utils.output("ERROR: Incorrect master key");
				return;
			}
		}
		catch (Exception e)
		{
			Utils.output("ERROR: Incorrect master key");
			return;
		}

		while (foundMenuItem == true)
		{
			lastMenuChoice = menu(lastMenuChoice);
			Utils.output("");
			if (lastMenuChoice.equalsIgnoreCase("1") == true)
			{
				this.pwm.printEntryKeys();
			}
			else if (lastMenuChoice.equalsIgnoreCase("2") == true)
			{
				// Add entry
				Utils.output("Add Entry");
				String key = Utils.prompt("Key");
				String user = Utils.prompt("Username");
				String pass = Utils.promptPassword("Password");
				String pass2 = Utils.promptPassword("Password (again)");
				String note = Utils.prompt("Note");

				if (pass.compareTo(pass2) == 0)
				{
					if ((key.length() > 0) && (user.length() > 0) && (pass.length() > 0))
					{
						if (this.addEntry(key, user, pass, note) == true)
						{
							Utils.output("Added: " + key);
						}
						else
						{
							Utils.output("ERROR adding: " + key);
						}
					}
				}
				else
				{
					Utils.output("ERROR: The values do not match");
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("3") == true)
			{
				// Update entry
				String key = Utils.prompt("Key");

				if (this.pwm.keyExists(key) == true)
				{
					PasswordEntry pe = (PasswordEntry)this.pwm.getEntry(key);
					Utils.output("Update Entry: " + key);
					String user = Utils.prompt("Username", pe.getUsername());
					String pass = Utils.promptPassword("Password");
					String pass2 = Utils.promptPassword("Password (again)");
					String note = Utils.prompt("Note", pe.getNote());

					if (pass.compareTo(pass2) == 0)
					{
						if ((key.length() > 0) && (user.length() > 0) && (pass.length() > 0))
						{
							pe = new PasswordEntry(user, pass, note);
							if (this.pwm.updateEntry(key, pe) == true)
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
						Utils.output("ERROR: The values do not match");
					}
				}
				else
				{
					Utils.output("ERROR: Invalid Key: " + key);
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("4") == true)
			{
				// Delete entry
				String key = Utils.prompt("Key");
				if ( (key.length() > 0) && (this.pwm.keyExists(key) == true) )
				{
					if (Utils.prompt("Are you sure you want to delete the entry: " + key, "N").equalsIgnoreCase("Y") == true)
					{
						if (this.pwm.removeEntry(key) == true)
						{
							Utils.output("Removed: " + key);
						}
						else
						{
							Utils.output("ERROR removing: " + key);
						}
					}
				}
				else
				{
					Utils.output("ERROR: Invalid Key: " + key);
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("5") == true)
			{
				// Get entry
				String key = Utils.prompt("Key");
				if (this.pwm.keyExists(key) == true)
				{
					PasswordEntry pe = (PasswordEntry)this.pwm.getEntry(key);
					Utils.output(key);
					if (pe.getUsername() != null) { Utils.output("Username: " + this.getUsername(key)); }
					if (pe.getPassword() != null) { Utils.output("Password: " + this.getPassword(key)); }
					if (pe.getNote() != null) { Utils.output("Note: " + this.getNote(key)); }
				}
				else
				{
					Utils.output("ERROR: Invalid key: " + key);
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

	private String menu(String defaultChoice)
	{
		Utils.output("");
		Utils.output("=================================");
		Utils.output("       CF PasswordManager        ");
		Utils.output("                                 ");
		Utils.output("Blame: M Jacobsen (mjg2.net/code)");
		Utils.output("=================================");
		Utils.output("");
		Utils.output("1) List keys");
		Utils.output("2) Add entry");
		Utils.output("3) Update entry");
		Utils.output("4) Delete entry");
		Utils.output("5) Get entry");
		return Utils.prompt("Choice", defaultChoice);
	}
	
	public void printEntryKeys()
	{
		this.pwm.printEntryKeys();
	}
	
	public boolean addEntry(String key, String user, String pass, String note)
	{
		if (user == null) { user = ""; }
		if (pass == null) { pass = ""; }
		if (note == null) { note = ""; }
		pass = this.encDecProx.encrypt(pass);
		PasswordEntry pe = new PasswordEntry(user, pass, note);
		return this.pwm.addEntry(key, pe);
	}
	
	public String getUsername(String key)
	{
		return ((PasswordEntry)this.pwm.getEntry(key)).getUsername();
	}
	
	public String getPassword(String key)
	{
		String val = ((PasswordEntry)this.pwm.getEntry(key)).getPassword();
		return this.encDecProx.decrypt(val);
	}
	
	public String getNote(String key)
	{
		return ((PasswordEntry)this.pwm.getEntry(key)).getNote();
	}
}
