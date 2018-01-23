package cc.vmaster.finder;

import java.awt.Graphics;
import java.util.Collection;

import cc.vmaster.Hacker;
import cc.vmaster.helper.RGB;

public abstract class AbstractFinder implements IFinder {

	private boolean debug = false;// 默认不启动，以加速

	protected void adaptRadio(int[] startPoint, int[] endPoint, int width, int height) {
		int[] phoneRatio = Hacker.getPhoneRatio();

		startPoint[0] = startPoint[0] * width / phoneRatio[0];
		startPoint[1] = startPoint[1] * height / phoneRatio[1];
		endPoint[0] = endPoint[0] * width / phoneRatio[0];
		endPoint[1] = endPoint[1] * height / phoneRatio[1];
	}

	/**
	 * 按手机分辨率比例获取绽放后的值
	 * 
	 * @param num 缩放前
	 * @param width 照片真实宽度
	 * @return 缩放后
	 */
	protected int getScaleWidth(int num, int width) {
		return num * width / Hacker.getPhoneRatio()[0];
	}

	/**
	 * 按手机分辨率比例获取绽放后的值
	 * 
	 * @param num 缩放前
	 * @param height 照片真实高度
	 * @return 缩放后
	 */
	protected int getScaleHeight(int num, int height) {
		return num * height / Hacker.getPhoneRatio()[1];
	}

	/**
	 * 清除Debug数据，以方便下一次Debug
	 */
	protected void clearDebug() {

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
	 * 是否无效点
	 * 
	 * @param point 点
	 * @param ratio 分辨率
	 */
	public boolean invalidPoint(int[] point, int... ratio) {
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
