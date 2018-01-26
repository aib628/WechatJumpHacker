package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import cc.vmaster.Phone;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.finder.helper.MapHelper;
import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.finder.helper.XLineAscComparator;
import cc.vmaster.finder.helper.YLineAscComparator;
import cc.vmaster.helper.CoordinateChecker;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.RGB;

/**
 * 
 * 寻找下一目标位置中心点
 * 
 * beginPoint:同瓶子当前位置寻找器beginPoint，如Phone.getBeginPoint();
 * 
 * endPoint:{Phone.getEndPoint()[0], position[1]-skipHight} ， position为瓶子位置坐标 ， skipHight为瓶子高度
 * 
 * @author VMaster
 *
 */
public class NextCenterFinder extends TimeRecodFinder {

	private int[] position;
	private final RGB RGB_TARGET_GAME_OVER = new RGB(51, 46, 44);// 游戏结束RGB色值
	private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	private final Pattern PATTERN_POINT = Pattern.compile("^\\((-*[0-9]+),(-*[0-9]+)\\)$");

	public static NextCenterFinder getInstance() {
		return new NextCenterFinder();
	}

	public void setPosition(int[] position) {
		this.position = position;
	}

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		CoordinateChecker.checkAdjustPoints(image, beginPoint, endPoint);
		clearDebug();

		// 取出背景色，判断是否游戏结束
		RGB overBg = RGB.calcRGB(image.getRGB(image.getWidth() / 2, 5));
		if (matched(overBg, RGB_TARGET_GAME_OVER, 16)) {
			return null;// 有可能是游戏结束了
		}

		int width = image.getWidth();
		for (int y = beginPoint[1]; y < endPoint[1]; y++) {
			changeBgColor(image, y, width);

			for (int x = beginPoint[0]; x < endPoint[0]; x++) {
				RGB rgb = RGB.calcRGB(image.getRGB(x, y));

				if (matched(rgb, RGB_TARGET_BG, 5)) {
					continue;
				}

				classifyPixel(rgb, new int[] { x, y }, 30);
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.reorderCountMap(countMap);
		MapHelper.removeUseless(sortedMap, 0, 4);
		removeImposible(sortedMap, width, endPoint[1]);

		// 经过上述移除操作，此时一定只剩唯点集合
		for (PixelContainer pixel : sortedMap.values()) {
			points.addAll(pixel.pointList);
		}

		removeDiscontinuousPoints(width);// 移除不连续的点
		Collections.sort(points, XLineAscComparator.instance);

		int[] min = points.get(0);
		int[] max = points.get(points.size() - 1);

		int[] point = new int[] { (min[0] + max[0]) / 2, Math.min(min[1], max[1]) };
		makeupPoint(image, point, image.getHeight());

		return point;
	}

	/**
	 * 移除不太可能的点，比如点最大值与最小值相差太大
	 */
	private void removeImposible(Map<Integer, PixelContainer> sortedMap, int width, int maxY) {
		removeByPosition(sortedMap, width);// 移除位置不可能的部分
		removeByBottle(sortedMap, maxY);// 移除瓶子头部区域
		removeTargetBottom(sortedMap);// 可移除目标块高度切面部分
	}

	/**
	 * 通过X轴位置移除不可能集合点。瓶子位于左方，则下一中心点一定位于瓶子右方，反之亦然
	 * 
	 * @param sortedMap 集合点
	 * @param width 屏幕宽度
	 */
	private void removeByPosition(Map<Integer, PixelContainer> sortedMap, int width) {
		// 由于瓶子当前位置不停变化，此处只有手动传入时才会生效
		if (position == null) {
			return;
		}

		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		int spliter = position[0];// 获取瓶子X轴位置,从而确定下一中心点是位置屏幕左方，还是右方

		// 瓶子中心点位于右方、下一中心最小值一定位于瓶子中心点左方
		if (spliter > width / 2) {
			while (iterator.hasNext()) {
				if (sortedMap.size() < 2) {
					return;
				}

				Entry<Integer, PixelContainer> e = iterator.next();
				List<int[]> points = e.getValue().pointList;
				int minX = maxInt;
				for (int[] point : points) {
					minX = Math.min(minX, point[0]);
				}

				// 如果连最小值都位于屏幕右边，此值定有问题
				if (minX > position[0]) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过瓶子位置移除：右方");
					}
				}
			}
		}

