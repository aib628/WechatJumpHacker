package cc.vmaster.converter;

import java.awt.image.BufferedImage;

public abstract class AbstractConverter implements IConverter {

	protected boolean checkAdjustPoints(BufferedImage image, int[] beginPoint, int[] endPoint) {
		int width = image.getWidth();
		int height = image.getHeight();

		boolean adjustedBegin = checkAdjustBeginPoint(beginPoint);
		boolean adjustedEnd = checkAdjustEndPoint(endPoint, width, height);

		return adjustedBegin || adjustedEnd;
	}

	/**
	 * 检查起始坐标是否合法
	 * 
	 * @param beginPoint 坐标
	 */
	protected boolean checkAdjustBeginPoint(int[] beginPoint) {
		boolean adjusted = false;

		if (beginPoint[0] < 0) {
			beginPoint[0] = 0;
			adjusted = true;
		}

		if (beginPoint[1] < 0) {
			beginPoint[1] = 0;
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
	protected boolean checkAdjustEndPoint(int[] endPoint, int width, int height) {
		boolean adjusted = false;

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

}
