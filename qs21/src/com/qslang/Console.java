package com.qslang;

import java.util.Scanner;

/**
 * Testing and debugging stuff.
 */
public class Console
{
	public static void main (String[] argv)
	{
		qs21 vm = new qs21();
		vm.def("gets", new CustomGets());
		vm.autoUpdate(false);
		vm.setAutoUpdateDelay(0L);

		vm.yield("{def>cons>{puts:{nl}}{gets>cmd>{{cmd}}{yield>100>{cons}}}}{cons}");
	}


	static class CustomGets extends CustomProcedure
	{
		@Override
		public void Procedure (qs21 vm)
		{
			Scanner scanner = new Scanner(System.in);
			vm.def(vm.v("gets-0"), scanner.nextLine());
			vm.yield(vm.v("gets-1"));
		}
	}
}
