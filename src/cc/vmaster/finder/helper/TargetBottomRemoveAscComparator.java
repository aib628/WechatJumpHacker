package cc.vmaster.finder.helper;

import java.util.Comparator;
import java.util.Map.Entry;

public class TargetBottomRemoveAscComparator implements Comparator<Entry<Integer, TargetBottomRemove>> {

	public static final TargetBottomRemoveAscComparator instance = new TargetBottomRemoveAscComparator();

	@Override
	public int compare(Entry<Integer, TargetBottomRemove> a, Entry<Integer, TargetBottomRemove> b) {
		return Integer.valueOf(a.getValue().max).compareTo(b.getValue().max);
	}

}
