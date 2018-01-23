package cc.vmaster.converter.impl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import cc.vmaster.converter.AbstractConverter;
import cc.vmaster.converter.IConverter;
import cc.vmaster.helper.CoordinateChecker;
import cc.vmaster.helper.IOUtils;
import cc.vmaster.helper.ImageHelper;
import cc.vmaster.helper.RGB;

public class RedChannelConverter extends AbstractConverter {

	@Override
	public BufferedImage convert(BufferedImage image, int[] beginPoint, int[] endPoint) {
		CoordinateChecker.checkAdjustPoints(image, beginPoint, endPoint);

		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();

		for (int x = beginPoint[0]; x < endPoint[0]; x++) {
			for (int y = beginPoint[1]; y < endPoint[1]; y++) {
				int pixel = image.getRGB(x, y);
				RGB rgb = RGB.calcRGB(pixel);
				graphics.setColor(new Color(rgb.R, 0, 0));
				graphics.fillRect(x, y, 1, 1);
			}
		}

		graphics.dispose();

		return bufferedImage;
	}

	public static void main(String[] args) throws IOException {
		IConverter converter = new RedChannelConverter();

		URL url = IOUtils.getURL(IConverter.class, "classpath:imgs");
		System.out.println("WorkHome:" + url.getFile());

		File[] files = new File(url.getFile()).listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				continue;
			}

			File descFile = new File(url.getPath() + "/found", file.getName());
			if (!descFile.exists()) {
				descFile.mkdirs();
				descFile.createNewFile();
			}

			BufferedImage image = ImageHelper.loadImage(file.getAbsolutePath());

			int[] beginPoint = new int[] { 0, 0 };
			int[] endPoint = new int[] { image.getWidth(), image.getHeight() };
			BufferedImage bufferedImage = converter.convert(image, beginPoint, endPoint);

			ImageIO.write(bufferedImage, "png", descFile);

			System.out.println("Done ...");
		}
	}

}
