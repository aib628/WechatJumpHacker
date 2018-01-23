package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import cc.vmaster.Phone;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.finder.helper.MapHelper;
import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.finder.helper.XLineAscComparator;
import cc.vmaster.finder.helper.YLineAscComparator;
import cc.vmaster.helper.CoordinateChecker;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.RGB;

/**
 * 
 * 下一位置寻找器
 * 
 * @author VMaster
 *
 */
public class NextCenterFinder extends TimeRecodFinder {

	private RGB RGB_TARGET_BG = new RGB(255, 210, 210);// 默认背景色
	private RGB RGB_TARGET_SHADOW = new RGB(178, 149, 148);// 影子RGB色值
	private RGB RGB_TARGET_SHADOW_2 = new RGB(178, 149, 100);// 影子RGB色值
	private final RGB RGB_TARGET_GAME_OVER = new RGB(51, 46, 44);// 游戏结束RGB色值

	public static NextCenterFinder getInstance() {
		return new NextCenterFinder();
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		CoordinateChecker.checkAdjustPoints(image, beginPoint, endPoint);
		clearDebug();

		// 取出背景色，判断是否游戏结束
		RGB overBg = RGB.calcRGB(image.getRGB(image.getWidth() / 2, 5));
		if (matched(overBg, RGB_TARGET_GAME_OVER, 16)) {
			return null;// 有可能是游戏结束了
		}

		int width = image.getWidth();
		Map<Integer, PixelContainer> countMap = new HashMap<Integer, PixelContainer>();
		for (int y = beginPoint[1]; y < endPoint[1]; y++) {
			RGB bg = RGB.calcRGB(image.getRGB(width - 5, y));
			this.RGB_TARGET_BG = bg;
			for (int x = beginPoint[0]; x < endPoint[0]; x++) {
				RGB rgb = RGB.calcRGB(image.getRGB(x, y));
				if (matched(rgb, bg, 16)) {
					continue;
				}

				classifyPixel(countMap, rgb, new int[] { x, y }, 16);
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.sortMapByValue(countMap);
		MapHelper.removeUseless(sortedMap, 0, 3);
		removeImposible(sortedMap);

		if (debug()) {
			pixels.addAll(sortedMap.values());
		}

		List<int[]> points = new ArrayList<int[]>();
		for (PixelContainer pixel : sortedMap.values()) {
			points.addAll(pixel.pointList);
		}

		Collections.sort(points, XLineAscComparator.instance);

		int[] min = points.get(0);
		int[] max = points.get(points.size() - 1);

		return new int[] { (min[0] + max[0]) / 2, min[1] };
	}

	/**
	 * 移除不太可能的点，比如点最大值与最小值相差太大
	 */
	private void removeImposible(Map<Integer, PixelContainer> sortedMap) {
		// 移除影子
		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Integer, PixelContainer> e = iterator.next();

			if (sortedMap.size() > 1) {
				if (matched(RGB.calcRGB(e.getKey()), RGB_TARGET_SHADOW, 16)) {
					if (debug()) {
						System.out.println("通过背景色移除");
					}

					iterator.remove();
					continue;
				}

				if (matched(RGB.calcRGB(e.getKey()), RGB_TARGET_SHADOW_2, 16)) {
					if (debug()) {
						System.out.println("通过背景色2移除");
					}

					iterator.remove();
					continue;
				}
			}
		}

		if (sortedMap.size() <= 1) {
			return;
		}

		// 区域长度
		iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			List<int[]> points = iterator.next().getValue().pointList;

			// 横向看
			if (sortedMap.size() > 1) {
				Collections.sort(points, XLineAscComparator.instance);

				int[] min = points.get(0);
				int[] max = points.get(points.size() - 1);
				if ((max[0] - min[0]) > 500) {
					if (debug()) {
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
				if ((max[0] - min[0]) > 500) {
					if (debug()) {
						System.out.println("通过纵长移除");
					}

					iterator.remove();
					continue;
				}
			}
		}

		if (sortedMap.size() <= 1) {
			return;
		}

		// 放大误差
		iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Integer, PixelContainer> e = iterator.next();

			if (sortedMap.size() > 1) {
				if (matched(RGB.calcRGB(e.getKey()), RGB_TARGET_BG, 20)) {
					if (debug()) {
						System.out.println("通过放大误差移除");
					}

					iterator.remove();
					continue;
				}
			}
		}

		if (sortedMap.size() <= 1) {
			return;
		}

		// 按数量移除
		int size = sortedMap.size();
		int removeMinCount = size - 1;// 留1个
		iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			iterator.next();

			if (sortedMap.size() > 1) {
				removeMinCount++;
				if (size - removeMinCount < 0) {
					if (debug()) {
						System.out.println("通过统计数量移除");
					}

					iterator.remove();
					continue;
				}
			}
		}
	}

