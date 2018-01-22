package cc.vmaster.finder;

import java.awt.image.BufferedImage;

public interface IFinder {

	public final int maxInt = Integer.MAX_VALUE;
	public final int minInt = Integer.MIN_VALUE;

	/**
	 * 在指定坐标区间寻找目标
	 * 
	 * @param image 目标图片
	 * @param beginPoint 起始坐标
	 * @param endPoint 结束坐标
	 * @return 目标坐标中心位置
	 * @author VMaster
	 */
	public int[] find(BufferedImage image, int[] beginPoint, int[] endPoint);

}
