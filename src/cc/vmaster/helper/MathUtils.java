package cc.vmaster.helper;

public class MathUtils {

	/**
	 * 计算两点间距离
	 */
	public static int calcDistance(int[] beginPoint, int[] endPoint) {
		double a = Math.pow(endPoint[0] - beginPoint[0], 2);
		double b = Math.pow(endPoint[1] - beginPoint[1], 2);
		return Double.valueOf(Math.sqrt(a + b)).intValue();
	}

}
