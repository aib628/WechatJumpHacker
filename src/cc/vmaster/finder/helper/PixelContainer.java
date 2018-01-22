package cc.vmaster.finder.helper;

import java.util.ArrayList;
import java.util.List;

public class PixelContainer implements Comparable<Integer> {

		public Integer count;
		public List<int[]> pointList = new ArrayList<int[]>();

		public PixelContainer(int[] point) {
			addCount(point);
		}

		public void addCount(int[] point) {
			addCount(point[0], point[1]);
		}

		public void addCount(int x, int y) {
			if (count == null) {
				count = 1;
			} else {
				count += 1;
			}

			pointList.add(new int[] { x, y });
		}

		@Override
		public int compareTo(Integer o) {
			return count.compareTo(o);
		}

		@Override
		public String toString() {
			String str = "Count : %s , Points : %s";
			return String.format(str, count, pointList.size());
		}

	}