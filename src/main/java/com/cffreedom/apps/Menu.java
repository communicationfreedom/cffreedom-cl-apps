package com.cffreedom.apps;

import java.io.IOException;

import com.cffreedom.exceptions.FileSystemException;
import com.cffreedom.exceptions.InfrastructureException;
import com.cffreedom.utils.Utils;

public class Menu
{
	public static void main(String[] args) throws FileSystemException, IOException, InfrastructureException
	{
		Menu m = new Menu();
		m.run();
	}
	
	public void run() throws FileSystemException, IOException, InfrastructureException
	{
		String lastMenuChoice = null;
		boolean foundMenuItem = true;
		
		while (foundMenuItem == true)
		{
			String option = menu(lastMenuChoice);
			System.out.println("");
			if (option.equalsIgnoreCase("1") == true)
			{
				PasswordManager pwm = new PasswordManager();
				pwm.run();
			}
			else if (option.equalsIgnoreCase("2") == true)
			{
				DbUtil dbu = new DbUtil();
				dbu.run();
			}
			else if (option.equalsIgnoreCase("3") == true)
			{
				ServiceManager sm = new ServiceManager();
				sm.run();
			}
			else
			{
				foundMenuItem = false;
				System.out.println("Goodbye");
			}
			System.out.println("\n\n");
		}
	}
	
	private String menu(String defaultChoice)
	{
		System.out.println("");
		System.out.println("=================================");
		System.out.println("             CF Menu             ");
		System.out.println("                                 ");
		System.out.println("Blame: M Jacobsen (mjg2.net/code)");
		System.out.println("=================================");
		System.out.println("");
		System.out.println("1) Password Manager");
		System.out.println("2) DB Utils");
		System.out.println("3) Service Manager");
		return Utils.prompt("Choice", defaultChoice);
	}
}
