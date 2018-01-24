package cc.vmaster.helper;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import cc.vmaster.helper.shell.CommandHelper;
import cc.vmaster.helper.shell.ExecuteResult;

/**
 * 图片操作工具
 * 
 * @author VMaster
 */
public class ImageHelper {

	/**
	 * 加载图片
	 * 
	 * @param imagePath Image路径
	 * @return Image对象
	 * @throws IOException
	 */
	public static BufferedImage loadImage(String imagePath) throws IOException {
		InputStream inputs = IOUtils.getInputStream(imagePath);
		return loadImage(inputs);
	}

	/**
	 * 加载图片
	 * 
	 * @param imagePath Image路径
	 * @return Image对象
	 * @throws IOException
	 */
	public static BufferedImage loadImage(InputStream inputs) throws IOException {
		if (inputs == null) {
			return null;
		}

		try {
			BufferedInputStream bufferedInputs = new BufferedInputStream(inputs);
			return ImageIO.read(bufferedInputs);
		} finally {
			inputs.close();
		}
	}

	/**
	 * 发送截图指令到手机，并将所截图片上传指定位置保存
	 * 
	 * @param adb 命令位置
	 * @param saveFile 屏幕截图保存文件
	 */
	public static boolean getScreenShot(String adb, File saveFile) {
		String screenshot = adb + " shell /system/bin/screencap -p /sdcard/screenshot.png";
		ExecuteResult result = CommandHelper.executeCommand(screenshot);
		if (result.success()) {
			String pull = adb + " pull /sdcard/screenshot.png " + saveFile.getAbsolutePath();
			result = CommandHelper.executeCommand(pull);
			if (result.success()) {
				System.out.println("Screen shot saved in : " + saveFile.getAbsolutePath());
			} else {
				System.out.println(result.result());
			}
		} else {
			System.out.println(result.result());
		}

		return result.success();
	}

