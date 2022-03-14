package com.qslang;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;

/**
 * Set of qs21 standard library functions wrapped inside a single class
 * To be honest, true standard hasn't been defined yet
 */
class stdlib
{
	public stdlib (qs21 vm)
	{
		// ----------------------------------------
		// -- Connect to vm
		vm.def("memdump", new stdlib_memdump());
		vm.def("yield", new stdlib_yield());
		vm.def("uuid", new stdlib_uuid());
		vm.def("puts", new stdlib_puts());
		vm.def("gets", new stdlib_gets());
		vm.def("perf", new stdlib_perf());
		vm.def("def", new stdlib_def());
		vm.def("raw", new stdlib_raw());

		vm.def("add", new stdlib_add());
		vm.def("sub", new stdlib_sub());
		vm.def("mul", new stdlib_mul());
		vm.def("div", new stdlib_div());

		vm.def("len", new stdlib_len());
		vm.def("cc", new stdlib_cc());

		vm.def("eq", new stdlib_eq());
		vm.def("nq", new stdlib_nq());
		vm.def("ls", new stdlib_ls());
		vm.def("gt", new stdlib_gt());

		vm.def("qq", new stdlib_qq());
		vm.def("ns", new stdlib_ns());

		// ?File IO
		vm.def("fread", new stdlib_fread());
		vm.def("fwrite", new stdlib_fwrite());

		// Constants:
		vm.def("nl", "\n");
		vm.def("tb", "\t");
		vm.def("cl", ":");
		vm.def("ar", ">");
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
		vm.def(vm.v("gets-0"), "not handled");
		vm.yield(vm.v("gets-1"));
	}
}

// ----------------------------------------
// -- {def: NAME: VALUE}
class stdlib_def extends CustomProcedure
{
	@Override
	public void Procedure (qs21 vm)
	{
		vm.def(vm.v("def-0"), vm.v("def-1"));
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
		return vm.eval(vm.v("ns-1").replaceAll("~", vm.v("ns-0")));
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
		vm.eval(doBody);
		/*
		String doBody = "{def>%id-next>{ls:{%id}>%times>{yield:0:{qq>{def>%id:{add:{%id}>1}}%body}}>%callback}}{def>%id>-1}{%id-next}";
		doBody = doBody.replace("%id", vm.v("perf-0"));
		doBody = doBody.replace("%times", "" + (vm.d("perf-1") - 1)); // .FIXME.
		doBody = doBody.replace("%body", vm.v("perf-2"));
		doBody = doBody.replace("%callback", vm.v("perf-3"));
		vm.eval(doBody);
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
			return vm.eval(vm.v("nq-2"));
		else
			return vm.eval(vm.v("nq-3"));
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
			return vm.eval(vm.v("eq-2"));
		else
			return vm.eval(vm.v("eq-3"));
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
			return vm.eval(vm.v("ls-2"));
		else
			return vm.eval(vm.v("ls-3"));
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
			return vm.eval(vm.v("gt-2"));
		else
			return vm.eval(vm.v("gt-3"));
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
		vm.yield
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
			vm.def(varName, String.join("\n", Files.readAllLines(Paths.get(fileName))));
			vm.eval(expr);
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
			vm.eval(expr);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
}