package cc.vmaster.finder.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.imageio.ImageIO;

import cc.vmaster.Hacker;
import cc.vmaster.finder.TimeRecodFinder;
import cc.vmaster.finder.helper.PixelContainer;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;

public class NextCenterFinderByBgColor extends TimeRecodFinder {

	private Collection<PixelContainer> pixels;
	private RGB RGB_TARGET_BG = new RGB(255, 210, 210);// 背景RGB色值
	private RGB RGB_TARGET_SHADOW = new RGB(178, 149, 148);// 影子RGB色值
	private RGB RGB_TARGET_SHADOW_2 = new RGB(178, 149, 100);// 影子RGB色值
	private RGB RGB_TARGET_GAME_OVER = new RGB(51, 46, 44);// 游戏结束RGB色值

	@Override
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) throws IOException {
		NextCenterFinderByBgColor NEXT_CENTER_BY_BG = new NextCenterFinderByBgColor();
		NEXT_CENTER_BY_BG.debug(true);// 开启Debug

		URL url = IOUtils.getURL(NEXT_CENTER_BY_BG.getClass(), "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		long costs = 0;
		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (!file.getName().endsWith(".png")) {
				continue;
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());

			int[] point = NEXT_CENTER_BY_BG.findAndRecord(image, Hacker.getBeginPoint(), Hacker.getEndPoint());

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

			int i = 0;
			Color[] colors = new Color[] { Color.blue, Color.green, Color.orange };
			for (PixelContainer pixel : NEXT_CENTER_BY_BG.pixels) {
				i++;
				graphics.setColor(colors[i % 3]);
				NEXT_CENTER_BY_BG.debug(graphics, pixel.pointList);
			}

			graphics.setColor(Color.red);
			graphics.fillRect(point[0] - 5, point[1] - 5, 10, 10);// 标记位置
			graphics.dispose();

			ImageIO.write(bufferedImage, "png", descFile);

			costs += recoder.end();
		}

		System.out.println("average time cost(ms): " + (costs / files.length / 1_000_000));
	}

}