	/**
	 * 剪切图片
	 * 
	 * @param imageFile 源图片
	 * @param targetFile 目标图片
	 * @param x 走遍
	 * @param y
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	public void cutImage(File imageFile, File targetFile, int[] startPoint, int width, int height) throws IOException {
		ImageInputStream imageInputs = ImageIO.createImageInputStream(imageFile);

		try {
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInputs);
			ImageReader reader = (ImageReader) iterator.next();
			reader.setInput(imageInputs, true);

			ImageReadParam param = reader.getDefaultReadParam();
			Rectangle rectangle = new Rectangle(startPoint[0], startPoint[1], width, height);
			param.setSourceRegion(rectangle);

			BufferedImage bufferedImage = reader.read(0, param);
			ImageIO.write(bufferedImage, reader.getFormatName(), targetFile);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			imageInputs.close();
		}
	}

	/**
	 * 横向合成图片
	 */
	public static void xPic(String first, String second, String out) {
		try {
			/* 1 读取第一张图片 */
			File fileOne = new File(first);
			BufferedImage imageFirst = ImageIO.read(fileOne);
			int width = imageFirst.getWidth();// 图片宽度
			int height = imageFirst.getHeight();// 图片高度
			int[] imageArrayFirst = new int[width * height];// 从图片中读取RGB
			imageArrayFirst = imageFirst.getRGB(0, 0, width, height, imageArrayFirst, 0, width);

			/* 1 对第二张图片做相同的处理 */
			File fileTwo = new File(second);
			BufferedImage imageSecond = ImageIO.read(fileTwo);
			int widthTwo = imageSecond.getWidth();// 图片宽度
			int heightTwo = imageSecond.getHeight();// 图片高度
			int[] imageArraySecond = new int[widthTwo * heightTwo];
			imageArraySecond = imageSecond.getRGB(0, 0, widthTwo, heightTwo, imageArraySecond, 0, widthTwo);

			int h = height;
			if (height < heightTwo) {
				h = heightTwo;
			}

			// 生成新图片
			BufferedImage imageResult = new BufferedImage(width + widthTwo, h, BufferedImage.TYPE_INT_RGB);
			imageResult.setRGB(0, 0, width, height, imageArrayFirst, 0, width);// 设置左半部分的RGB
			imageResult.setRGB(width, 0, widthTwo, heightTwo, imageArraySecond, 0, widthTwo);// 设置右半部分的RGB
			File outFile = new File(out);
			ImageIO.write(imageResult, "jpg", outFile);// 写图片
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 实现图像的等比缩放
	 * 
	 * @param source 待处理的图片流
	 * @param targetW 宽度
	 * @param targetH 高度
	 * @return
	 */
	public static BufferedImage resize(BufferedImage source, int targetW, int targetH) {
		return zoomImage(source, targetW, targetH);
	}

	/**
	 * 按比例裁剪图片
	 * 
	 * @param source 待处理的图片流
	 * @param startX 开始x坐标
	 * @param startY 开始y坐标
	 * @param endX 结束x坐标
	 * @param endY 结束y坐标
	 * @return
	 */
	public static BufferedImage crop(BufferedImage source, int startX, int startY, int endX, int endY) {
		BufferedImage result = new BufferedImage(endX, endY, source.getType());
		for (int y = startY; y < endY + startY; y++) {
			for (int x = startX; x < endX + startX; x++) {
				int rgb = source.getRGB(x, y);
				result.setRGB(x - startX, y - startY, rgb);
			}
		}

		return result;
	}

	/**
	 * 旋转图片为指定角度
	 * 
	 * @param bufferedimage 目标图像
	 * @param degree 旋转角度
	 * @return
	 */
	public static BufferedImage rotateImage(final BufferedImage bufferedimage, final int degree) {
		int width = bufferedimage.getWidth();
		int height = bufferedimage.getHeight();
		int type = bufferedimage.getColorModel().getTransparency();
		BufferedImage img = new BufferedImage(width, height, type);

		Graphics2D graphics2d = img.createGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.rotate(Math.toRadians(degree), width / 2, height / 2 + Math.abs(width - height) / 2);
		graphics2d.drawImage(bufferedimage, 0, 0, null);
		graphics2d.dispose();

		return img;
	}

	/**
	 * 图片左转90度
	 * 
	 * @param bufferedimage
	 * @return
	 */
	public static BufferedImage rotateImageLeft90(BufferedImage bufferedimage) {
		int width = bufferedimage.getWidth();
		int height = bufferedimage.getHeight();
		int type = bufferedimage.getColorModel().getTransparency();
		BufferedImage img = new BufferedImage(width, height, type);

		Graphics2D graphics2d = img.createGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.rotate(Math.toRadians(270), width / 2, height / 2 + (width - height) / 2);
		graphics2d.drawImage(bufferedimage, 0, 0, null);
		graphics2d.dispose();

		return img;
	}

	/**
	 * 图片右转90度
	 * 
	 * @param bufferedImage
	 * @return
	 */
	public static BufferedImage rotateImageRight90(BufferedImage bufferedImage) {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		int type = bufferedImage.getColorModel().getTransparency();
		BufferedImage img = new BufferedImage(width, height, type);

		Graphics2D graphics2d = img.createGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.rotate(Math.toRadians(90), width / 2 - (width - height) / 2, height / 2);
		graphics2d.drawImage(bufferedImage, 0, 0, null);
		graphics2d.dispose();

		return img;
	}

	/**
	 * 将图片对折，180度镜像变换
	 * 
	 * @param imageFile 图片文件
	 * @throws Exception
	 */
	public void rotateImageOppo(File imageFile) throws Exception {
		BufferedImage bufferedimage = ImageIO.read(imageFile);
		int width = bufferedimage.getWidth();
		int height = bufferedimage.getHeight();
		int type = bufferedimage.getColorModel().getTransparency();
		BufferedImage img = new BufferedImage(width, height, type);

		Graphics2D graphics2d = img.createGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.rotate(Math.toRadians(180), width / 2, height / 2);
		graphics2d.drawImage(bufferedimage, 0, 0, null);
		graphics2d.dispose();

		String imageType = getImageType(imageFile.getName());
		ImageIO.write(img, imageType, imageFile);
	}

	/**
	 * 图片镜像处理,最左与最右边互换像素
	 * 
	 * @param imageFile 原始图片
	 * @param mirrorType 0 为上下反转、 1 为左右反转
	 */
	public void imageMirror(File imageFile, int mirrorType) {
		String imageType = getImageType(imageFile.getName());

		try {
			BufferedImage bufferedImage = ImageIO.read(imageFile);
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();
			int[][] datas = new int[width][height];
			for (int h = 0; h < height; h++) {
				for (int w = 0; w < width; w++) {
					datas[w][h] = bufferedImage.getRGB(w, h);
				}
			}

			int[][] tmps = new int[width][height];
			if (mirrorType == 0) {// 上下
				for (int h = 0, a = height - 1; h < height; h++, a--) {
					for (int w = 0; w < width; w++) {
						tmps[w][a] = datas[w][h];
					}
				}
			} else if (mirrorType == 1) {// 左右
				for (int h = 0; h < height; h++) {
					for (int w = 0, b = width - 1; w < width; w++, b--) {
						tmps[b][h] = datas[w][h];
					}
				}
			}

			for (int h = 0; h < height; h++) {
				for (int w = 0; w < width; w++) {
					bufferedImage.setRGB(w, h, tmps[w][h]);
				}
			}

			ImageIO.write(bufferedImage, imageType, imageFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 对图片进行强制放大或缩小
	 * 
	 * @param image 原始图片
	 * @param width 新图片宽度
	 * @param height 并图片高度
	 * @return 新图片
	 */
	public static BufferedImage zoomImage(BufferedImage image, int width, int height) {
		BufferedImage newImage = new BufferedImage(width, height, image.getType());

		Graphics g = newImage.getGraphics();
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();

		return newImage;
	}

	/**
	 * 获取Image文件后缀名。比如a.jpg返回jpg、b.png返回png
	 * 
	 * @param imageName 文件名称
	 * @return 文件后缀名
	 */
	private String getImageType(String imageName) {
		int splitIndex = imageName.lastIndexOf('.');
		if (splitIndex < 0 || splitIndex >= imageName.length()) {
			return null;
		}

		return imageName.substring(splitIndex + 1);
	}
}
