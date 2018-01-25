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
 * beginPoint：瓶子当前位置坐标
 * 
 * endPoint：无效
 * 
 * @author FanRenwei
 *
 */
public class BottleRangeCenterFinder extends TimeRecodFinder {

	public static BottleRangeCenterFinder getInstance() {
		return new BottleRangeCenterFinder();
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		clearDebug();
		int width = image.getWidth();
		int height = image.getHeight();
		CoordinateChecker.checkAdjustBeginPoint(beginPoint, width, height);

		int xline = beginPoint[0];// 瓶子X轴位置
		int yline = beginPoint[1];// 瓶子Y轴位置
		boolean[] flag = new boolean[] { false, false };
		for (int y = yline; y >= 0; y--) {
			if (!flag[0]) {
				if (YLine(image, xline, y, width)) {
					flag[0] = true;
				}
			}

			if (!flag[1] && yline * 2 - y < height) {
				if (YLine(image, xline, yline * 2 - y, width)) {
					flag[1] = true;
				}
			}

			if (flag[0] && flag[1]) {
				break;
			}
		}

		return new int[] { 0, 0 };
	}

	/**
	 * Y及Y对称点处理，均遇背景色停止
	 * 
	 * @param image
	 * @param xline X中轴线
	 * @param y 当前Y
	 * @param width 屏幕宽度
	 * @return 是否停止
	 */
	private boolean YLine(BufferedImage image, int xline, int y, int width) {
		changeBgColor(image, y, width);

		// 中轴线颜色
		RGB rgb = RGB.calcRGB(image.getRGB(xline, y));
		if (matched(rgb, RGB_TARGET_BG, 16)) {
			return true;
		}

		// 记录沿X轴左边及右边停止状态
		boolean[] flag = new boolean[] { false, false };
		for (int x = xline; x >= 0; x--) {
			if (XLine(image, flag, xline, x, y, width)) {
				break;
			}
		}

		return false;
	}

	/**
	 * X及X对称点处理，均遇背景色停止
	 * 
	 * @param image
	 * @param flag 记录沿X轴左边及右边停止状态，若左边为False则停止左边计算，反之亦然，右同
	 * @param xline X中轴线
	 * @param x 当前X
	 * @param y 当前Y
	 * @param width 图片宽度
	 * @return 是否停止
	 */
	private boolean XLine(BufferedImage image, boolean[] flag, int xline, int x, int y, int width) {
		boolean leftBreak = false;
		if (flag[0]) {
			leftBreak = true;
		} else {
			RGB leftPointRGB = RGB.calcRGB(image.getRGB(x, y));
			if (!matched(RGB_TARGET_BG, leftPointRGB, 16)) {
				classifyPixel(leftPointRGB, new int[] { x, y }, 16);
			} else {
				flag[0] = true;
				leftBreak = true;
			}
		}

		// X对称点
		x = xline * 2 - x;
		boolean rigthBreak = false;
		if (flag[1]) {
			rigthBreak = true;
		} else {
			if (x < width) {
				RGB rigthPointRGB = RGB.calcRGB(image.getRGB(x, y));
				if (!matched(RGB_TARGET_BG, rigthPointRGB, 16)) {
					classifyPixel(rigthPointRGB, new int[] { x, y }, 16);
				} else {
					flag[1] = true;
					rigthBreak = true;
				}
			}
		}

		return rigthBreak && leftBreak;
	}

	public static void main(String[] args) throws IOException {
		MyPositionFinder My_POSITION = MyPositionFinder.getInstance();
		BottleRangeCenterFinder BOTTLE_RANGE_CENTER = BottleRangeCenterFinder.getInstance();
		BOTTLE_RANGE_CENTER.debug(true);// 开启Debug

		URL url = IOUtils.getURL(BOTTLE_RANGE_CENTER.getClass(), "classpath:imgs");
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

			int[] point = BOTTLE_RANGE_CENTER.findAndRecord(image, position, null);
			System.out.println(String.format("下一中心位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s", BOTTLE_RANGE_CENTER.getMilliCosts()));

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

			BOTTLE_RANGE_CENTER.debugWithMultiColor(graphics, BOTTLE_RANGE_CENTER.countMap.values());

			graphics.setColor(Color.red);
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);// 标记位置
			graphics.dispose();

			ImageIO.write(bufferedImage, "png", descFile);

			costs += recoder.end();
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));
	}

}
