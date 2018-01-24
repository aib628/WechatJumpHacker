package cc.vmaster.finder.impl;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Queue;

import cc.vmaster.finder.TimeRecodFinder;

/**
 * 寻找一下目标位置中的白色中心点,如果有
 * 
 * beginPoint:由NextCenterFinder寻找到的下一目标位置坐标
 * 
 * endPoint:无效
 * 
 * @author VMaster
 *
 */
public class WhiterPointFinder extends TimeRecodFinder {

	private final int TARGET = 245;

	public static WhiterPointFinder getInstance() {
		return new WhiterPointFinder();
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		clearDebug();

		int width = image.getWidth();
		int height = image.getHeight();

		int x1 = Math.max(beginPoint[0], 0);
		int x2 = Math.min(endPoint[0], width - 1);
		int y1 = Math.max(beginPoint[1], 0);
		int y2 = Math.min(endPoint[1], height - 1);

		for (int i = x1; i <= x2; i++) {
			for (int j = y1; j <= y2; j++) {
				int pixel = image.getRGB(i, j);
				int r = (pixel & 0xff0000) >> 16;
				int g = (pixel & 0xff00) >> 8;
				int b = (pixel & 0xff);
				if (r == TARGET && g == TARGET && b == TARGET) {
					boolean[][] vMap = new boolean[width][height];
					Queue<int[]> queue = new ArrayDeque<>();
					int[] pos = { i, j };
					queue.add(pos);
					int maxX = Integer.MIN_VALUE;
					int minX = Integer.MAX_VALUE;
					int maxY = Integer.MIN_VALUE;
					int minY = Integer.MAX_VALUE;
					while (!queue.isEmpty()) {
						pos = queue.poll();
						int x = pos[0];
						int y = pos[1];
						if (x < x1 || x > x2 || y < y1 || y > y2 || vMap[x][y]) {
							continue;
						}
						vMap[x][y] = true;
						pixel = image.getRGB(x, y);
						r = (pixel & 0xff0000) >> 16;
						g = (pixel & 0xff00) >> 8;
						b = (pixel & 0xff);
						if (r == TARGET && g == TARGET && b == TARGET) {
							maxX = Math.max(maxX, x);
							minX = Math.min(minX, x);
							maxY = Math.max(maxY, y);
							minY = Math.min(minY, y);
							queue.add(buildArray(x - 1, y));
							queue.add(buildArray(x + 1, y));
							queue.add(buildArray(x, y - 1));
							queue.add(buildArray(x, y + 1));
						}
					}

					// System.out.println("whitePoint: " + maxX + ", " + minX + ", " + maxY + ", " + minY);
					if (maxX - minX <= 45 && maxX - minX >= 35 && maxY - minY <= 30 && maxY - minY >= 20) {
						int[] ret = { (minX + maxX) / 2, (minY + maxY) / 2 };
						return ret;
					} else {
						return null;
					}

				}
			}
		}
		return null;
	}

	public static int[] buildArray(int i, int j) {
		int[] ret = { i, j };
		return ret;
	}

}
