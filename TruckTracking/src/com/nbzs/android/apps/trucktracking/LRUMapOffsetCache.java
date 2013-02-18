package com.nbzs.android.apps.trucktracking;

import android.graphics.Point;
import com.google.protobuf.WireFormat;

import java.util.LinkedHashMap;


public class LRUMapOffsetCache extends LinkedHashMap<Integer, Point> {

	private int mCapacity;

	public LRUMapOffsetCache(final int aCapacity) {
		super(aCapacity + 2, 0.1f, true);
		mCapacity = aCapacity;
	}

	public void ensureCapacity(final int aCapacity) {
		if (aCapacity > mCapacity) {
			mCapacity = aCapacity;
		}
	}

	@Override
	public Point remove(final Object aKey) {
		final Point ret = super.remove(aKey);
		return ret;
	}

	@Override
	public void clear() {
		// remove them all individually so that they get recycled
		while (size() > 0) {
			remove(keySet().iterator().next());
		}

		// and then clear
		super.clear();
	}

	@Override
	protected boolean removeEldestEntry(final Entry<Integer, Point> aEldest) {
		if (size() > mCapacity) {
			remove(aEldest.getKey());
			// don't return true because we've already removed it
		}
		return false;
	}
}

