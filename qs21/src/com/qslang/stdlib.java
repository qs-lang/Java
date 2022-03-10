package com.qslang;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;

// ----------------------------------------
// -- qs21 Standard Library
public class stdlib
{
	public stdlib (qs21 vm)
	{
		// ----------------------------------------
		// -- Connect to vm
		vm.Def("memdump", new stdlib_memdump());
		vm.Def("yield", new stdlib_yield());
		vm.Def("uuid", new stdlib_uuid());
		vm.Def("puts", new stdlib_puts());
		vm.Def("gets", new stdlib_gets());
		vm.Def("perf", new stdlib_perf());
		vm.Def("def", new stdlib_def());
		vm.Def("raw", new stdlib_raw());

		vm.Def("add", new stdlib_add());
		vm.Def("sub", new stdlib_sub());
		vm.Def("mul", new stdlib_mul());
		vm.Def("div", new stdlib_div());

		vm.Def("len", new stdlib_len());
		vm.Def("cc", new stdlib_cc());

		vm.Def("eq", new stdlib_eq());
		vm.Def("nq", new stdlib_nq());
		vm.Def("ls", new stdlib_ls());
		vm.Def("gt", new stdlib_gt());

		vm.Def("qq", new stdlib_qq());
		vm.Def("ns", new stdlib_ns());

		// ?File IO
		vm.Def("fread", new stdlib_fread());
		vm.Def("fwrite", new stdlib_fwrite());

		// Constants:
		vm.Def("nl", "\n");
		vm.Def("tb", "\t");
		vm.Def("cl", ":");
		vm.Def("ar", ">");
	}
}

// ----------------------------------------
// -- {memdump}
class stdlib_memdump extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		vm.dumpMemory();
	}
}

// ----------------------------------------
// -- {puts: VALUE}
class stdlib_puts extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		System.out.printf("%s", vm.v("puts-0"));
	}
}

// ----------------------------------------
// -- {gets: value > expr}
class stdlib_gets extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		(new GetsHandler(vm.v("gets-0"), vm.v("gets-1"), vm)).start();
	}

	private static class GetsHandler extends Thread
	{
		private String varName;
		private String expr;
		private qs21 vm;
		GetsHandler (String varName, String expr, qs21 vm)
		{
			this.varName = varName;
			this.expr = expr;
			this.vm = vm;
		}
		@Override
		public void run()
		{
			Scanner scanner = new Scanner(System.in);
			vm.ReEval(
					String.format("{def>%s>%s}%s", varName, scanner.nextLine(), expr)
			);
		}
	}
}

// ----------------------------------------
// -- {def: NAME: VALUE}
class stdlib_def extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		vm.Def(vm.v("def-0"), vm.v("def-1"));
	}
}

// ----------------------------------------
// -- {raw: name}
class stdlib_raw extends CustomFunction
{
	@Override
	public String Function (qs21 vm)
	{
		return vm.v(vm.v("raw-0"));
	}
}

// ----------------------------------------
// -- {ns: name > expr}
class stdlib_ns extends CustomFunction
{
	@Override
	public String Function (qs21 vm)
	{
		return vm.ReEval(vm.v("ns-1").replaceAll("~", vm.v("ns-0")));
	}
}

// ----------------------------------------
// -- {perf: name: times > expr > finish}
class stdlib_perf extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		String doBody = "{def>perf-%name>{def>%name:{perf-%name-0}}{ls:{perf-%name-0}>%times>{def>%name-next>{yield:0:{qq>perf-%name:{add:{perf-%name-0}>1}}}}%body>{def>%name-next>}%callback}}{perf-%name>0}";
		doBody = doBody.replace("%name", vm.v("perf-0"));
		doBody = doBody.replace("%times", vm.v("perf-1"));
		doBody = doBody.replace("%body", vm.v("perf-2"));
		doBody = doBody.replace("%callback", vm.v("perf-3"));
		vm.ReEval(doBody);
		/*
		String doBody = "{def>%id-next>{ls:{%id}>%times>{yield:0:{qq>{def>%id:{add:{%id}>1}}%body}}>%callback}}{def>%id>-1}{%id-next}";
		doBody = doBody.replace("%id", vm.v("perf-0"));
		doBody = doBody.replace("%times", "" + (vm.d("perf-1") - 1)); // .FIXME.
		doBody = doBody.replace("%body", vm.v("perf-2"));
		doBody = doBody.replace("%callback", vm.v("perf-3"));
		vm.ReEval(doBody);
		*/
	}
}

