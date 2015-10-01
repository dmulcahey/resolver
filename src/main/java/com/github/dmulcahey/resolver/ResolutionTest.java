package com.github.dmulcahey.resolver;

public interface ResolutionTest<T> extends Ordered{
	
	ResolutionTestResult execute(T input);
	
}
