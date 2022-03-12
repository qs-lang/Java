package com.qslang;

import java.util.ArrayList;

/**
 * qs21 Virtual Machine, for main methods see:
 * update, yield and def
 */
public class qs21
{
	// Limits:
	private int schedulerSize;
	private int limitYields;
	private int refMapSize;
	private int maxCalls;

	// Fields:
	private ArrayList<ArrayList<Ref>> refMap;
	private ArrayList<Yield> scheduler;
	private long autoUpdateDelay;
	private boolean continuous;
	private int callCounter;
	private boolean busy;
	private qs21 vm;


	/**
	 * Creates a new instance of qs21 virtual machine.
	 */
	public qs21 ()
	{
		useDefaultLimits();
		initVM();
	}

	/**
	 * Evaluates given qs21 expression without resetting the countCalls field. Only to be invoked from
	 * custom qs21 functions or procedures bodies.
	 * Using it to inject code to the vm, might cause errors and undefined behaviour.
	 * In order to inject code use "yield" method.
	 * @param expr a valid qs21 expression to evaluate
	 * @return results of evaluations
	 */
	// ----------------------------------------
	// -- Eval And Caller
	public String eval (String expr)
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
						sFun = bNext ? this.eval(val) : val;
					else
						sArgs[dArg - 1] = bNext ? this.eval(val) : val;
						// -- def(sFun + "-" + (dArg - 1), bNext ? this.Eval(val) : val); // .mut lim.

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
	 * Performs calls inside qs21 virtual machine. Will either: invoke custom procedure/function
	 * or evaluate expression / data found inside given memory location.
	 * @param name of the memory location inside refMap
	 * @param argc amount of the arguments declared upon calling
	 * @return results of the call
	 */
	public String call (String name, int argc)
	{
		if (this.callCounter++ > this.maxCalls) {
			def("!err", "Reached maximum calls!");
			return "";
		}

		def(name + "-len", String.format("%d", argc));
		Ref ref = loc(name);

		if (ref == null)
			return "";

		if (ref.getRef() instanceof String)
			return eval((String) ref.getRef());

		else if (ref.getRef() instanceof CustomProcedure)
			((CustomProcedure) ref.getRef()).Procedure(this);

		else if (ref.getRef() instanceof CustomFunction)
			return ((CustomFunction) ref.getRef()).Function(this);

		return "";
	}

	private void useDefaultLimits ()
	{
		setSchedulerSize(0xFF);
		setLimitYields(0xF);
		setRefMapSize(0xF);
		setMaxCalls(0xFF);
		this.continuous = false;
		this.autoUpdateDelay = 100L;
	}

	private void initVM ()
	{
		this.continuous = false;
		this.callCounter = 0;
		this.scheduler = new ArrayList<Yield>();
		this.refMap = new ArrayList<ArrayList<Ref>>();
		for (int i = 0; i < refMapSize; i++)
			this.refMap.add(new ArrayList<Ref>());
		vm = this;
		new stdlib(this);
	}

	/**
	 * Creates new thread for qs21 virtual machine. New thread will perform vm.update() method.
	 * Continuous thread will run until vm.destroy() method is invoked. Non continuous thread
	 * stops working as soon as qs21 virtual machine does.
	 * @param continuous stop condition for the thread loop
	 */
	public void autoUpdate (boolean continuous)
	{
		this.continuous = continuous;
		(new BuiltInUpdateThread()).start();
	}

	public int pos (String name)
	{
		return name.charAt(0) % this.refMapSize;
	}

	public Ref loc (String name)
	{
		if (name.equals(""))
			return null;

		for (Ref ref : this.refMap.get(pos(name)))
			if (ref.getName().equals(name))
				return ref;
		return null;
	}

	/**
	 * Defines data inside refMap (a qs21 virtual machine memory)
	 * @param name of the data
	 * @param ref data itself
	 */
	public void def (String name, Object ref)
	{
		Ref r = loc(name);
		if (r == null)
			this.refMap.get(pos(name)).add(new Ref(name, ref));
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
		Ref r = loc(name);
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
	 * Dumps qs21 refMap (memory) contents.
	 */
	public void dumpMemory ()
	{
		for (ArrayList<Ref> bank : this.refMap)
			for (Ref ref : bank)
				System.out.println(
						ref.getName() + " -> " + (
								ref.getRef() instanceof String
										? "'" + (String) ref.getRef() + "'"
										: ref.getRef().getClass().toString()
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
	 * Updates qs12 virtual machine - mainly scheduler.
	 * Evaluates every yield from the list that has reached its time.
	 * Cannot evaluated more than limitYields flag - a safety feature.
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
	 * Built in thread that will update qs21 vm as long
	 * as isALive() returns true. Also, will pause in
	 * between updates for getAutoUpdateDelay() ms.
	 */
	private class BuiltInUpdateThread extends Thread
	{
		@Override
		public void run()
		{
			while (isAlive())
			{
				update();
				try { Thread.sleep(getAutoUpdateDelay());  }
				catch (Exception e) { e.printStackTrace(); }
			}
		}
	}

	/**
	 * Yields evaluation of given expression for given amount of milliseconds.
	 * @param timeOut after which expression gets evaluated
	 * @param expr expression to evaluate
	 */
	public void yield (long timeOut, String expr)
	{
		if (scheduler.size() < this.schedulerSize)
			scheduler.add(new Yield(System.currentTimeMillis() + timeOut, expr));
	}

	/**
	 * Puts expression onto scheduler with minimum possible delay, so
	 * that it gets evaluated as soon as possible. Used to inject code
	 * to the running qs21 vm.
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

	/**
	 * Safest way to determine whether the qs21 vm is still running or not,
	 * provided third party libraries follow qs21 design principles.
	 * @return whether the virtual machine is still running or not
	 */
	public boolean isAlive ()
	{
		return (this.continuous || this.scheduler.size() > 0 || this.busy);
	}
}
