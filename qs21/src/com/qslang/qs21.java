package com.qslang;

import java.util.ArrayList;

/**
 * qs21 Virtual Machine (qsvm for short)
 */
public class qs21
{
	// Limits:
	private int schedulerSize;
	private int limitYields;
	private int refMapSize;
	private int maxCalls;

	// Fields:
	private ArrayList<ArrayList<Data>> vmem;
	private ArrayList<Yield> scheduler;
	private long autoUpdateDelay;
	private boolean autoUpdateContinuous;
	private int callCounter;
	private boolean busy;
	private qs21 vm;


	public qs21 ()
	{
		useDefaultLimits();
		initVM();
	}

	/**
	 * Evaluates given qs21 expression without resetting the countCalls field. Only to be invoked from
	 * custom qs21 functions / procedures. Do not inject code into vm with this! Direct code injection
	 * may lead to weird bugs and undefined behaviour. Use scheduler in order to inject your code!
	 * @param expr qs21 expression to evaluate
	 * @return results of evaluations
	 */
	public String eval (String expr)
	{
		boolean bMode = true;
		boolean bNext = true;
		String sFun = null;
		StringBuilder sRet = new StringBuilder();
		String[] sArgs = new String[0xFF]; // FIXME: (somehow) Use vmem instead of array
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
						sFun = bNext ? this.eval(val) : val;
					else
						sArgs[dArg - 1] = bNext ? this.eval(val) : val;
						// -- def(sFun + "-" + (dArg - 1), bNext ? this.Eval(val) : val); // TODO: Bring it back somehow

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
							def(sFun + "-" + i, sArgs[i]);

						// call
						String sCall = this.call(sFun, dArg - 1);
						if (sCall != null)
							sRet.append(sCall);
						bMode = true;
					}
			}
		}

		return sRet.toString();
	}

	/**
	 * Performs calls inside qsvm. Will either: invoke custom procedure/function
	 * or evaluate raw data found inside given memory location.
	 * To be used only be the qsvm!
	 * @param name of the memory location inside vmem
	 * @param argc amount of the arguments declared upon calling
	 * @return results of the call
	 */
	private String call (String name, int argc)
	{
		if (this.callCounter++ > this.maxCalls) {
			def("!err", "Reached maximum calls!");
			return "";
		}

		def(name + "-len", String.format("%d", argc));
		Data data = loc(name);

		if (data == null)
			return "";

		if (data.getRef() instanceof String)
			return eval((String) data.getRef());

		else if (data.getRef() instanceof CustomProcedure)
			((CustomProcedure) data.getRef()).Procedure(this);

		else if (data.getRef() instanceof CustomFunction)
			return ((CustomFunction) data.getRef()).Function(this);

		return "";
	}

	private void useDefaultLimits ()
	{
		setSchedulerSize(0xFF);
		setLimitYields(0xF);
		setRefMapSize(0xF);
		setMaxCalls(0xFF);
		this.autoUpdateContinuous = false;
		this.autoUpdateDelay = 100L;
	}

	private void initVM ()
	{
		this.autoUpdateContinuous = false;
		this.callCounter = 0;
		this.scheduler = new ArrayList<Yield>();
		this.vmem = new ArrayList<ArrayList<Data>>();
		for (int i = 0; i < refMapSize; i++)
			this.vmem.add(new ArrayList<Data>());
		vm = this;
		new stdlib(this);
	}

	/**
	 * Creates new thread for qs21 vm. New thread will perform update() method
	 * as long as isAlive() returns true
	 */
	public void autoUpdate ()
	{
		this.autoUpdateContinuous = false;
		this.yield(1000, "");
		(new BuiltInUpdateThread()).start();
	}

	/**
	 * Creates new thread for qs21 vm. New thread will perform update() method
	 * without any stop condition. Use destroy() in order to stop it.
	 */
	public void autoUpdateContinuously ()
	{
		this.autoUpdateContinuous = true;
		(new BuiltInUpdateThread()).start();
	}

	public int pos (String name)
	{
		return name.charAt(0) % this.refMapSize;
	}

	public Data loc (String name)
	{
		if (name.equals(""))
			return null;

		for (Data data : this.vmem.get(pos(name)))
			if (data.getName().equals(name))
				return data;
		return null;
	}

	/**
	 * Defines (sets / declares) data inside vmem.
	 * @param name of the data
	 * @param ref data itself
	 */
	public void def (String name, Object ref)
	{
		Data r = loc(name);
		if (r == null)
			this.vmem.get(pos(name)).add(new Data(name, ref));
		else
			r.setRef(ref);
	}

	/**
	 * @param name of the variable
	 * @return variable contents as String
	 * @see String
	 */
	public String v (String name)
	{
		Data r = loc(name);
		if (r != null)
			if (r.getRef() instanceof String)
				return (String) r.getRef();
		return "";
	}

	/**
	 * @param name of the variable
	 * @return float representation of variable contents
	 * @see Float
	 */
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

	/**
	 * @param name of the variable
	 * @return integer value / representation of the variable
	 */
	public int d (String name)
	{
		if (!v(name).equals(""))
			return (int) Math.round(f(name));
		else
			return 0;
	}

	/**
	 * @param name of the variable
	 * @return reference to object stored in the memory
	 */
	public Object p (String name)
	{
		return loc(name) != null
			? loc(name).getRef()
			: null;
	}

	/**
	 * Dumps qs21 vmem contents.
	 */
	public void dumpMemory ()
	{
		for (ArrayList<Data> bank : this.vmem)
			for (Data data : bank)
				System.out.println(
						data.getName() + " -> " + (
								data.getRef() instanceof String
										? "'" + (String) data.getRef() + "'"
										: data.getRef().getClass().toString()
						)
				);
	}

	/**
	 * Converts float to a valid qs21 string data representation.
	 * @param f value to be converted
	 * @return converted float (without rounding error)
	 */
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


	/**
	 * Powers the qsvm.
	 */
	public void update ()
	{
		int inlineYields = 0;
		for (int i = scheduler.size() - 1; i >= 0; i--)
			if (scheduler.get(i).isReady(vm))
			{
				scheduler.remove(i);
				if (inlineYields++ > limitYields)
					break;
			}
	}

	/**
	 * Built in thread that will update qsvm as long
	 * as isALive() returns true. Also, will pause in
	 * between updates for autoUpdateDelay [ms].
	 */
	private class BuiltInUpdateThread extends Thread
	{
		@Override
		public void run()
		{
			while (vm.isAlive())
			{
				update();
				try { Thread.sleep(getAutoUpdateDelay());  }
				catch (Exception e) { e.printStackTrace(); }
			}
		}
	}

	/**
	 * Yields evaluation of given expression for a given amount of milliseconds.
	 * @param timeOut after which expression gets evaluated
	 * @param expr expression to evaluate
	 */
	public void yield (long timeOut, String expr)
	{
		if (scheduler.size() < this.schedulerSize)
			scheduler.add(new Yield(System.currentTimeMillis() + timeOut, expr));
	}

	/**
	 * Puts expression onto scheduler with no delay whatsoever - so
	 * that it gets evaluated as soon as possible. Preferred way to
	 * inject code to the running vm.
	 * @param expr expression to evaluate
	 */
	public void yield (String expr)
	{
		if (scheduler.size() < this.schedulerSize)
			scheduler.add(new Yield(System.currentTimeMillis(), expr));
	}

	/**
	 * Clears callCounter flag so that more code can get evaluated.
	 * To be used only by scheduler!
	 */
	void clearCallCounter ()
	{
		this.callCounter = 0;
	}


	/**
	 * @return current busy flag status.
	 */
	public boolean isBusy () {
		return busy;
	}

	/**
	 * Makes sure isAlive() returns true, even though virtual machine
	 * might not be performing anything at this moment.
	 * Could be used by blocking calls with callbacks to make sure
	 * vm doesn't get accidentally destroyed.
	 * @param busy new flag status
	 */
	public void setBusy (boolean busy) {
		this.busy = busy;
	}

	public long getAutoUpdateDelay() {
		return autoUpdateDelay;
	}

	public void setAutoUpdateDelay(long autoUpdateDelay) {
		this.autoUpdateDelay = autoUpdateDelay;
	}

	public void setMaxCalls(int maxCalls) {
		this.maxCalls = maxCalls;
	}

	public void setSchedulerSize(int schedulerSize) {
		this.schedulerSize = schedulerSize;
	}

	public void setLimitYields(int limitYields) {
		this.limitYields = limitYields;
	}

	public void setRefMapSize (int refMapSize) {
		this.refMapSize = refMapSize;
		// FIXME: Actually resize existing memory map ..
	}

	public ArrayList<ArrayList<Data>> getVmem ()
	{
		return this.vmem;
	}

	/**
	 * Safest way to determine whether the qs21 vm is still running or not,
	 * provided third party libraries follow qs21 design principles.
	 * @return whether the virtual machine is still running or not
	 */
	public boolean isAlive ()
	{
		return (this.autoUpdateContinuous || this.scheduler.size() > 0 || this.busy);
	}


	/**
	 * Destroys qsvm by clearing scheduler and vmem entries.
	 * Java handles the rest.
	 */
	public void destroy ()
	{
		this.autoUpdateContinuous = false;
		this.scheduler.clear();
		this.vmem.clear();
		this.busy = false;
	}
}
