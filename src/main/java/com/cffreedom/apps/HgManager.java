package com.cffreedom.apps;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.utils.file.FileUtils;
import com.cffreedom.utils.KeyValueFileMgr;
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
 * 2013-05-06 	markjacobsen.net	Created
 */
public class HgManager
{
	public static final String DEFAULT_FILE = SystemUtils.getDirConfig() + SystemUtils.getPathSeparator() + "hg.dat";
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.apps.HgManager");
	private KeyValueFileMgr kvfm = null;
	private String file = null;
	
	public HgManager()
	{
		this(DEFAULT_FILE);
	}
	
	public HgManager(String file)
	{
		loadFile(file);
	}
	
	public static void main(String[] args) throws Exception
	{
		HgManager hgm = new HgManager();
		hgm.run();
	}
	
	public boolean loadFile(String file)
	{
		if (file == null) { file = HgManager.DEFAULT_FILE; }
		
		logger.debug("Loading file: {}", file);
		this.file = file;
		this.kvfm = new KeyValueFileMgr(this.file);  // init so we can use it from other apps
		return true;
	}
	
	public void run() throws Exception
	{
		boolean foundMenuItem = true;
		String lastMenuChoice = null;
				
		while (foundMenuItem == true)
		{
			lastMenuChoice = menu(lastMenuChoice);
			System.out.println("");
			if (lastMenuChoice.equalsIgnoreCase("1") == true)
			{
				this.printEntryKeys();
			}
			else if (lastMenuChoice.equalsIgnoreCase("2") == true)
			{
				// Add local repo
				String key = Utils.prompt("Key");
				String value = Utils.prompt("Repo Directory");
				
				if (FileUtils.folderExists(value) == true)
				{
					if ( (key.length() > 0) && (value.length() > 0) )
					{
						if (this.kvfm.addEntry(key, value) == true) {
							System.out.println("Added: " + key);
						}else{
							System.out.println("ERROR adding: " + key);
						}
					}
				}
				else
				{
					System.out.println("The selected folder does not exist: " + value);
					if (Utils.prompt("Create [Y/N]").equalsIgnoreCase("Y") == true)
					{
						if (FileUtils.createFolder(value) == true)
						{
							String url = Utils.prompt("Remote Repo URL");
							System.out.println("Wait for the command to complete");
							int rc = hgClone(url, value);
							if (rc == 0){
								this.kvfm.addEntry(key, value);
							}
						}
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("3") == true)
			{
				// Update local repo
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				
				if (this.kvfm.keyExists(key) == true)
				{
					String dir = this.kvfm.getEntryAsString(key);
					System.out.println("Wait for the command to complete");
					int rcPull = this.hgPull(dir);
					if (rcPull != 0){
						System.out.println("Error pulling to local repo");
					}
					
					int rcUpdate = hgUpdate(dir);
					if (rcUpdate != 0){
						System.out.println("Error updating local repo");
					}
				}
				else
				{
					System.out.println("ERROR: Invalid Key: " + key);
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("4") == true)
			{
				// Commit local changes
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				
				if (this.kvfm.keyExists(key) == true)
				{
					String comment = Utils.prompt("Comment");
					String dir = this.kvfm.getEntryAsString(key);
					System.out.println("Wait for the command to complete");
					int rc = hgCommitLocal(dir, comment);
					if (rc != 0){
						System.out.println("Error updating local repo");
					}
				}
				else
				{
					System.out.println("ERROR: Invalid Key: " + key);
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("5") == true)
			{
				// Push local changes to remote repo
				System.out.println("Commenting out this functionality until I actually have to use it");
//				this.kvfm.printEntryKeys();
//				String key = Utils.prompt("Key");
//				
//				if (this.kvfm.keyExists(key) == true)
//				{
//					String dir = this.kvfm.getEntry(key);
//					System.out.println("Wait for the command to complete");
//					int rc = hgCommitRemote(dir);
//					if (rc != 0){
//						System.out.println("Error pushing to remote repo");
//					}
//				}
//				else
//				{
//					System.out.println("ERROR: Invalid Key: " + key);
//				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("10") == true)
			{
				// List Branches
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				
				if (this.kvfm.keyExists(key) == true)
				{
					String dir = this.kvfm.getEntryAsString(key);
					int rc = hgBranchList(dir);
					if (rc != 0){
						Utils.output("ERROR: Displaying branch list: " + key);
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("11") == true)
			{
				// Current local Branch
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				
				if (this.kvfm.keyExists(key) == true)
				{
					String dir = this.kvfm.getEntryAsString(key);
					int rc = hgBranchDisplay(dir);
					if (rc != 0){
						Utils.output("ERROR: Displaying branch: " + key);
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("11") == true)
			{
				// Switch to Branch
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				
				if (this.kvfm.keyExists(key) == true)
				{
					String dir = this.kvfm.getEntryAsString(key);
					String branchName = Utils.prompt("Branch name to switch to");
					int rc = hgBranchSwitchTo(dir, branchName);
					if (rc != 0){
						Utils.output("ERROR: switching to branch: " + branchName);
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("12") == true)
			{
				// Branch
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				
				if (this.kvfm.keyExists(key) == true)
				{
					String dir = this.kvfm.getEntryAsString(key);
					String branchName = Utils.prompt("New Branch Name");
					Utils.output("Wait for the command to complete");
					int rc = hgBranch(dir, branchName);
					if (rc != 0){
						Utils.output("ERROR: Creating new branch: " + branchName);
					}
					else
					{
						String persist = Utils.prompt("Would you like to commit to persist your branch name remotely?", "Y");
						if (persist.equalsIgnoreCase("Y") == true)
						{
							this.hgCommitLocal(dir, "New branch: " + branchName);
							this.hgCommitRemote(dir);
						}
					}
				}
				else
				{
					Utils.output("ERROR: Invalid Key: " + key);
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("70") == true)
			{
				String baseUrl = Utils.prompt("Base URL", "http://spa-build/hg/jpod-test");
				String baseDir = Utils.prompt("Base Project Directory", "D:\\jpod\\eclipse\\workspace");
				String projectList = Utils.prompt("Project List File", "D:\\jpod\\eclipse\\workspace\\jPod Utils\\mercurial-projects.txt");
				String delete = Utils.prompt("Delete local contents if they exist?", "N");
				String update = Utils.prompt("Update local contents if they exist?", "N");
				List<String> projects = FileUtils.getFileLines(projectList);
				for (String project : projects)
				{
					String dir = FileUtils.buildPath(baseDir, project);
					String url = baseUrl + "/" + project;
					Utils.output("==============================================");
					
					if (FileUtils.folderExists(dir) == false)
					{
						Utils.output("Creating Directory: " + dir);
						FileUtils.createFolder(dir);
						
						Utils.output("Cloning to local repo");
						this.hgClone(url, dir);
						
						Utils.output("Updating local repo");
						this.hgUpdate(dir);
					}
					else
					{
						Utils.output("Dir DOES exist: " + dir);
						if (delete.equalsIgnoreCase("Y") == true)
						{
							Utils.output("Deleting: " + dir);
							FileUtils.deleteFolder(dir);
							
							Utils.output("Creating: " + dir);
							FileUtils.createFolder(dir);
						}
						
						if (update.equalsIgnoreCase("Y") == true)
						{
							Utils.output("Updating local repo");
							this.hgUpdate(dir);
						}
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("80") == true)
			{
				// Delete local repo
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				String dir = this.kvfm.getEntryAsString(key);
				
				if ((key.length() > 0) && (FileUtils.deleteFolder(dir) == true))
				{
					if (this.kvfm.removeEntry(key) == true) {
						System.out.println("Removed: " + key);
					}else{
						System.out.println("ERROR removing: " + key);
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("99") == true)
			{
				this.kvfm.printEntryKeys();
				String key = Utils.prompt("Key");
				System.out.println(this.kvfm.getEntry(key));
			}
			else
			{
				foundMenuItem = false;
				System.out.println("Goodbye");
			}
			System.out.println("\n\n");
		}
	}
	
	public void printEntryKeys()
	{
		this.kvfm.printEntryKeys();
	}
	
	private String menu(String lastChoice)
	{
		System.out.println("");
		System.out.println("=================================");
		System.out.println("    CF Mercurial (Hg) Manager    ");
		System.out.println("                                 ");
		System.out.println("Blame: M Jacobsen (mjg2.net/code)");
		System.out.println("=================================");
		System.out.println("");
		System.out.println("1) List local repos");
		System.out.println("2) Add local repo (clone)");
		System.out.println("3) Update local repo contents (pull and update)");
		System.out.println("4) Commit changes to local repo (commit)");
		System.out.println("5) Commit changes to remote repo (push)");
		System.out.println("10) Show Branches");
		System.out.println("11) Show current local Branch");
		System.out.println("12) Branch from current");
		System.out.println("13) Switch to Branch");
		System.out.println("70) Bulk Load");
		System.out.println("80) Remove local repo dir");
		System.out.println("99) Get entry");
		return Utils.prompt("Choice", lastChoice);
	}

	private int hgClone(String url, String dir)
	{
		return SystemUtils.exec("hg clone -U \"" + url + "\" \"" + dir + "\"");
	}
	
	private int hgPull(String dir)
	{
		return SystemUtils.exec("hg pull -u -R \"" + dir + "\"");
	}
	
	private int hgUpdate(String dir)
	{
		return SystemUtils.exec("hg update -R \"" + dir + "\"");
	}
	
	private int hgCommitLocal(String dir, String comment)
	{
		return SystemUtils.exec("hg commit -R \"" + dir + "\" -m \"" + comment + "\"");
	}
	
	private int hgCommitRemote(String dir)
	{
		return SystemUtils.exec("hg push -R \"" + dir + "\"");
	}
	
	private int hgBranch(String dir, String branchName)
	{
		return SystemUtils.exec("hg branch \"" + branchName + "\" -R \"" + dir + "\"");
	}
	
	private int hgBranchDisplay(String dir)
	{
		return SystemUtils.exec("hg branch -R \"" + dir + "\"");
	}
	
	private int hgBranchList(String dir)
	{
		return SystemUtils.exec("hg branches -R \"" + dir + "\"");
	}
	
	private int hgBranchSwitchTo(String dir, String branchName)
	{
		return SystemUtils.exec("hg update \"" + branchName + "\" -C -R \"" + dir + "\"");
	}
}
