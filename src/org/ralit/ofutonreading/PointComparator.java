package org.ralit.ofutonreading;

import java.util.Comparator;

public class PointComparator implements Comparator<Word> {

	@Override
	public int compare(Word lhs, Word rhs) {
		return lhs.getLeft() - lhs.getRight();
	}

}