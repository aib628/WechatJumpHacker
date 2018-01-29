package cc.vmaster;

public class Phone {

	public static int width = 1080;
	public static int height = 1920;

	/**
	 * 每次返回新坐标数组，防止被误修改
	 */
	public static int[] getBeginPoint() {
		return new int[] { width / 16, height / 6 };
	}

	/**
	 * 每次返回新坐标数组，防止被误修改
	 */
	public static int[] getEndPoint() {
		return new int[] { width * 15 / 16, height * 14 / 15 };
	}
}
