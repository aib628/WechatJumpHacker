package cc.vmaster.finder.helper;

import java.util.Comparator;

/**
 * X轴从小到大排序
 * 
 * @author FanRenwei
 *
 */
public class XLineAscComparator implements Comparator<int[]> {

	public static final XLineAscComparator instance = new XLineAscComparator();

	private XLineAscComparator() {
	}

	@Override
	public int compare(int[] a, int[] b) {
		return Integer.valueOf(a[0]).compareTo(b[0]);
	}

}
