package cc.vmaster.finder;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;

import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.helper.RGB;

public abstract class AbstractFinder implements IFinder {

	protected final Collection<PixelContainer> pixels = new ArrayList<PixelContainer>();
	protected final Collection<int[]> points = new ArrayList<int[]>();
	private boolean debug = false;// 默认不启动，以加速

	/**
	 * 清除Debug数据，以方便下一次Debug
	 */
	protected void clearDebug() {
		pixels.clear();
		points.clear();
	}

	/**
	 * 获取Debug开关
	 */
	protected boolean debug() {
		return debug;
	}

	/**
	 * 开启Debug
	 */
	protected void debug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * 将给定坐标点标记颜色
	 * 
	 * @param image 源图片
	 * @param pixels
	 */
	protected void debug(Graphics graphics, Collection<int[]> points) {
		for (int[] point : points) {
			graphics.fillRect(point[0], point[1], 1, 1);// 标记位置
		}
	}

	/**
	 * 计算在指定误差内，是否匹配目标RGB
	 * 
	 * @param r R值
	 * @param g G值
	 * @param b B值
	 * @param tolerance 允许误差
	 * @return 匹配结果
	 */
	protected boolean matched(RGB rgb, RGB target, int tolerance) {
		int R_TARGET = target.R;
		int G_TARGET = target.G;
		int B_TARGET = target.B;

		if (rgb.R > R_TARGET - tolerance && rgb.R < R_TARGET + tolerance) {
			if (rgb.G > G_TARGET - tolerance && rgb.G < G_TARGET + tolerance) {
				if (rgb.B > B_TARGET - tolerance && rgb.B < B_TARGET + tolerance) {
					return true;
				}
			}
		}

		return false;
	}

}
