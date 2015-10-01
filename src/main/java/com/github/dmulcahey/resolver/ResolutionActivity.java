package com.github.dmulcahey.resolver;

public interface ResolutionActivity<T> extends Ordered{

	void perform(T input);
	
}
