package cc.vmaster;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import cc.vmaster.finder.impl.BottleTopFinder;
import cc.vmaster.finder.impl.MyPositionFinder;
import cc.vmaster.finder.impl.NextCenterFinder;
import cc.vmaster.finder.impl.WhiterPointFinder;
import cc.vmaster.helper.CoordinateChecker;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.shell.CommandHelper;

/**
 * 破解器入口("adb shell wm size")
 * 
 * @author VMaster
 *
 */
public class Hacker {

	private static boolean debug = true;

	private static volatile boolean autoMode = false;

	/**
	 * 图片数量
	 */
	private static int imageCount;

	/**
	 * 设置图片保存路径
	 */
	private static String imageSavePath;

	/**
	 * ADB路径
	 */
	private static String ADB_PATH = "adb";

	/**
	 * 弹跳系数。获取屏幕分辨率
	 */
	private static volatile float JUMP_RATIO = 1.500f;

	/**
	 * 记录上次瓶子位置
	 */
	private static int[] lastPositionPoint = null;

	/**
	 * 理论、预期距离
	 */
	private static int expectdDistance = 0;

	/**
	 * 随机数发生器
	 */
	private static final Random RANDOM = new Random();
	private static final Pattern PATTERN_POINT = Pattern.compile("^\\(([-+]*[0-9]+),([-+]*[0-9]+)\\)$");
	private static final MyPositionFinder My_POSITION = MyPositionFinder.getInstance();
	private static final NextCenterFinder NEXT_CENTER = NextCenterFinder.getInstance();
	private static final WhiterPointFinder WHITE_POINT = WhiterPointFinder.getInstance();
	private static final BottleTopFinder BOTTLE_TOP = BottleTopFinder.getInstance();
	private static IPhone phone = new Phone();// 默认1080*1920 分辨率

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			imageSavePath = args[0];
			if (args.length > 1) {
				ADB_PATH = args[1];
			}
		}

		URL url = IOUtils.getURL(imageSavePath + "/config.properties");
		if (url != null) {
			Properties properties = new Properties();
			properties.load(url.openStream());
			String configSwitch = properties.getProperty("switch");
			if ("true".equalsIgnoreCase(configSwitch)) {
				System.out.println("使用自定义配置.........");
				final String width = properties.getProperty("width");
				final String height = properties.getProperty("height");
				final String beginPoint = properties.getProperty("beginPoint");
				final String endPoint = properties.getProperty("endPoint");
				final String jumpRatio = properties.getProperty("jumpRatio");

				final Matcher beginMatcher = PATTERN_POINT.matcher(beginPoint);
				final Matcher endMatcher = PATTERN_POINT.matcher(endPoint);

				try {
					JUMP_RATIO = Float.parseFloat(jumpRatio);
					if (beginMatcher.matches() && endMatcher.matches()) {
						phone = new IPhone() {

							@Override
							public int getWidth() {
								return Integer.parseInt(width);
							}

							@Override
							public int getHeight() {
								return Integer.parseInt(height);
							}

							@Override
							public int[] getEndPoint() {
								return new int[] { Integer.parseInt(endMatcher.group(1)),
										Integer.parseInt(endMatcher.group(2)) };
							}

							@Override
							public int[] getBeginPoint() {
								return new int[] { Integer.parseInt(beginMatcher.group(1)),
										Integer.parseInt(beginMatcher.group(2)) };
							}
						};
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File inputDirectory = initInputDirectory();
		System.out.println("WorkHome: " + inputDirectory.getAbsolutePath());
		final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		int hitCount = 0;
		int executeCount = 1;
		for (;; executeCount++) {
			File imageFile = new File(inputDirectory, imageCount++ + ".png");
			NEXT_CENTER.setImageFile(imageFile);
			if (imageFile.exists() && !debug) {
				imageFile.deleteOnExit();
			}

			if (!autoMode) {
				listenInput(reader);
			}

			if (!ImageHelper.getScreenShot(ADB_PATH, imageFile)) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(imageFile.getAbsolutePath());

			int[] position = My_POSITION.find(image, phone.getBeginPoint(), phone.getEndPoint());
			if (CoordinateChecker.invalidPoint(position)) {
				System.out.println("游戏结束...");
				break;// 未找到当前坐标
			}

			adjustRatio(position);
			lastPositionPoint = position;
			System.out.println(String.format("当前位置坐标：(%s,%s)", position[0], position[1]));

			int[] bottleTop = BOTTLE_TOP.find(image, position, null);
			int skipHeight = position[1] - bottleTop[1];

			NEXT_CENTER.setPosition(position);
			int[] nextCenterEndPoint = new int[] { phone.getEndPoint()[0], position[1] - skipHeight };
			int[] nextCenter = NEXT_CENTER.find(image, phone.getBeginPoint(), nextCenterEndPoint);
			if (CoordinateChecker.invalidPoint(nextCenter)) {
				System.out.println("游戏结束...");
				break;// 未找到下一目标位置坐标
			}

			System.out.println(String.format("目标位置坐标：(%s,%s)", nextCenter[0], nextCenter[1]));

			int[] whiteBeginPoint = new int[] { nextCenter[0] - 100, nextCenter[1] - 100 };
			int[] whiteEndPoint = new int[] { nextCenter[0] + 100, nextCenter[1] + 100 };
			int[] whitePoint = WHITE_POINT.find(image, whiteBeginPoint, whiteEndPoint);
			if (!CoordinateChecker.invalidPoint(whitePoint)) {
				hitCount++;
				nextCenter = whitePoint;
				System.out.println(String.format("目标中心坐标：(%s,%s)", whitePoint[0], whitePoint[1]));
			}

			int distance = calcDistance(nextCenter, position);
			expectdDistance = distance;
			System.out.println("distance: " + distance);

			int pressX = 400 + RANDOM.nextInt(100);
			int pressY = 500 + RANDOM.nextInt(100);

			String command = ADB_PATH + " shell input swipe %d %d %d %d %d";
			command = String.format(command, pressX, pressY, pressX, pressY, distance);
			CommandHelper.executeCommand(command);

			if (debug) {
				Graphics graphics = image.getGraphics();
				graphics.setColor(Color.white);
				graphics.fillRect(position[0] - 5, position[1] - 5, 10, 10);// 标记位置

				graphics.setColor(Color.red);
				graphics.fillRect(nextCenter[0] - 5, nextCenter[1] - 5, 10, 10);// 标记位置

				if (!CoordinateChecker.invalidPoint(whitePoint)) {
					graphics.setColor(Color.BLACK);
					graphics.fillRect(whitePoint[0] - 5, whitePoint[1] - 5, 10, 10);// 标记位置
				}

				graphics.dispose();

				ImageIO.write(image, "png", imageFile);
			}

			try {
				if (autoMode) {
					if (reader.ready()) {
						listenInput(reader);
					}

					Thread.sleep(3_000 + RANDOM.nextInt(3000));
				} else {
					Thread.sleep(distance);// 防止按快，导致截图有问题而结束
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			float hitRate = hitCount * 100 / executeCount;
			System.out.println(String.format("共执行：%s次，命中：%s次，命中率：%s%%", executeCount, hitCount, hitRate));
			if (autoMode) {
				System.out.println();
			}
		}
	}

	/**
	 * 初始化工作目录
	 * 
	 * @return 工作目录文件对象
	 */
	private static File initInputDirectory() {
		String workHome = Hacker.class.getResource("/").getPath();
		if (imageSavePath != null && imageSavePath.length() > 0) {
			workHome = imageSavePath;
		}

		File inputDirectory = new File(workHome, "imgs/input");
		inputDirectory.mkdirs();

		return inputDirectory;
	}

	/**
	 * 计算两点间距离
	 */
	private static int calcDistance(int[] beginPoint, int[] endPoint) {
		double a = Math.pow(endPoint[0] - beginPoint[0], 2);
		double b = Math.pow(endPoint[1] - beginPoint[1], 2);
		return new Long(Math.round(Math.sqrt(a + b) * JUMP_RATIO)).intValue();
	}

	/**
	 * 监听输入
	 * 
	 * @param reader
	 * @throws IOException
	 */
	private static void listenInput(BufferedReader reader) throws IOException {
		String tips = "输入选项【mode：切换为%s|数字：更改弹跳系数|直接回车：手动模式下继续|其它：忽略】按下Enter继续...\n";
		if (autoMode) {
			System.out.println(String.format(tips, "手动模式"));
		} else {
			System.out.println(String.format(tips, "自动模式"));
		}

		String mode = reader.readLine();
		if ("mode".equalsIgnoreCase(mode)) {
			autoMode = !autoMode;
		} else if (mode != null && mode.length() > 0) {
			try {
				JUMP_RATIO = Float.parseFloat(mode);
			} catch (Exception e) {
			}
		}
	}

	private static void adjustRatio(int[] positionPoint) {

		if (expectdDistance > 0 && lastPositionPoint != null) {
			// int actualDistance = calcDistance(lastPositionPoint, positionPoint);
			// JUMP_RATIO = JUMP_RATIO * expectdDistance / actualDistance;

			System.out.println("当前弹跳系数：" + JUMP_RATIO);
		}

	}

}