	private void classifyPixel(Map<Integer, PixelContainer> countMap, RGB rgb, int[] point, int tolerance) {
		if (countMap.size() == 0) {
			countMap.put(rgb.pixel, new PixelContainer(point));
			return;
		}

		boolean found = false;
		for (Entry<Integer, PixelContainer> e : countMap.entrySet()) {
			RGB target = RGB.calcRGB(e.getKey());

			// pixel与Map中存储像素存在相似
			if (matched(rgb, target, tolerance)) {
				e.getValue().addCount(point);
				found = true;
				break;
			}
		}

		if (!found) {
			countMap.put(rgb.pixel, new PixelContainer(point));
		}
	}

	public static void main(String[] args) throws IOException {
		MyPositionFinder My_POSITION = MyPositionFinder.getInstance();
		BottleTopFinder BOTTLE_TOP = BottleTopFinder.getInstance();
		NextCenterFinder NEXT_CENTER = NextCenterFinder.getInstance();
		NEXT_CENTER.debug(true);// 开启Debug

		URL url = IOUtils.getURL(NEXT_CENTER.getClass(), "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		long costs = 0;
		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (!file.getName().endsWith(".png")) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());
			int[] position = My_POSITION.find(image, Phone.getBeginPoint(), Phone.getEndPoint());
			if (CoordinateChecker.invalidPoint(position)) {
				break;// 未找到当前坐标
			}

			System.out.println(String.format("当前位置坐标：(%s,%s)", position[0], position[1]));
			int[] bottleTop = BOTTLE_TOP.find(image, position, null);
			int skipHeight = position[1] - bottleTop[1];

			int[] nextCenterEndPoint = new int[] { Phone.getEndPoint()[0], position[1] - skipHeight };
			int[] point = NEXT_CENTER.findAndRecord(image, Phone.getBeginPoint(), nextCenterEndPoint);
			System.out.println(String.format("下一中心位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s", NEXT_CENTER.getMilliCosts()));

			File descFile = new File(url.getPath() + "/found", file.getName());
			if (!descFile.exists()) {
				descFile.mkdirs();
				descFile.createNewFile();
			}

			TimeRecoder recoder = getRecoder().begin();
			int width = image.getWidth();
			int height = image.getHeight();

			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = bufferedImage.getGraphics();
			graphics.drawImage(image, 0, 0, width, height, null); // 绘制缩小后的图

			graphics.setColor(Color.white);
			graphics.fillRect(position[0] - 5, position[1] - 5, 10, 10);

			int i = 0;
			Color[] colors = new Color[] { Color.blue, Color.green, Color.orange };
			for (PixelContainer pixel : NEXT_CENTER.pixels) {
				i++;
				graphics.setColor(colors[i % 3]);
				NEXT_CENTER.debug(graphics, pixel.pointList);
			}

			graphics.setColor(Color.red);
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);// 标记位置
			graphics.dispose();

			ImageIO.write(bufferedImage, "png", descFile);

			costs += recoder.end();
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));
	}

}
