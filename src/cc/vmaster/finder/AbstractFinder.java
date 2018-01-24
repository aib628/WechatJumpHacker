package cc.vmaster.finder;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.helper.RGB;

public abstract class AbstractFinder implements IFinder {

	protected final Map<Integer, PixelContainer> countMap = new HashMap<Integer, PixelContainer>();
	protected final List<PixelContainer> pixels = new ArrayList<PixelContainer>();
	protected final List<int[]> points = new ArrayList<int[]>();
	protected RGB RGB_TARGET_BG = new RGB(255, 210, 210);// 默认背景色
	protected RGB RGB_TARGET_BOTTLE = new RGB(40, 43, 86);// 默认瓶子RGB色值
	private boolean debug = false;// 默认不启动，以加速

	/**
	 * 清除Debug数据，以方便下一次Debug
	 */
	protected void clearDebug() {
		countMap.clear();
		pixels.clear();
		points.clear();

		// 将默认背景重置，以方便下一次切换背景
		RGB_TARGET_BG.pixel(0);
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
	 * @param graphics
	 * @param points 坐标集合
	 */
	protected void debug(Graphics graphics, Collection<int[]> points) {
		for (int[] point : points) {
			graphics.fillRect(point[0], point[1], 1, 1);// 标记位置
		}
	}

	/**
	 * 以设定的多种颜色区分各块显示
	 * 
	 * @param graphics
	 * @param pixels 容器集合
	 */
	protected void debugWithMultiColor(Graphics graphics, Collection<PixelContainer> pixels) {
		int i = 0;
		Color[] colors = new Color[] { Color.blue, Color.green, Color.orange };
		for (PixelContainer pixel : pixels) {
			i++;
			graphics.setColor(colors[i % 3]);
			debug(graphics, pixel.pointList);
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

	/**
	 * 按颜色相似度分类
	 * 
	 * @param rgb 当前点RGB
	 * @param point 当前点坐标
	 * @param tolerance 容差
	 */
	protected void classifyPixel(RGB rgb, int[] point, int tolerance) {
		if (countMap.size() == 0) {
			countMap.put(rgb.pixel, new PixelContainer(point));
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
			countMap.put(rgb.pixel, new PixelContainer(point));
		}
	}

	/**
	 * 获取最小点坐标Y轴值
	 * 
	 * @param points 坐标集合
	 */
	protected int getMinPoint(Collection<int[]> points) {
		int min = maxInt;
		for (int[] point : points) {
			min = Math.min(min, point[1]);
		}

		return min;
	}
}
