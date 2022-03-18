package com.qslang;

/**
 * Single entry in memory pool.
 */
public class Slab
{
	private String name;
	private Object ref;

	Slab (String name, Object ref)
	{
		this.name = name;
		this.ref = ref;
	}

	void setRef (Object ref)
	{
		this.ref = ref;
	}

	public String getName ()
	{
		return this.name;
	}

	public Object getRef ()
	{
		return this.ref;
	}

}
