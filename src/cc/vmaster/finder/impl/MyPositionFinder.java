package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import cc.vmaster.Phone;
import cc.vmaster.finder.TimeRecodFinder;
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

	private final RGB RGB_TARGET = new RGB(40, 43, 86);// 瓶子RGB色值

	public static MyPositionFinder getInstance() {
		return new MyPositionFinder();
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		clearDebug();

		int maxX = minInt;// 初始化X最大值
		int minX = maxInt;// 初始化X最小值
		int maxY = minInt;// 初始化Y最大值
		int minY = maxInt;// 初始化Y最小值

		CoordinateChecker.checkAdjustPoints(image, beginPoint, endPoint);
		for (int x = beginPoint[0]; x < endPoint[0]; x++) {
			for (int y = beginPoint[1]; y < endPoint[1]; y++) {
				int pixel = image.getRGB(x, y);
				RGB rgb = RGB.calcRGB(pixel);
				if (this.matched(rgb, RGB_TARGET, 16)) {
					maxX = Math.max(maxX, x);
					minX = Math.min(minX, x);
					maxY = Math.max(maxY, y);
					minY = Math.min(minY, y);

					if (debug()) {
						points.add(new int[] { x, y });
					}
				}
			}
		}

		int[] result = new int[2];
		result[0] = (maxX + minX) / 2 + 3;
		result[1] = maxY;

		return result;
	}

	@Override
	protected void clearDebug() {
		points.clear();
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
