package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import cc.vmaster.Hacker;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.finder.helper.MapHelper;
import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.RGB;

/**
 * 寻找瓶子所在中心点
 * 
 * @author VMaster
 */
public class CenterFinder extends TimeRecodFinder {

	private Collection<PixelContainer> pixels;
	private RGB RGB_TARGET_BG = new RGB(255, 210, 210);// 背景RGB色值
	private RGB RGB_TARGET_BOTTLE = new RGB(40, 43, 86);// 瓶子RGB色值
	private RGB RGB_TARGET_SHADOW = new RGB(178, 149, 148);// 影子RGB色值
	private RGB RGB_TARGET_SHADOW_2 = new RGB(178, 149, 100);// 影子RGB色值

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		int width = image.getWidth();
		int height = image.getHeight();
		adaptRadio(beginPoint, endPoint, width, height);

		Map<Integer, PixelContainer> countMap = new HashMap<Integer, PixelContainer>();
		for (int x = beginPoint[0]; x < endPoint[0]; x++) {
			for (int y = beginPoint[1]; y < endPoint[1]; y++) {
				classifyPixel(countMap, image, new int[] { x, y }, 16);
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.sortMapByValue(countMap);
		MapHelper.removeUseless(sortedMap, 0, 2);

		if (debug()) {
			pixels = sortedMap.values();
		}

		return new int[] { 0, 0 };
	}

	private void classifyPixel(Map<Integer, PixelContainer> countMap, BufferedImage image, int[] point, int tolerance) {
		int pixel = image.getRGB(point[0], point[1]);
		RGB rgb = RGB.calcRGB(pixel);

		// 背景色，移除

		if (matched(rgb, RGB_TARGET_BG, tolerance)) {
			return;
		}

		if (matched(rgb, RGB_TARGET_BOTTLE, tolerance + 5)) {
			return;
		}

		if (countMap.size() == 0) {
			countMap.put(pixel, new PixelContainer(point));
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
			countMap.put(pixel, new PixelContainer(point));
		}
	}

	public static void main(String[] args) throws IOException {
		MyPositionFinder My_POSITION = new MyPositionFinder();

		CenterFinder CENTER = new CenterFinder();
		CENTER.debug(true);// 开启Debug

		URL url = IOUtils.getURL(CENTER.getClass(), "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		long costs = 0;
		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (!file.getName().endsWith(".png")) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());
			int[] positionPoint = My_POSITION.find(image, Hacker.getBeginPoint(), Hacker.getEndPoint());
			if (My_POSITION.invalidPoint(positionPoint)) {
				break;// 未找到当前坐标
			}

			System.out.println(String.format("当前位置坐标：(%s,%s)", positionPoint[0], positionPoint[1]));
			int[] centerBeginPoint = new int[] { positionPoint[0] - 200, positionPoint[1] - 200 };
			int[] centerEndPoint = new int[] { positionPoint[0] + 200, positionPoint[1] + 200 };
			int[] point = CENTER.findAndRecord(image, centerBeginPoint, centerEndPoint);
			System.out.println(String.format("下一中心位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s", CENTER.getMilliCosts()));

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

			graphics.setColor(Color.white);// 我的位置
			graphics.fillRect(positionPoint[0] - 5, positionPoint[1] - 5, 10, 10);

			int i = 0;
			Color[] colors = new Color[] { Color.blue, Color.green, Color.orange };
			for (PixelContainer pixel : CENTER.pixels) {
				i++;
				graphics.setColor(colors[i % 3]);
				CENTER.debug(graphics, pixel.pointList);
			}

			graphics.setColor(Color.red);// 中心位置
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);// 标记位置
			graphics.dispose();

			ImageIO.write(bufferedImage, "png", descFile);

			costs += recoder.end();
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));

	}

}
