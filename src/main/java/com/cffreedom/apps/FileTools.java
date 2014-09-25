package com.cffreedom.apps;

import java.util.List;
import java.util.Map;

import com.cffreedom.utils.Convert;
import com.cffreedom.utils.Utils;
import com.cffreedom.utils.file.FileUtils;

public class FileTools 
{
	public FileTools()
	{
		
	}
	
	public static void main(String[] args)
	{
		FileTools ft = new FileTools();
	}
	
	public void run() throws Exception
	{
		boolean foundMenuItem = true;
		String lastMenuChoice = null;
		String lastFile = null;
				
		while (foundMenuItem == true)
		{
			lastMenuChoice = menu(lastMenuChoice);
			System.out.println("");
			if (lastMenuChoice.equalsIgnoreCase("1") == true)
			{
				lastFile = getFile(lastFile);
				if (lastFile != null)
				{
					Map<String, Integer> report = FileUtils.getLineCounts(lastFile);
					for (String key : report.keySet())
					{
						Utils.output(report.get(key) + " --> " + key);
					}
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("2") == true)
			{
				lastFile = getFile(lastFile);
				if (lastFile != null)
				{
					int lineCount = Convert.toInt(Utils.prompt("Number of lines", "20"));
					printLines(FileUtils.getFirstXLines(lastFile, lineCount));
				}
			}
			else if (lastMenuChoice.equalsIgnoreCase("3") == true)
			{
				lastFile = getFile(lastFile);
				if (lastFile != null)
				{
					int lineCount = Convert.toInt(Utils.prompt("Number of lines", "20"));
					printLines(FileUtils.getLastXLines(lastFile, lineCount));
				}
			}
			else
			{
				foundMenuItem = false;
				System.out.println("Goodbye");
			}
			System.out.println("\n\n");
		}
	}
	
	private String getFile(String defaultFile)
	{
		String file = Utils.prompt("File", defaultFile);
		if (FileUtils.fileExists(file) == false)
		{
			file = null;
		}
		return file;
	}
	
	private void printLines(List<String> lines)
	{
		for (String line : lines)
		{
			Utils.output(line);
		}
	}
	
	private String menu(String lastChoice)
	{
		System.out.println("");
		System.out.println("=================================");
		System.out.println("       CF File Utilities         ");
		System.out.println("                                 ");
		System.out.println("Blame: M Jacobsen (mjg2.net/code)");
		System.out.println("=================================");
		System.out.println("");
		System.out.println("1) Occurances of lines in file");
		System.out.println("2) Head");
		System.out.println("3) Tail");
		System.out.println("0) Exit");
		return Utils.prompt("Choice", lastChoice);
	}
}
