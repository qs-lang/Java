package com.qslang;

class Scheduled
{
	private long timeOut;
	private String expr;

	Scheduled (long timeOut, String expr)
	{
		this.timeOut = timeOut;
		this.expr = expr;
	}

	boolean IsReady (qs21 vm)
	{
		if (timeOut < System.currentTimeMillis())
		{
			vm.Eval(this.expr);
			return true;
		}
		return false;
	}
}
