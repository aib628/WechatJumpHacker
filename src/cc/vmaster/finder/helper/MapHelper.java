package cc.vmaster.finder.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapHelper {

	/**
	 * 使用 Map按value对PixelContainer进行降序排序:按Count数排
	 * 
	 * @return 传入无序Map，返回有序LinkedHashMap实例
	 */
	public static Map<Integer, PixelContainer> reorderCountMap(Map<Integer, PixelContainer> map) {
		Map<Integer, PixelContainer> sortedMap = new LinkedHashMap<Integer, PixelContainer>();
		List<Entry<Integer, PixelContainer>> entryList = new ArrayList<>(map.entrySet());
		Collections.sort(entryList, PixelCountAscComparator.instance);

		Iterator<Entry<Integer, PixelContainer>> iterator = entryList.iterator();
		while (iterator.hasNext()) {
			Entry<Integer, PixelContainer> e = iterator.next();
			sortedMap.put(e.getKey(), e.getValue());
		}

		return sortedMap;
	}

	/**
	 * 按Map中数组第一个元素进行升序排序
	 * 
	 * @param map
	 */
	public static Map<Integer, TargetBottomRemove> reorderMaxValueMap(Map<Integer, TargetBottomRemove> map) {
		Map<Integer, TargetBottomRemove> sortedMap = new LinkedHashMap<Integer, TargetBottomRemove>();
		List<Entry<Integer, TargetBottomRemove>> entryList = new ArrayList<>(map.entrySet());
		Collections.sort(entryList, TargetBottomRemoveAscComparator.instance);

		Iterator<Entry<Integer, TargetBottomRemove>> iterator = entryList.iterator();
		while (iterator.hasNext()) {
			Entry<Integer, TargetBottomRemove> e = iterator.next();
			sortedMap.put(e.getKey(), e.getValue());
		}

		return sortedMap;
	}

	/**
	 * 移除removeMaxCount个最大记录，留下candidateCount个作为候选，其余小值全部移除
	 * 
	 * @param sortedMap 数量从大往小排
	 * @param removeMaxCount 需要移除最大数量
	 * @param candidateCount 预留数量
	 */
	public static void removeUseless(Map<Integer, PixelContainer> sortedMap, int removeMaxCount, int candidateCount) {
		int size = sortedMap.size();
		int removeMinCount = size - candidateCount;// 留四个

		// size不够五个
		if (removeMinCount <= 0) {
			removeMinCount = 0;
		}

		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			iterator.next();
			if (removeMaxCount > 0) {
				iterator.remove();
				removeMaxCount--;
				continue;
			}

			removeMinCount++;
			if (size - removeMinCount < 0) {
				iterator.remove();
				continue;
			}
		}
	}

	/**
	 * 通过目标块最大宽度或最大高度移除
	 * 
	 * @param sortedMap
	 * @param maxLength 目标块可能的最大宽度及高度
	 * @param debug 是否Debug模式
	 */
	public static void removeByLength(Map<Integer, PixelContainer> sortedMap, int maxLength, boolean debug) {
		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			List<int[]> points = iterator.next().getValue().pointList;

			// 横向看
			if (sortedMap.size() > 1) {
				Collections.sort(points, XLineAscComparator.instance);

				int[] min = points.get(0);
				int[] max = points.get(points.size() - 1);
				if ((max[0] - min[0]) > maxLength) {
					if (debug) {
						System.out.println("通过横长移除");
					}

					iterator.remove();
					continue;
				}
			}

			// 纵向看
			if (sortedMap.size() > 1) {
				Collections.sort(points, YLineAscComparator.instance);

				int[] min = points.get(0);
				int[] max = points.get(points.size() - 1);
				if ((max[0] - min[0]) > maxLength) {
					if (debug) {
						System.out.println("通过纵长移除");
					}

					iterator.remove();
					continue;
				}
			}
		}
	}
}
