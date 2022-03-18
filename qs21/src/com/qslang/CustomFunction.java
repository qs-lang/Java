package com.qslang;

/**
 * Extend this class in order to create custom qs21 function.
 * Then use def in order to link your newly created custom
 * function with qsvm.
 */
public abstract class CustomFunction
{
	public abstract String Function (qs21 vm);
}
