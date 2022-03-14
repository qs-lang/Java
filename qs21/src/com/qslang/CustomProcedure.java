package com.qslang;

/**
 * Extend this class in order to create custom qs21 procedure.
 * Then use def in order to link your newly created custom
 * procedure with qsvm.
 */
public abstract class CustomProcedure
{
	public abstract void Procedure (qs21 vm);
}
