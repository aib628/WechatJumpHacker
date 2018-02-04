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

import cc.vmaster.IPhone;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.finder.helper.MapHelper;
import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.finder.helper.XLineAscComparator;
import cc.vmaster.finder.helper.YLineAscComparator;
import cc.vmaster.helper.CoordinateChecker;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.MathUtils;
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
	private final Pattern PATTERN_POINT = Pattern.compile("^\\(([-+]*[0-9]+),([-+]*[0-9]+)\\)$");

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

				classifyPixel(rgb, new int[] { x, y }, 5);
			}
		}

		Map<Integer, PixelContainer> sortedMap = MapHelper.reorderCountMap(countMap);
		MapHelper.removeUseless(sortedMap, 0, 4);// 按数量移除较少的集合点
		removeImposible(sortedMap, width, endPoint[1]);// 移除一定不可能的点

		if (sortedMap.size() == 0) {
			// 保证只执行一次
			if (position != null && endPoint[1] != position[1]) {
				endPoint[1] = position[1];
				return find(image, beginPoint, endPoint);
			}
		}

		return removeUntilOnlyOne(image, sortedMap, width);// 移除直至只剩一个
	}

	/**
	 * 此处传入的Map经过处理后仅剩一个坐标集合。或者目标块低于瓶子头部时此处Map无元素
	 * 
	 * @param image
	 * @param sortedMap
	 * @param width
	 * @return
	 */
	private int[] calcCenterPoint(BufferedImage image, int width, boolean tips) {
		removeDiscontinuousPoints(image, width);// 移除不连续的点

		Collections.sort(points, XLineAscComparator.instance);
		int[] min = points.get(0);
		int[] max = points.get(points.size() - 1);
		int[] point = new int[] { (min[0] + max[0]) / 2, Math.min(min[1], max[1]) };
		boolean flag = makeupPoint(image, point, image.getHeight(), tips);
		if (!flag) {
			return new int[] { 0, 0 };
		}

		changeBgColor(image, point[1], width);// 将背景色切换为当前行

		return point;
	}

	/**
	 * 移除不太可能的点，比如点最大值与最小值相差太大
	 */
	private void removeImposible(Map<Integer, PixelContainer> sortedMap, int width, int maxY) {
		removeByPosition(sortedMap, width);// 移除位置不可能的部分
		removeByBottle(sortedMap, maxY);// 移除瓶子头部区域
	}

	private int[] removeUntilOnlyOne(BufferedImage image, Map<Integer, PixelContainer> sortedMap, int width) {
		Map<Integer, PixelContainer> map = MapHelper.reorderCountMap(sortedMap);// 重新生成一份备用
		removeTargetBottom(sortedMap);// 移除目标块高度切面部分
		for (PixelContainer pixel : sortedMap.values()) {
			points.addAll(pixel.pointList);// 经过上述移除操作，此时一定只剩唯一点集合
		}

		int[] point = calcCenterPoint(image, width, false);

		boolean matchedBG = false;
		if (point != null && point.length == 2 && !CoordinateChecker.invalidPoint(point)) {
			RGB rgb = RGB.calcRGB(image.getRGB(point[0], point[1]));// 背景色，肯定找的不对
			matchedBG = matched(rgb, RGB_TARGET_BG, 16);
		}

		if (CoordinateChecker.invalidPoint(point) || matchedBG) {
			if (debug()) {
				System.out.println("取消目标块高度切面移除，改用集合点数量移除");
			}

			MapHelper.removeUseless(map, 0, 1);// 按占集合数量移除
			points.clear();// 清除上步错误的点集合
			for (PixelContainer pixel : map.values()) {
				points.addAll(pixel.pointList);// 经过上述移除操作，此时一定只剩唯一点集合
			}

			point = calcCenterPoint(image, width, true);
		}

		return point;
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
			System.out.println("程序未设置当前位置");
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
				int maxX = minInt;
				for (int[] point : points) {
					minX = Math.min(minX, point[0]);
					maxX = Math.max(maxX, point[0]);
				}

				// 如果连最小值都位于屏幕右边，此值定有问题
				if (minX > position[0]) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过瓶子位置移除：右方");
					}
				}

				// 如果通过计算得到的中心位置比当前位置还偏右，则移除
				else if ((maxX + minX) / 2 > position[0]) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过中心位置与瓶子位置相比移除：右方");
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
				int minX = maxInt;
				for (int[] point : points) {
					minX = Math.min(minX, point[0]);
					maxX = Math.max(maxX, point[0]);
				}

				// 如果连最大值都位于屏幕左边，此值定有问题
				if (maxX < position[0]) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过瓶子位置移除：左方");
					}
				}

				// 如果通过计算得到的中心位置比当前位置还偏左，则移除
				else if ((maxX + minX) / 2 < position[0]) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过中心位置与瓶子位置相比移除：左方");
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
		// 由于瓶子当前位置不停变化，此处只有手动传入时才会生效
		if (position == null) {
			System.out.println("程序未设置当前位置");
			return;
		}

		int[] point0 = new int[] { position[0], endY - 40 };// 瓶子头部圆心坐标

		Iterator<Entry<Integer, PixelContainer>> iterator = sortedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			PixelContainer container = iterator.next().getValue();
			for (int[] point : container.pointList) {
				int r = MathUtils.calcDistance(point0, point);
				if (r <= 25) {
					iterator.remove();
					if (debug()) {
						System.out.println("通过瓶子头部移除");
					}

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
	private void removeDiscontinuousPoints(BufferedImage image, int width) {
		removeYLineDiscontinuousPoints();// 按Y轴将不连续的点移除、最上面的点才是正确的点
		removeXLineDiscontinuousPoints(width);// 按X轴将不连续的点移除、此处有风险，保留点数多的集合。并将结果同步回points中
		removeYLineDiscontinuousPoints();// 按Y轴将不连续的点移除、最上面的点才是正确的点
		removeXLineDiscontinuousPoints(image);// 以X轴为中线，将不在该线上连续的点移除，解决药瓶上下大小不一致且颜色相同的问题。
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
	 * 以X轴为中线，将不在该线上连续的点移除，解决药瓶上下大小不一致且颜色相同的问题。
	 */
	private void removeXLineDiscontinuousPoints(BufferedImage image) {
		Collections.sort(points, XLineAscComparator.instance);
		int[] min = points.get(0);
		int[] max = points.get(points.size() - 1);
		int xline = (min[0] + max[0]) / 2;// 得到X中轴线位置，要求在该线上，Y轴连续

		int maxY = minInt;
		Collections.sort(points, YLineAscComparator.instance);
		min = points.get(0);
		max = points.get(points.size() - 1);

		for (int y = min[1]; y <= max[1]; y++) {
			boolean found = false;
			for (int[] point : points) {
				if (point[1] == y && point[0] == xline) {
					found = true;
					maxY = Math.max(maxY, y);
					break;
				}
			}

			if (!found) {
				break;
			}
		}

		// 一个都没找到，取消操作
		if (maxY == minInt) {
			return;
		}

		// 如果导致Y轴跨度小于此值，则取消操作，解决目标块环状图形以及背景在正上方图形
		if (maxY - min[1] < 30) {
			boolean found = false;// 寻找圆环的另一端
			RGB rgb = RGB.calcRGB(image.getRGB(xline, maxY));
			for (int y = maxY + 1; y < max[1]; y++) {
				RGB pointRGB = RGB.calcRGB(image.getRGB(xline, y));
				if (matched(pointRGB, rgb, 5)) {
					found = true;
					maxY = Math.max(maxY, y);
					break;
				}
			}

			if (!found) {
				return;
			} else {
				Iterator<int[]> iterator = points.iterator();
				while (iterator.hasNext()) {
					int[] point = iterator.next();
					if (point[1] < min[1]) {
						iterator.remove();
					}
				}
			}

		}

		Iterator<int[]> iterator = points.iterator();
		while (iterator.hasNext()) {
			int[] point = iterator.next();
			if (point[1] > maxY) {
				iterator.remove();
			}
		}
	}

	/**
	 * 对point进行修正，以精确中心位置：通过当前向上下方向搜索颜色相近坐标，并扩展，以取得最佳Y轴位置,颜色匹配误差不可太大，否则反而会出错(主要适用于纯色目标块)。经过测试中心白点会对该方法造成干扰，不过可以不用处理，
	 * 后续会替换为白点位置
	 * 
	 * @param point 当前坐标
	 */
	private boolean makeupPoint(BufferedImage image, int[] point, int height, boolean tips) {
		int minY = maxInt;
		int maxY = minInt;
		int xline = point[0];
		int yline = point[1];
		RGB rgb = RGB.calcRGB(image.getRGB(xline, yline));
		if (matched(rgb, RGB_TARGET_BG, 5)) {
			if (tips) {
				inputConfirm(image, point, "下一中心点识别遇到问题，请确认输出图片标记是否正确（Y/N）");
			}

			return false;// 人工干预，直接返回
		}

		boolean[] flag = new boolean[] { false, false };
		for (int y = yline; y < height; y++) {
			if (!flag[0]) {
				RGB pointRGB = RGB.calcRGB(image.getRGB(point[0], y));
				if (!matched(pointRGB, rgb, 5)) {
					maxY = Math.max(maxY, y);
					flag[0] = true;
				}
			}

			if (!flag[1] && yline * 2 - y < height) {
				RGB pointRGB = RGB.calcRGB(image.getRGB(point[0], yline * 2 - y));
				if (!matched(pointRGB, rgb, 5)) {
					minY = Math.min(minY, yline * 2 - y);
					flag[1] = true;
				}
			}

			if (flag[0] && flag[1]) {
				break;
			}
		}

		point[1] = (minY + maxY) / 2;

		return true;
	}

	/**
	 * 调整位置
	 * 
	 * @param image
	 * @param point 当前点
	 * @throws IOException
	 */
	private void inputConfirm(BufferedImage image, int[] point, String tips) {
		try {
			markPoint(image, imageFile, point);
			System.out.println(tips);

			String str = reader.readLine();
			if ("Y".equalsIgnoreCase(str)) {
				// 无操作
			} else if ("N".equalsIgnoreCase(str)) {
				inputAdjust(image, point, "请输入调整坐标偏移，如(12,-13)代表X轴向右移12且Y轴向上移13个像素点.");
			} else {
				System.out.println("继续（Y）/辅助调整位置（N）后回车");
				inputConfirm(image, point, tips);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 调整位置
	 * 
	 * @param image
	 * @param point 当前点
	 * @throws IOException
	 */
	private void inputAdjust(BufferedImage image, int[] point, String tips) throws IOException {
		System.out.println(tips);

		String str = reader.readLine();
		Matcher matcher = PATTERN_POINT.matcher(str);
		if (matcher.matches()) {
			StringBuilder sb = new StringBuilder("调整结果:");
			sb.append(String.format("(%s,%s) - >", point[0], point[1]));

			point[0] = point[0] + Integer.parseInt(matcher.group(1));
			point[1] = point[1] + Integer.parseInt(matcher.group(2));

			System.out.println(sb.append(String.format("(%s,%s)", point[0], point[1])));

			markPoint(image, imageFile, point);

			inputConfirm(image, point, "调整已OK继续游戏(Y),再次调整(N)");
		} else {
			inputAdjust(image, point, "坐标格式为：(x,y)");
		}
	}

	/**
	 * 在图片上标记坐标点
	 * 
	 * @param image 源文件
	 * @param imageFile 输出文件
	 * @param point 坐标点
	 * @throws IOException
	 */
	private void markPoint(BufferedImage image, File imageFile, int[] point) throws IOException {
		Graphics graphics = image.getGraphics();
		graphics.setColor(Color.red);
		graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);
		graphics.dispose();

		if (imageFile != null) {
			String fileType = imageFile.getName().substring(imageFile.getName().lastIndexOf('.') + 1);
			String filePath = imageFile.getAbsolutePath().replace("." + fileType, "_adjusted." + fileType);
			File adjustFile = new File(filePath);
			ImageIO.write(image, fileType, adjustFile);
			System.out.println("请查看文件，确认调整结果：" + adjustFile.getAbsolutePath());
		} else {
			System.out.println("程序未设置当前处理文件...");
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

			System.out.println("当前处理文件：" + file.getAbsolutePath());
			NEXT_CENTER.setImageFile(file);
			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());
			IPhone phone = getPhone(image);

			int[] position = My_POSITION.find(image, phone.getBeginPoint(), phone.getEndPoint());
			if (CoordinateChecker.invalidPoint(position)) {
				break;// 未找到当前坐标
			}

			System.out.println(String.format("当前位置坐标：(%s,%s)", position[0], position[1]));
			int[] bottleTop = BOTTLE_TOP.find(image, position, null);
			int skipHeight = position[1] - bottleTop[1];

			NEXT_CENTER.setPosition(position);
			int[] nextCenterEndPoint = new int[] { phone.getEndPoint()[0], position[1] - skipHeight };
			int[] point = NEXT_CENTER.findAndRecord(image, phone.getBeginPoint(), nextCenterEndPoint);
			System.out.println(String.format("下一中心位置：(%s,%s)", point[0], point[1]));
			System.out.println(String.format("匹配耗时(ms)：%s\n", NEXT_CENTER.getMilliCosts()));

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

			NEXT_CENTER.debugWithMultiColor(graphics, NEXT_CENTER.countMap.values());

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