		// 瓶子中心点位于左方、下一中心最大值一定位于瓶子中心点右方
		else {
			while (iterator.hasNext()) {
				if (sortedMap.size() < 2) {
					return;
				}

				Entry<Integer, PixelContainer> e = iterator.next();
				List<int[]> points = e.getValue().pointList;
				int maxX = minInt;
				for (int[] point : points) {
					maxX = Math.max(maxX, point[0]);
				}

				// 如果连最大值都位于屏幕左边，此值定有问题
				if (maxX < position[0]) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过瓶子位置移除：左方");
					}
				}
			}
		}
	}

	/**
	 * 移除瓶子头部圆球处所有坐标：瓶子颈部空白高度10px,瓶子圆球直径60px
	 * 
	 * @param sortedMap 集合块
	 * @param endY 由BottleTopFinder计算出的瓶子顶部高度，此处为瓶子颈部坐标
	 */
	private void removeByBottle(Map<Integer, PixelContainer> sortedMap, int endY) {
		int maxY = endY - 10;
		int minY = endY - 70;
		int maxX = position[0] + 30;
		int minX = position[0] - 30;

		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			PixelContainer container = iterator.next().getValue();
			for (int[] point : container.pointList) {
				int x = point[0];
				int y = point[1];
				if (minX < x && x < maxX && minY < y && y < maxY) {
					iterator.remove();
					break;
				}
			}
		}
	}

	/**
	 * 移除目标块高度切面：目标块最低点坐标一定大于其高度最高点，因此只需找出最高点，即可移除影子及高度切面。但要注意目标块以外物体
	 * 
	 * @param sortedMap 集合点
	 */
	private void removeTargetBottom(Map<Integer, PixelContainer> sortedMap) {
		if (sortedMap.size() < 2) {
			return;
		}

		// 获取集合点的最高点，即最小值。0号位置存在最小值，以便排序，1叫位置存放sortedMap的Key，以方便移除
		List<int[]> list = new ArrayList<int[]>();
		for (Entry<Integer, PixelContainer> e : sortedMap.entrySet()) {
			int min = maxInt;
			for (int[] point : e.getValue().pointList) {
				min = Math.min(min, point[1]);
			}

			list.add(new int[] { min, e.getKey() });
		}

		// 按最小值进行升序排序
		Collections.sort(list, XLineAscComparator.instance);

		// 取第一个值便是最高点
		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getKey() != list.get(0)[1]) {
				iterator.remove();
				if (debug()) {
					System.out.println("通过目标块高度切面移除");
				}
			}
		}
	}

	/**
	 * 移除不连续的Y点：从最高处开始，Y点数小的才是目标块点
	 * 
	 * 移除不连续的X点：从最左边开始，将不连续的点进行分组，然后移除不可能组，得到最终点集合
	 */
	private void removeDiscontinuousPoints(int width) {
		removeYLineDiscontinuousPoints();
		removeXLineDiscontinuousPoints(width);
		removeYLineDiscontinuousPoints();
	}

	/**
	 * 按Y轴将不连续的点移除、最上面的点才是正确的点
	 */
	private void removeYLineDiscontinuousPoints() {
		int maxY = minInt;
		Collections.sort(points, YLineAscComparator.instance);
		Iterator<int[]> iterator = points.iterator();
		while (iterator.hasNext()) {
			int[] point = iterator.next();
			if (maxY == minInt || point[1] - maxY <= 1) {
				maxY = Math.max(maxY, point[1]);
			} else {
				iterator.remove();
				maxY = Math.max(maxY, point[1]);
			}
		}
	}

	/**
	 * 按X轴将不连续的点移除、此处有风险，保留点数多的集合。并将结果同步回points中
	 */
	private void removeXLineDiscontinuousPoints(int width) {
		int maxX = minInt;
		Map<Integer, PixelContainer> map = new HashMap<Integer, PixelContainer>();
		Collections.sort(points, XLineAscComparator.instance);
		Iterator<int[]> iterator = points.iterator();
		while (iterator.hasNext()) {
			int[] point = iterator.next();
			if (maxX == minInt || point[0] - maxX <= 1) {
				maxX = Math.max(maxX, point[0]);
				if (map.size() == 0) {
					map.put(1, new PixelContainer(point));
				} else {
					map.get(map.size()).addCount(point);
				}
			} else {
				iterator.remove();
				maxX = Math.max(maxX, point[0]);
				map.put(map.size() + 1, new PixelContainer(point));
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.reorderCountMap(map);
		removeByPosition(sortedMap, width);
		MapHelper.removeUseless(sortedMap, 0, 1);

		points.clear();
		for (PixelContainer pixel : sortedMap.values()) {
			points.addAll(pixel.pointList);
		}
	}

	/**
	 * 对point进行修正，以精确中心位置
	 * 
	 * @param point 当前坐标
	 */
	private void makeupPoint(BufferedImage image, int[] point, int height) {
		int minY = maxInt;
		int maxY = minInt;
		int xline = point[0];
		int yline = point[1];
		RGB rgb = RGB.calcRGB(image.getRGB(xline, yline));
		if (matched(rgb, RGB_TARGET_BG, 16)) {
			System.out.println("下一中心点识别遇到问题，请确认输出图片标记是否正确（Y/N）");
			try {
				inputConfirm(image, point);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		boolean[] flag = new boolean[] { false, false };
		for (int y = yline; y < height; y++) {
			if (!flag[0]) {
				RGB pointRGB = RGB.calcRGB(image.getRGB(point[0], y));
				if (!matched(pointRGB, rgb, 10)) {
					maxY = Math.max(maxY, y);
					flag[0] = true;
				}
			}

			if (!flag[1] && yline * 2 - y < height) {
				RGB pointRGB = RGB.calcRGB(image.getRGB(point[0], yline * 2 - y));
				if (!matched(pointRGB, rgb, 10)) {
					minY = Math.min(minY, yline * 2 - y);
					flag[1] = true;
				}
			}

			if (flag[0] && flag[1]) {
				break;
			}
		}

		point[1] = (minY + maxY) / 2;
	}

	/**
	 * 调整位置
	 * 
	 * @param image
	 * @param point 当前点
	 * @throws IOException
	 */
	private void inputConfirm(BufferedImage image, int[] point) throws IOException {
		String str = reader.readLine();
		if ("Y".equalsIgnoreCase(str)) {
			// 无操作
		} else if ("N".equalsIgnoreCase(str)) {
			System.out.println("请输入调整坐标偏移，如(12,-13)代表X轴向右移12且Y轴向上移13个像素点.");
			inputAdjust(image, point);
		} else {
			System.out.println("继续（Y）/辅助调整位置（N）后回车");
			inputConfirm(image, point);
		}
	}

	/**
	 * 调整位置
	 * 
	 * @param image
	 * @param point 当前点
	 * @throws IOException
	 */
	private void inputAdjust(BufferedImage image, int[] point) throws IOException {
		String str = reader.readLine();
		Matcher matcher = PATTERN_POINT.matcher(str);
		if (matcher.matches()) {
			StringBuilder sb = new StringBuilder("调整结果:");
			sb.append(String.format("(%s,%s) - >", point[0], point[1]));

			point[0] = point[0] + Integer.parseInt(matcher.group(1));
			point[1] = point[1] + Integer.parseInt(matcher.group(2));

			Graphics graphics = image.getGraphics();
			graphics.setColor(Color.white);
			graphics.fillRect(position[0] - 5, position[1] - 5, 10, 10);
			graphics.dispose();

			if (imageFile != null) {
				String fileType = imageFile.getName().substring(imageFile.getName().lastIndexOf('.') + 1);
				ImageIO.write(image, fileType, imageFile);
			}

			System.out.println(sb.append(String.format("(%s,%s)", point[0], point[1])));
			System.out.println("继续游戏(Y),再次调整(N)");
			inputConfirm(image, point);
		} else {
			System.out.println("坐标格式为：(x,y)");
			inputAdjust(image, point);
		}
	}

	public static void main(String[] args) throws IOException {
		MyPositionFinder My_POSITION = MyPositionFinder.getInstance();
		BottleTopFinder BOTTLE_TOP = BottleTopFinder.getInstance();
		NextCenterFinder NEXT_CENTER = NextCenterFinder.getInstance();
		NEXT_CENTER.debug(true);// 开启Debug

		URL url = IOUtils.getURL(NEXT_CENTER.getClass(), "classpath:imgs");
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
			int[] bottleTop = BOTTLE_TOP.find(image, position, null);
			int skipHeight = position[1] - bottleTop[1];

			NEXT_CENTER.setPosition(position);
			int[] nextCenterEndPoint = new int[] { Phone.getEndPoint()[0], position[1] - skipHeight };
			int[] point = NEXT_CENTER.findAndRecord(image, Phone.getBeginPoint(), nextCenterEndPoint);
			System.out.println(String.format("下一中心位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s", NEXT_CENTER.getMilliCosts()));

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

			// NEXT_CENTER.debugWithMultiColor(graphics, NEXT_CENTER.countMap.values());
			graphics.setColor(Color.BLACK);
			NEXT_CENTER.debug(graphics, NEXT_CENTER.points);

			graphics.setColor(Color.red);
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);// 标记位置
			graphics.dispose();

			ImageIO.write(bufferedImage, "png", descFile);

			costs += recoder.end();
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));
	}

}
