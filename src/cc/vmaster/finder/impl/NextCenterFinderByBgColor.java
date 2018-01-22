package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import cc.vmaster.Hacker;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.finder.helper.MapHelper;
import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.finder.helper.XLineAscComparator;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;

public class NextCenterFinderByBgColor extends TimeRecodFinder {

	private Collection<PixelContainer> pixels;
	private RGB RGB_TARGET_BG = new RGB(255, 210, 210);// 背景RGB色值
	private RGB RGB_TARGET_SHADOW = new RGB(178, 149, 148);// 影子RGB色值
	private RGB RGB_TARGET_SHADOW_2 = new RGB(178, 149, 100);// 影子RGB色值
	private RGB RGB_TARGET_GAME_OVER = new RGB(51, 46, 44);// 游戏结束RGB色值

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		int width = image.getWidth();
		int height = image.getHeight();
		adaptRadio(beginPoint, endPoint, width, height);

		RGB rgb = this.calcRGB(image.getRGB(width / 2, 5));// 取出背景色，防止切换背景
		if (!matched(rgb, RGB_TARGET_BG, 16)) {
			if (matched(rgb, RGB_TARGET_GAME_OVER, 16)) {
				return null;// 有可能是游戏结束了
			}

			RGB_TARGET_BG = rgb;
		}

		Map<Integer, PixelContainer> countMap = new HashMap<Integer, PixelContainer>();
		for (int x = beginPoint[0]; x < endPoint[0]; x++) {
			for (int y = beginPoint[1]; y < endPoint[1]; y++) {
				classifyPixel(countMap, image, new int[] { x, y }, 16);
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.sortMapByValue(countMap);
		MapHelper.removeUseless(sortedMap, 0, 4);
		//removeImposible(sortedMap);

		if (debug()) {
			pixels = sortedMap.values();
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
	
	private void classifyPixel(Map<Integer, PixelContainer> countMap, BufferedImage image, int[] point, int tolerance) {
		int pixel = image.getRGB(point[0], point[1]);
		RGB rgb = this.calcRGB(pixel);

		// 背景色，移除
		if (matched(rgb, RGB_TARGET_BG, tolerance)) {
			return;
		}

		if (countMap.size() == 0) {
			countMap.put(pixel, new PixelContainer(point));
			return;
		}

		boolean found = false;
		for (Entry<Integer, PixelContainer> e : countMap.entrySet()) {
			RGB target = this.calcRGB(e.getKey());

			// pixel与Map中存储像素存在相似
			if (matched(rgb, target, tolerance)) {
				e.getValue().addCount(point);
				found = true;
				break;
			}
		}

		if (!found) {
			countMap.put(pixel, new PixelContainer(point));
		}
	}

	public static void main(String[] args) throws IOException {
		NextCenterFinderByBgColor NEXT_CENTER_BY_BG = new NextCenterFinderByBgColor();
		NEXT_CENTER_BY_BG.debug(true);// 开启Debug

		URL url = IOUtils.getURL(NEXT_CENTER_BY_BG.getClass(), "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		long costs = 0;
		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (!file.getName().endsWith(".png")) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());

			int[] point = NEXT_CENTER_BY_BG.findAndRecord(image, Hacker.getBeginPoint(), Hacker.getEndPoint());

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

			int i = 0;
			Color[] colors = new Color[] { Color.blue, Color.green, Color.orange };
			for (PixelContainer pixel : NEXT_CENTER_BY_BG.pixels) {
				i++;
				graphics.setColor(colors[i % 3]);
				NEXT_CENTER_BY_BG.debug(graphics, pixel.pointList);
			}

			graphics.dispose();

			ImageIO.write(bufferedImage, "png", descFile);

			costs += recoder.end();
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));
	}

}
