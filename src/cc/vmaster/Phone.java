package cc.vmaster;

public class Phone {

	public static final int width = 1080;
	public static final int height = 1920;
	private static final int[] beginPoint = new int[] { width / 16, height / 6 };
	private static final int[] endPoint = new int[] { width * 15 / 16, height * 14 / 15 };

	/**
	 * 每次返回新坐标数组，防止被误修改
	 */
	public static int[] getBeginPoint() {
		return new int[] { beginPoint[0], beginPoint[1] };
	}

	/**
	 * 每次返回新坐标数组，防止被误修改
	 */
	public static int[] getEndPoint() {
		return new int[] { endPoint[0], endPoint[1] };
	}
}
