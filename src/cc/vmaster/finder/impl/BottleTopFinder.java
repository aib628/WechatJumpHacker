package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import cc.vmaster.IPhone;
import cc.vmaster.finder.IFinder;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.helper.CoordinateChecker;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.RGB;

/**
 * 寻找瓶子最高点，用于计算瓶子高度
 * 
 * beginPoint:瓶子当前位置坐标
 * 
 * endPoint:无效
 * 
 * @author FanRenwei
 *
 */
public class BottleTopFinder extends TimeRecodFinder {

	public static BottleTopFinder getInstance() {
		return new BottleTopFinder();
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		clearDebug();

		int width = image.getWidth();
		int height = image.getHeight();
		CoordinateChecker.checkAdjustBeginPoint(beginPoint, width, height);

		int min = maxInt;// 寻找最高点

		int x = beginPoint[0];
		for (int y = beginPoint[1]; y >= 0; y--) {
			RGB bg = RGB.calcRGB(image.getRGB(width - 5, y));
			int pixel = image.getRGB(x, y);
			if (matched(RGB.calcRGB(pixel), bg, 16)) {
				break;
			}

			if (debug()) {
				points.add(new int[] { x, y });
			}

			min = Math.min(min, y);
		}

		return new int[] { x, min };
	}

	public static void main(String[] args) throws IOException {
		MyPositionFinder positionFinder = MyPositionFinder.getInstance();
		BottleTopFinder bottleTopFinder = BottleTopFinder.getInstance();
		bottleTopFinder.debug(true);

		URL url = IOUtils.getURL(IFinder.class, "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		long costs = 0;
		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (!file.getName().endsWith(".png")) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());
			IPhone phone = getPhone(image);

			int[] position = positionFinder.find(image, phone.getBeginPoint(), phone.getEndPoint());
			System.out.println(String.format("当前位置：(%s,%s)", position[0], position[1]));

			int[] point = bottleTopFinder.findAndRecord(image, position, null);
			System.out.println(String.format("瓶子顶部位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s", bottleTopFinder.getMilliCosts()));

			TimeRecoder recoder = getRecoder().begin();
			int width = image.getWidth();
			int height = image.getHeight();
			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = bufferedImage.getGraphics();
			graphics.drawImage(image, 0, 0, width, height, null); // 绘制缩小后的图

			graphics.setColor(Color.black);
			bottleTopFinder.debug(graphics, bottleTopFinder.points);

			graphics.setColor(Color.white);
			graphics.fillRect(position[0] - 5, position[1] - 5, 10, 10);

			graphics.setColor(Color.red);
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);
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

	private static IPhone getPhone(final BufferedImage image) {
		IPhone phone = new IPhone() {

			@Override
			public int getWidth() {
				return image.getWidth();
			}

			@Override
			public int getHeight() {
				return image.getHeight();
			}

			@Override
			public int[] getEndPoint() {
				return new int[] { getWidth() * 15 / 16, getHeight() * 14 / 15 };
			}

			@Override
			public int[] getBeginPoint() {
				return new int[] { getWidth() / 16, getHeight() / 6 };
			}
		};

		return phone;
	}
}