// ----------------------------------------
// -- {nq: A: B > T > F}
class stdlib_nq extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		if (!vm.v("nq-0").equals(vm.v("nq-1")))
			return vm.ReEval(vm.v("nq-2"));
		else
			return vm.ReEval(vm.v("nq-3"));
	}
}


// ----------------------------------------
// -- {eq: A: B > T > F}
class stdlib_eq extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		if (vm.v("eq-0").equals(vm.v("eq-1")))
			return vm.ReEval(vm.v("eq-2"));
		else
			return vm.ReEval(vm.v("eq-3"));
	}
}

// ----------------------------------------
// -- {ls: A: B > T > F}
class stdlib_ls extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		if (vm.f("ls-0") < vm.f("ls-1"))
			return vm.ReEval(vm.v("ls-2"));
		else
			return vm.ReEval(vm.v("ls-3"));
	}
}

// ----------------------------------------
// -- {gt: A: B > T > F}
class stdlib_gt extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		if (vm.f("gt-0") > vm.f("gt-1"))
			return vm.ReEval(vm.v("gt-2"));
		else
			return vm.ReEval(vm.v("gt-3"));
	}
}

// ----------------------------------------
// -- {add: A: B}
class stdlib_add extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		return vm.strNorma(vm.f("add-0") + vm.f("add-1"));
	}
}

// ----------------------------------------
// -- {sub: A: B}
class stdlib_sub extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		return vm.strNorma(vm.f("sub-0") - vm.f("sub-1"));
	}
}

// ----------------------------------------
// -- {mul: A: B}
class stdlib_mul extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		return vm.strNorma(vm.f("mul-0") * vm.f("mul-1"));
	}
}

// ----------------------------------------
// -- {div: A: B}
class stdlib_div extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		if (vm.f("div-1") == 0)
			return "";
		else
			return vm.strNorma(vm.f("div-0") / vm.f("div-1"));
	}
}

// ----------------------------------------
// -- {uuid}
class stdlib_uuid extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		return UUID.randomUUID().toString();
	}
}

// ----------------------------------------
// -- {yield: T: EXPR}
class stdlib_yield extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		vm.Yield
		(
				Math.round(vm.f("yield-0")),
				vm.v("yield-1")
		);
	}
}

// String Lib

// ----------------------------------------
// -- {len: str}
class stdlib_len extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		return String.format("%d", vm.v("len-0").length());
	}
}

// ----------------------------------------
// -- {cc: str > at}
class stdlib_cc extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		String str = vm.v("cc-0");
		int index = vm.d("cc-1");

		if (index >= 0 && index < str.length())
			return String.format("%c", vm.v("cc-0").charAt(vm.d("cc-1")));
		else
			return "";
	}
}

// ----------------------------------------
// -- {qq: str}
class stdlib_qq extends CustomFunction
{
	@Override
	public String Function(qs21 vm)
	{
		int count = vm.d("qq-len");
		StringBuilder wrapped = new StringBuilder("{");

		for (int i = 0; i < count; i++)
			wrapped.append(vm.v("qq-" + i)).append((i + 1 < count) ? ">" : "");

		return wrapped.append("}").toString();
	}
}

// ----------------------------------------
// -- {fread: file: name > expr}
class stdlib_fread extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		String fileName = vm.v("fread-0");
		String varName = vm.v("fread-1");
		String expr = vm.v("fread-2");

		try
		{
			vm.Def(varName, String.join("\n", Files.readAllLines(Paths.get(fileName))));
			vm.ReEval(expr);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
}

// ----------------------------------------
// -- {fwrite: file: value > callBack}
class stdlib_fwrite extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		String fileName = vm.v("fwrite-0");
		String value = vm.v("fwrite-1");
		String expr = vm.v("fwrite-2");

		try
		{
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));
			bufferedWriter.write(value);
			bufferedWriter.close();
			vm.ReEval(expr);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
}