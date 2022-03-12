package com.qslang;

/**
 * Single entry in qs21 refMap / Memory
 */
class Ref
{
	private String name;
	private Object ref;

	Ref (String name, Object ref)
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
