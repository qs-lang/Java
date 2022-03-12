package com.qslang;

/**
 * Extend this class in order to create custom qs21 functions.
 * Then use def in order to link your newly created custom
 * function with qs21 virtual machine.
 */
public abstract class CustomFunction
{
	public abstract String Function (qs21 vm);
}
