package com.bms.resolver;

public interface ResolutionActivity<T> extends Ordered{

	void perform(T input);
	
}
