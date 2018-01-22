package cc.vmaster.finder.helper;

import java.util.Comparator;

/**
 * Y轴从小到大排序
 * 
 * @author FanRenwei
 *
 */
public class YLineAscComparator implements Comparator<int[]> {

	public static final YLineAscComparator instance = new YLineAscComparator();

	private YLineAscComparator() {
	}

	@Override
	public int compare(int[] a, int[] b) {
		return Integer.valueOf(a[1]).compareTo(b[1]);
	}

}
