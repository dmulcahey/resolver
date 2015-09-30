package com.bms.resolver;

public interface ResolutionTest<T> extends Ordered{
	
	ResolutionTestResult execute(T input);
	
}
