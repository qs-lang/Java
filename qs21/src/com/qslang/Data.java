package com.qslang;

/**
 * Single entry in qsvm vmem.
 */
class Data
{
	private String name;
	private Object ref;

	Data(String name, Object ref)
	{
		this.name = name;
		this.ref = ref;
	}

	void setRef (Object ref)
	{
		this.ref = ref;
	}

	String getName ()
	{
		return this.name;
	}

	Object getRef ()
	{
		return this.ref;
	}

}
