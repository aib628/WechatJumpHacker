package cc.vmaster.finder.helper;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * PixelContainer按count值从大往小排序
 * 
 * @author FanRenwei
 */
public class PixelCountAscComparator implements Comparator<Map.Entry<Integer, PixelContainer>> {

	public static final PixelCountAscComparator instance = new PixelCountAscComparator();

	private PixelCountAscComparator() {
	}

	@Override
	public int compare(Entry<Integer, PixelContainer> a, Entry<Integer, PixelContainer> b) {
		return b.getValue().count.compareTo(a.getValue().count);
	}

}
