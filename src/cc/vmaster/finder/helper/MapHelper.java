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
	 * 使用 Map按value对PixelContainer进行降序排序
	 * 
	 * @return 传入无序Map，返回有序LinkedHashMap实例
	 */
	public static Map<Integer, PixelContainer> sortMapByValue(Map<Integer, PixelContainer> map) {
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

}
