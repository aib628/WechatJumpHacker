package cc.vmaster;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import cc.vmaster.finder.impl.MyPositionFinder;
import cc.vmaster.finder.impl.NextCenterFinder;
import cc.vmaster.finder.impl.WhiterPointFinder;
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
	private static float JUMP_RATIO = 1.400f;

	/**
	 * 记录上次瓶子位置
	 */
	private static int[] lastPositionPoint = null;

	/**
	 * 理论、预期距离
	 */
	private static int expectdDistance = 0;

	private static final int[] phoneRatio = new int[] { 1080, 1920 };// 手机分辨率1080*1920(可以根据图片自适应，无需修改)
	private static final int[] beginPoint = new int[] { phoneRatio[0] / 16, phoneRatio[1] / 7 };
	private static final int[] endPoint = new int[] { phoneRatio[0] * 15 / 16, phoneRatio[1] * 14 / 15 };

	/**
	 * 随机数发生器
	 */
	private static final Random RANDOM = new Random();

	private static final MyPositionFinder My_POSITION = new MyPositionFinder();
	private static final NextCenterFinder NEXT_CENTER = new NextCenterFinder();
	private static final WhiterPointFinder WHITE_POINT = new WhiterPointFinder();

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			ADB_PATH = args[0];
			if (args.length > 1) {
				imageSavePath = args[1];
			}
		}

		File inputDirectory = initInputDirectory();
		System.out.println("WorkHome: " + inputDirectory.getAbsolutePath());

		int hitCount = 0;
		int executeCount = 1;

		for (;; executeCount++) {
			File imageFile = new File(inputDirectory, imageCount++ + ".png");
			if (imageFile.exists() && !debug) {
				imageFile.deleteOnExit();
			}

			ImageHelper.getScreenShot(ADB_PATH, imageFile);
			BufferedImage image = ImageHelper.loadImage(imageFile.getAbsolutePath());

			int[] positionPoint = My_POSITION.find(image, beginPoint, endPoint);
			if (My_POSITION.invalidPoint(positionPoint)) {
				System.out.println("游戏结束...");
				break;// 未找到当前坐标
			}

			adjustRatio(positionPoint);
			lastPositionPoint = new int[] { positionPoint[0], positionPoint[1] };
			System.out.println(String.format("当前位置坐标：(%s,%s)", positionPoint[0], positionPoint[1]));
			int[] nextCenterEndPoint = new int[] { endPoint[0], positionPoint[1] - 200 };
			int[] nextCenterPoint = NEXT_CENTER.find(image, beginPoint, nextCenterEndPoint);
			if (My_POSITION.invalidPoint(nextCenterPoint)) {
				System.out.println("游戏结束...");
				break;// 未找到下一目标位置坐标
			}

			System.out.println(String.format("目标位置坐标：(%s,%s)", nextCenterPoint[0], nextCenterPoint[1]));

			int[] whitePointBeginPoint = new int[] { nextCenterPoint[0] - 100, nextCenterPoint[1] - 100 };
			int[] whitePointEndPoint = new int[] { nextCenterPoint[0] + 100, nextCenterPoint[1] + 100 };
			int[] whitePoint = WHITE_POINT.find(image, whitePointBeginPoint, whitePointEndPoint);
			if (!My_POSITION.invalidPoint(whitePoint)) {
				System.out.println(String.format("目标中心坐标：(%s,%s)", whitePoint[0], whitePoint[1]));
				hitCount++;
				nextCenterEndPoint = whitePoint;
			}

			int distance = calcDistance(nextCenterPoint, positionPoint);
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
				graphics.fillRect(positionPoint[0] - 5, positionPoint[1] - 5, 10, 10);// 标记位置

				graphics.setColor(Color.red);
				graphics.fillRect(nextCenterPoint[0] - 5, nextCenterPoint[1] - 5, 10, 10);// 标记位置

				if (!My_POSITION.invalidPoint(whitePoint)) {
					graphics.setColor(Color.BLACK);
					graphics.fillRect(whitePoint[0] - 5, whitePoint[1] - 5, 10, 10);// 标记位置
				}

				graphics.dispose();

				ImageIO.write(image, "png", imageFile);
			}

			try {
				// sleep 随机时间，防止上传不了成绩
				Thread.sleep(3_000 + RANDOM.nextInt(3000));// 4_000 +
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			float hitRate = hitCount * 100 / executeCount;
			System.out.println(String.format("共执行：%s次，命中：%s次，命中率：%s%%", executeCount, hitCount, hitRate));
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
		return Double.valueOf(Math.sqrt(a + b) * JUMP_RATIO).intValue();
	}

	private static void adjustRatio(int[] positionPoint) {
		/*
		 * if (expectdDistance > 0 && lastPositionPoint != null) { int actualDistance = calcDistance(lastPositionPoint,
		 * positionPoint); JUMP_RATIO = JUMP_RATIO * expectdDistance / actualDistance;
		 * 
		 * System.out.println("当前弹跳系数：" + JUMP_RATIO); }
		 */
	}

	public static int[] getPhoneRatio() {
		return new int[] { phoneRatio[0], phoneRatio[1] };
	}

	public static int[] getBeginPoint() {
		return new int[] { beginPoint[0], beginPoint[1] };
	}

	public static int[] getEndPoint() {
		return new int[] { endPoint[0], endPoint[1] };
	}
}
