package com.qslang;

/**
 * Single thing to put away onto scheduler.
 */
class Yield
{
	private long timeOut;
	private String expr;

	Yield(long timeOut, String expr)
	{
		this.timeOut = timeOut;
		this.expr = expr;
	}

	/**
	 * Checks if yielded thing is ready and if so, evaluates it.
	 * @param vm
	 * @return whether the yielded thing has been evaluated already
	 */
	boolean isReady (qs21 vm)
	{
		if (timeOut < System.currentTimeMillis())
		{
			vm.clearCallCounter();
			vm.eval(this.expr);
			return true;
		}
		return false;
	}
}
