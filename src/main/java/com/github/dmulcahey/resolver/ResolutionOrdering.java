package com.github.dmulcahey.resolver;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

public class ResolutionOrdering<T extends Ordered> extends Ordering<T> {

	@Override
	public int compare(Ordered left, Ordered right) {
		return Ints.compare(left.getOrder(), right.getOrder());
	}

}
