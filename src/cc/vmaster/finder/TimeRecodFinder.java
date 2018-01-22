package cc.vmaster.finder;

import java.awt.image.BufferedImage;

public abstract class TimeRecodFinder extends AbstractFinder {

	private long costs = 0;// 记录匹配总耗时

	/**
	 * 毫秒表示
	 */
	public long getMilliCosts() {
		return costs / 1_000_000;
	}

	/**
	 * 微秒表示
	 */
	public long getMicroCosts() {
		return costs / 1_000;
	}

	/**
	 * 纳秒表示
	 */
	public long getNanoCosts() {
		return costs;
	}

	/**
	 * 在指定坐标区间寻找目标(与直接调用find方法相比，该方法会记录匹配时间)
	 * 
	 * @param image 目标图片
	 * @param beginPoint 起始坐标
	 * @param endPoint 结束坐标
	 * @return 目标坐标中心位置
	 * @author VMaster
	 */
	public int[] findAndRecord(BufferedImage image, int[] beginPoint, int[] endPoint) {
		TimeRecoder recoder = TimeRecoder.getRecoder().begin();

		int[] point = find(image, beginPoint, endPoint);

		costs = recoder.end();

		return point;
	}

	/**
	 * 获取Recoder实例用于自定义耗时记录
	 */
	protected static TimeRecoder getRecoder() {
		return TimeRecoder.getRecoder();
	}

	/**
	 * 记录匹配耗时
	 * 
	 * @author VMaster
	 */
	protected static class TimeRecoder {

		private long beginTime = 0;// 记录开始时间

		/**
		 * 获取实例
		 */
		protected static TimeRecoder getRecoder() {
			return new TimeRecoder();
		}

		public TimeRecoder begin() {
			this.beginTime = System.nanoTime();
			return this;
		}

		public long end() {
			return getTimes();
		}

		protected long getTimes() {
			return System.nanoTime() - beginTime;
		}
	}
}
