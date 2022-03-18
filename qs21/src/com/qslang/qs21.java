package com.qslang;

import java.util.ArrayList;

/**
 * qs21 Interpreter
 */
public class qs21
{
	private int limSchedulerPoolSize;
	private int limMaxSchedulerEvals;
	private int limPoolSize;
	private int limMaxCalls;

	private ArrayList<ArrayList<Slab>> pool;							// Virtual Memory Pool
	private ArrayList<Yield> scheduler;										// Scheduler Pool

	private boolean autoUpdateContinuous;
	private long autoUpdateDelay;
	private int callCounter;															// Protects native call stack
	private boolean busy;																	// forces isAlive() -> true

	private qs21 vm;


	public qs21 ()
	{
		useDefaultLimits();
		initVM();
	}

	/**
	 * Yields evaluation of given expression for a given amount of time [ms].
	 * @param timeOut after which expression gets evaluated
	 * @param expr expression to evaluate
	 */
	public void yield (long timeOut, String expr)
	{
		if (scheduler.size() < this.limSchedulerPoolSize)
			scheduler.add(new Yield(System.currentTimeMillis() + timeOut, expr));
	}

	/**
	 * Injects code into vm
	 * @param expr expression to evaluate
	 */
	public void yield (String expr)
	{
		if (scheduler.size() < this.limSchedulerPoolSize)
			scheduler.add(new Yield(System.currentTimeMillis(), expr));
	}

	/**
	 * Evaluates qs21 expression
	 * @param expr expression
	 * @return results of evaluation
	 */
	public String eval (String expr)
	{
		boolean bMode = true;
		boolean bNext = true;
		String sFun = null;
		StringBuilder sRet = new StringBuilder();
		String[] sArgs = new String[0xFF]; // FIXME: Use pool and rename variables l8er
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
	 * Performs calls inside interpreter.
	 * @param name to call
	 * @param argc amount of the arguments declared upon calling
	 * @return results of the call
	 */
	public String call (String name, int argc)
	{
		if (this.callCounter++ > this.limMaxCalls)
			return "";

		def(name + "-len", String.format("%d", argc));
		Slab slab = loc(name);

		if (slab == null)
			return "";

		if (slab.getRef() instanceof String)
			return eval((String) slab.getRef());

		else if (slab.getRef() instanceof CustomProcedure)
			((CustomProcedure) slab.getRef()).Procedure(this);

		else if (slab.getRef() instanceof CustomFunction)
			return ((CustomFunction) slab.getRef()).Function(this);

		return "";
	}

	public void useDefaultLimits ()
	{
		setLimSchedulerPoolSize(0xFF);
		setLimMaxSchedulerEvals(0xF);
		setLimPoolSize(0xF);
		setLimMaxCalls(0xFF);
		this.autoUpdateContinuous = false;
		this.autoUpdateDelay = 100L;
	}

	private void initVM ()
	{
		this.autoUpdateContinuous = false;
		this.callCounter = 0;
		this.scheduler = new ArrayList<Yield>();
		this.pool = new ArrayList<ArrayList<Slab>>();
		for (int i = 0; i < limPoolSize; i++)
			this.pool.add(new ArrayList<Slab>());
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

	/**
	 * @param name of variable
	 * @return index of inner pool inside outer memory pool
	 */
	public int pos (String name)
	{
		return name.charAt(0) % this.limPoolSize;
	}


	/**
	 * @param name of variable to lookup
	 * @return reference to a Slab object inside memory pool
	 */
	public Slab loc (String name)
	{
		if (name.equals(""))
			return null;

		for (Slab slab : this.pool.get(pos(name)))
			if (slab.getName().equals(name))
				return slab;
		return null;
	}

	/**
	 * Defines (sets / declares) data inside memory pool.
	 * @param name of the data
	 * @param ref data itself
	 */
	public void def (String name, Object ref)
	{
		Slab r = loc(name);
		if (r == null)
			this.pool.get(pos(name)).add(new Slab(name, ref));
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
		Slab r = loc(name);
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
	 * @return reference to object stored in the memory pool
	 */
	public Object p (String name)
	{
		return loc(name) != null
			? loc(name).getRef()
			: null;
	}

	/**
	 * Dumps qs21 memory pool contents.
	 */
	public void dumpPool ()
	{
		for (ArrayList<Slab> bank : this.pool)
			for (Slab slab : bank)
				System.out.println(
						slab.getName() + " -> " + (
								slab.getRef() instanceof String
										? "'" + (String) slab.getRef() + "'"
										: slab.getRef().getClass().toString()
						)
				);
	}

	/**
	 * Converts float to string (without float rounding error)
	 * @param f float value to convert
	 * @return string value
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
	 * Pumps life into vm
	 */
	public void update ()
	{
		int inlineYields = 0;
		for (int i = scheduler.size() - 1; i >= 0; i--)
			if (scheduler.get(i).isReady(vm))
			{
				scheduler.remove(i);
				if (inlineYields++ > limMaxSchedulerEvals)
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
	 * Clears callCounter flag
	 */
	public void clearCallCounter ()
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

	public void setLimMaxCalls(int limMaxCalls) {
		this.limMaxCalls = limMaxCalls;
	}

	public void setLimSchedulerPoolSize(int limSchedulerPoolSize) {
		this.limSchedulerPoolSize = limSchedulerPoolSize;
	}

	public void setLimMaxSchedulerEvals(int limMaxSchedulerEvals) {
		this.limMaxSchedulerEvals = limMaxSchedulerEvals;
	}

	public ArrayList<Yield> getScheduler () {
		return this.scheduler;
	}

	public void setLimPoolSize(int refMapSize) {
		this.limPoolSize = refMapSize;
		// FIXME: Actually resize existing memory pool and remap it ..
	}

	public ArrayList<ArrayList<Slab>> getPool()
	{
		return this.pool;
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
	 * Stops vm and its built-in threads. Also clears memory pool
	 * so that nothing else could be run on this very instance.
	 */
	public void destroy ()
	{
		this.autoUpdateContinuous = false;
		this.scheduler.clear();
		this.pool.clear();
		this.busy = false;
	}
}
