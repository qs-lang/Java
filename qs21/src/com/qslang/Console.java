package com.qslang;

import java.util.Scanner;

/**
 * WARNING!
 * qs21 was designed and destined to be a scripting language.
 * However, on the account that Java doesn't really need a
 * standalone scripting language, I've decided to turn this
 * interpreter, into a proper programming language.
 *
 * It is going to maintain all standard qsvm functionality!
 *
 * On top of that it is going to get supplied with a bunch
 * of Java libraries connected to the qsvm. More so, it is
 * going to receive a custom network based package manager
 * and much more!
 *
 * But for now it's only a *fun* project.
 */
public class Console
{
	public static void main (String[] argv)
	{
		// Create instance of qs21 Virtual Machine
		qs21 vm = new qs21();

		// Connect custom defined "gets" procedure
		vm.def("gets", new CustomGets());

		// Use autoUpdate() feature
		vm.autoUpdate();

		// Set qs21 built in thread loop to run with 1L delay
		vm.setAutoUpdateDelay(1L);

		// Read and evaluate scripts from the command line arguments
		for (int i = 0; i < argv.length; i++)
			vm.yield("{fread>" + argv[i] + ">code>{{code}}}");
	}

	// New definition of "gets" procedure.
	static class CustomGets extends CustomProcedure
	{
		// This will get executed as soon as something calls "gets" inside qsvm
		@Override
		public void Procedure (qs21 vm)
		{
			Scanner scanner = new Scanner(System.in);
			// Tell vm it's busy
			vm.setBusy(true);
			// Define data from scanner inside vm
			vm.def(vm.v("gets-0"), scanner.nextLine());
			// Tell vm it is not busy anymore
			vm.setBusy(false);
			// Execute given callback
			vm.yield(vm.v("gets-1"));
		}
	}
}
