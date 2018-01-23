package cc.vmaster.converter;

import java.awt.image.BufferedImage;

public interface IConverter {

	/**
	 * RGB通道转换器
	 * 
	 * @param image 图片
	 * @param beginPoint 起始坐标
	 * @param endPoint 结束坐标
	 */
	public BufferedImage convert(BufferedImage image, int[] beginPoint, int[] endPoint);

}
