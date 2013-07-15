package com.cffreedom.apps;

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
 */
public class ServiceManager
{
	private String lastAction = null;
	private String lastSvc = null;
	
	public static void main(String[] args)
	{
		ServiceManager sm = new ServiceManager();
		sm.run();
	}
	
	public void run()
	{
		String cmdFile = SystemUtils.getDirConfig() + SystemUtils.getPathSeparator() + "cmd.bat";
		boolean foundMenuItem = true;
		
//		try
//		{
			while (foundMenuItem == true)
			{
				String option = menu();
				String tempCmd = null;
				if (option.length() > 0)
				{
					Utils.output("");
					String[] opt = option.split(" ");
					option = option.replaceFirst(opt[0], "").trim();
					if (opt[0].equalsIgnoreCase("START") == true)
					{
						tempCmd = "NET START \"" + option + "\"";
						if (SystemUtils.isWindows() == false){
							tempCmd = "sudo service " + option + " start";
						}							
						SystemUtils.exec(tempCmd);
					}
					else if (opt[0].equalsIgnoreCase("STOP") == true)
					{
						tempCmd = "NET STOP \"" + option + "\"";
						if (SystemUtils.isWindows() == false){
							tempCmd = "sudo service " + option + " stop";
						}							
						SystemUtils.exec(tempCmd);
					}
					else if (opt[0].equalsIgnoreCase("RESTART") == true)
					{
						if (SystemUtils.isWindows() == false)
						{
							SystemUtils.exec("sudo service " + option + " restart");
						}
						else
						{
							FileUtils.writeStringToFile(cmdFile, "NET STOP \"" + option + "\"" + SystemUtils.getNewline(), false);
							FileUtils.writeStringToFile(cmdFile, "NET START \"" + option + "\"" + SystemUtils.getNewline(), true);
							FileUtils.writeStringToFile(cmdFile, "exit" + SystemUtils.getNewline(), true);
							SystemUtils.exec(cmdFile);
						}
					}
					else
					{
						foundMenuItem = false;
						Utils.output("Goodbye");
					}
				}
				Utils.output("\n\n");
			}
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
	}
	
	private String menu()
	{
		Utils.output("");
		Utils.output("=================================");
		Utils.output("     CF Service Manager Menu     ");
		Utils.output("                                 ");
		Utils.output("Blame: M Jacobsen (mjg2.net/code)");
		Utils.output("=================================");
		Utils.output("");
		this.lastAction = Utils.prompt("stop, start, restart", this.lastAction);
		this.lastSvc = Utils.prompt("Service", this.lastSvc);
		return this.lastAction + " " + this.lastSvc;
	}
}
