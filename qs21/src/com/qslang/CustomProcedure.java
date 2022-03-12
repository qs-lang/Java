package com.qslang;

/**
 * Extend this class in order to create custom qs21 procedures.
 * Then use def in order to link your newly created custom
 * procedure with qs21 virtual machine.
 */
public abstract class CustomProcedure
{
	public abstract void Procedure (qs21 vm);
}
