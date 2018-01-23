package cc.vmaster.helper;

import java.awt.image.BufferedImage;

public class CoordinateChecker {

	public static boolean checkAdjustPoints(BufferedImage image, int[] beginPoint, int[] endPoint) {
		int width = image.getWidth();
		int height = image.getHeight();

		boolean adjustedBegin = checkAdjustBeginPoint(beginPoint, width, height);
		boolean adjustedEnd = checkAdjustEndPoint(endPoint, width, height);

		return adjustedBegin || adjustedEnd;
	}

	/**
	 * 检查起始坐标是否合法
	 * 
	 * @param beginPoint 坐标
	 */
	public static boolean checkAdjustBeginPoint(int[] beginPoint, int width, int height) {
		boolean adjusted = false;

		if (beginPoint[0] < 0) {
			beginPoint[0] = 0;
			adjusted = true;
		}

		if (beginPoint[1] < 0) {
			beginPoint[1] = 0;
			adjusted = true;
		}

		if (beginPoint[0] > width) {
			beginPoint[0] = width;
			adjusted = true;
		}

		if (beginPoint[1] > height) {
			beginPoint[1] = height;
			adjusted = true;
		}

		return adjusted;
	}

	/**
	 * 检查结束坐标是否合法
	 * 
	 * @param endPoint 结束坐标
	 * @param width 屏宽
	 * @param height 屏高
	 */
	public static boolean checkAdjustEndPoint(int[] endPoint, int width, int height) {
		boolean adjusted = false;

		if (endPoint[0] < 0) {
			endPoint[0] = 0;
			adjusted = true;
		}

		if (endPoint[1] < 0) {
			endPoint[1] = 0;
			adjusted = true;
		}

		if (endPoint[0] > width) {
			endPoint[0] = width;
			adjusted = true;
		}

		if (endPoint[1] > height) {
			endPoint[1] = height;
			adjusted = true;
		}

		return adjusted;
	}

	/**
	 * 是否无效点
	 * 
	 * @param point 点
	 * @param ratio 分辨率
	 */
	public static boolean invalidPoint(int[] point, int... ratio) {
		if (point == null || (point[0] == 0 && point[1] == 0)) {
			return true;
		}

		if (ratio.length == 2) {
			if (point[0] >= ratio[0] || point[1] >= ratio[1]) {
				return true;
			}
		}

		return false;
	}

}
