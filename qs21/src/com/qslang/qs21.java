package com.qslang;

import java.util.ArrayList;

// ----------------------------------------
// -- Main Class
public class qs21
{
	// ----------------------------------------
	// -- Field
	private int refMapSize;
	private int countCalls;
	private int maxCalls;
	private int maxYields;
	private int maxRisings;
	private ArrayList<ArrayList<Ref>> refMap;
	private ArrayList<Scheduled> scheduler;
	private Boolean alive;
	private Boolean enabled;
	private qs21 vm;


	// ----------------------------------------
	// -- Constructor
	public qs21 ()
	{
		// TODO: Move and extend
		this.refMapSize = 0xF;
		this.maxCalls = 0xFF;
		this.maxYields = 0xFF;
		this.maxRisings = 0xF;
		this.countCalls = 0;
		this.scheduler = new ArrayList<Scheduled>();
		this.refMap = new ArrayList<ArrayList<Ref>>();
		for (int i = 0; i < refMapSize; i++)
			this.refMap.add(new ArrayList<Ref>());
		new stdlib(this);
		vm = this;
		alive = true;
		enabled = true;
		(new Scheduler()).start();
	}

	// ----------------------------------------
	// -- VM API Entry
	public String Eval (String expr)
	{
		// TODO: Inject code with yield rather then eval
		this.countCalls = 0;
		return this.ReEval(expr);
	}

	// ----------------------------------------
	// -- Eval And Caller
	public String ReEval (String expr)
	{
		boolean bMode = true;
		boolean bNext = true;
		String sFun = null;
		StringBuilder sRet = new StringBuilder();
		String[] sArgs = new String[0xFF]; // FIXME: (somehow) Use refMap instead of array
		int dNst = 0;
		int dOff = 0;
		int dArg = 0;

		for (int c = 0; c < expr.length(); c++)
		{
			char cc = expr.charAt(c);

			if (bMode)
			{
				if (cc == '{')
				{
					bMode = false;
					bNext = true;
					dOff = c;
					dNst = 0;
					dArg = 0;
				}
				else
					sRet.append(cc);
			}
			else
			{
				if ((cc == '>' || cc == ':' || cc == '}' ) && dNst == 0)
				{
					String val = expr.substring(dOff + 1, dOff + 1 + c - dOff - 1).trim();
					if (dArg == 0)
						sFun = bNext ? this.ReEval(val) : val;
					else
						sArgs[dArg - 1] = bNext ? this.ReEval(val) : val;
						// -- Def(sFun + "-" + (dArg - 1), bNext ? this.Eval(val) : val); // .mut lim.

					bNext = (cc == ':');
					dArg += 1;
					dOff = c;
				}

				if (cc == '{')
					dNst += 1;

				if (cc == '}')
					if (dNst != 0)
						dNst -= 1;
					else
					{
						// def
						for (int i = 0; i < dArg - 1; i++)
							Def(sFun + "-" + i, sArgs[i]);

						// call
						String sCall = this.Call(sFun, dArg - 1);
						if (sCall != null)
							sRet.append(sCall);
						bMode = true;
					}
			}
		}

		return sRet.toString();
	}

	public String Call (String name, int argc)
	{
		if (this.countCalls > this.maxCalls) {
			Def("!err", "Reached maximum calls!");
			return "";
		}

		this.countCalls += 1;
		Def(name + "-len", String.format("%d", argc));
		Ref ref = Loc(name);

		if (ref == null)
			return "";

		if (ref.getRef() instanceof String)
			return ReEval((String) ref.getRef());
		else if (ref.getRef() instanceof CustomProcedure)
			((CustomProcedure) ref.getRef()).Procedure(this);
		else if (ref.getRef() instanceof CustomFunction)
			return ((CustomFunction) ref.getRef()).Function(this);

		return "";
	}

	// ----------------------------------------
	// -- Ref Map Handler

	public int Pos (String name)
	{
		return name.charAt(0) % this.refMapSize;
	}

	public Ref Loc (String name)
	{
		if (name.equals(""))
			return null;

		for (Ref ref : this.refMap.get(Pos(name)))
			if (ref.getName().equals(name))
				return ref;
		return null;
	}

	public void Def (String name, Object ref)
	{
		Ref r = Loc(name);
		if (r == null)
			this.refMap.get(Pos(name)).add(new Ref(name, ref));
		else
			r.setRef(ref);
	}

	public String v (String name)
	{
		Ref r = Loc(name);
		if (r != null)
			if (r.getRef() instanceof String)
				return (String) r.getRef();
		return "";
	}

	public Float f (String name)
	{
		try
		{
			if (!v(name).equals(""))
				return Float.parseFloat(v(name));
			else
				return 0F;
		}
		catch (Exception e)
		{
			return 0F;
		}
	}

	public int d (String name)
	{
		if (!v(name).equals(""))
			return (int) Math.round(f(name));
		else
			return 0;
	}

	public Object p (String name)
	{
		return Loc(name) != null
			? Loc(name).getRef()
			: null;
	}

	public void dumpMemory ()
	{
		for (ArrayList<Ref> bank : this.refMap)
			for (Ref ref : bank)
				System.out.println(
						ref.getName() + " -> " + (ref.getRef() instanceof String ? "'" + (String) ref.getRef() + "'" : ref.getRef().getClass().toString())
				);
	}

	public String strNorma (float f)
	{
		if (Math.abs(Math.round(f) - f) < 0.0001)
			return String.format("%d", (int) Math.round(f));
		else
		{
			if (f == (long) f)
				return String.format("%d", (long) f);
			else
				return String.format("%s", f);
		}
	}

	// ----------------------------------------
	// -- Scheduler
	private class Scheduler extends Thread
	{
		@Override
		public void run()
		{
			while (alive)
			{
				int risings = 0;
				if (enabled)
					for (int i = scheduler.size() - 1; i >= 0; i--)
						if (scheduler.get(i).IsReady(vm))
						{
							scheduler.remove(i);
							if (risings++ > maxRisings)
								break;
						}
				try { Thread.sleep(1L);  } catch (Exception e) { e.printStackTrace(); }
			}
		}
	}

	public void Kill ()
	{
		alive = false;
	}

	public void Enable ()
	{
		enabled = true;
	}

	public void Disable ()
	{
		enabled = false;
	}

	public void Yield (long timeOut, String expr)
	{
		if (scheduler.size() < this.maxYields)
			scheduler.add(new Scheduled(System.currentTimeMillis() + timeOut, expr));
	}
}
