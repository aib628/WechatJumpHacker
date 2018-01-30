package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
 * 寻找瓶子当前位置
 * 
 * beginPoint:起始位置坐标，去除手机边上部分，测试1080P值为：Phone.getBeginPoint();
 * 
 * endPoint:结束位置坐标，去除手机边上部分，测试1080P值为：Phone.getEndPoint();
 * 
 * @author VMaster
 *
 */
public class MyPositionFinder extends TimeRecodFinder {

	public static MyPositionFinder getInstance() {
		return new MyPositionFinder();
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		clearDebug();

		CoordinateChecker.checkAdjustPoints(image, beginPoint, endPoint);
		for (int x = beginPoint[0]; x < endPoint[0]; x++) {
			for (int y = beginPoint[1]; y < endPoint[1]; y++) {
				int pixel = image.getRGB(x, y);
				RGB rgb = RGB.calcRGB(pixel);
				if (matched(rgb, RGB_TARGET_BOTTLE, 16)) {
					points.add(new int[] { x, y });
				}
			}
		}

		if (points.size() == 0) {
			System.out.println("请确认游戏已启动且手机牌亮屏状态后重启该程序再行尝试...");
			System.exit(0);
		}

		removeImpossible();
		Collections.sort(points, XLineAscComparator.instance);
		int minX = points.get(0)[0];
		int maxX = points.get(points.size() - 1)[0];

		Collections.sort(points, YLineAscComparator.instance);
		int maxY = points.get(points.size() - 1)[1];

		int[] result = new int[2];
		result[0] = (maxX + minX) / 2;
		result[1] = maxY;

		return result;
	}

	private void removeImpossible() {
		removeXLineDiscontinuousPoints();
		removeYLineDiscontinuousPoints();
	}

	/**
	 * 按X轴间隔大于一定值的进行分组，然后保留点数多的集合。并将结果同步回points中
	 */
	private void removeXLineDiscontinuousPoints() {
		Map<Integer, PixelContainer> map = new HashMap<Integer, PixelContainer>();
		Collections.sort(points, XLineAscComparator.instance);

		int[] point0 = points.get(0);
		Iterator<int[]> iterator = points.iterator();
		while (iterator.hasNext()) {
			int[] point = iterator.next();
			if (point == point0 || point[0] - point0[0] < 80) {
				point0 = point;
				if (map.size() == 0) {
					map.put(1, new PixelContainer(point));
				} else {
					map.get(map.size()).addCount(point);
				}
			} else {
				point0 = point;
				map.put(map.size() + 1, new PixelContainer(point));
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.reorderCountMap(map);
		MapHelper.removeUseless(sortedMap, 0, 1);

		points.clear();
		for (PixelContainer pixel : sortedMap.values()) {
			points.addAll(pixel.pointList);
		}
	}

	/**
	 * 按Y轴将间隔大于定值的点进行分组，然后移除点数少，保留最大那项
	 */
	private void removeYLineDiscontinuousPoints() {
		Map<Integer, PixelContainer> map = new HashMap<Integer, PixelContainer>();
		Collections.sort(points, YLineAscComparator.instance);

		int[] point0 = points.get(0);
		Iterator<int[]> iterator = points.iterator();
		while (iterator.hasNext()) {
			int[] point = iterator.next();
			if (point == point0 || point[1] - point0[1] < 80) {
				point0 = point;
				if (map.size() == 0) {
					map.put(1, new PixelContainer(point));
				} else {
					map.get(map.size()).addCount(point);
				}
			} else {
				point0 = point;
				map.put(map.size() + 1, new PixelContainer(point));
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.reorderCountMap(map);
		MapHelper.removeUseless(sortedMap, 0, 1);

		points.clear();
		for (PixelContainer pixel : sortedMap.values()) {
			points.addAll(pixel.pointList);
		}
	}

	public static void main(String[] args) throws IOException {
		MyPositionFinder finder = new MyPositionFinder();
		finder.debug(true);

		URL url = IOUtils.getURL(finder.getClass(), "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		long costs = 0;
		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (!file.getName().endsWith(".png")) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());
			Phone.width = image.getWidth();
			Phone.height = image.getHeight();

			int[] point = finder.findAndRecord(image, Phone.getBeginPoint(), Phone.getEndPoint());
			System.out.println(String.format("当前位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s", finder.getMilliCosts()));

			TimeRecoder recoder = getRecoder().begin();
			int width = image.getWidth();
			int height = image.getHeight();
			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = bufferedImage.getGraphics();
			graphics.drawImage(image, 0, 0, width, height, null); // 绘制缩小后的图

			graphics.setColor(Color.white);
			finder.debug(graphics, finder.points);

			graphics.setColor(Color.red);
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);// 标记位置
			graphics.dispose();

			File descFile = new File(url.getPath() + "/found", file.getName());
			if (!descFile.exists()) {
				descFile.mkdirs();
				descFile.createNewFile();
			}

			costs += recoder.end();

			ImageIO.write(bufferedImage, "png", descFile);
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));
	}
}
