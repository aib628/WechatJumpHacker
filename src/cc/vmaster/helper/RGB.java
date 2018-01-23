package cc.vmaster.helper;

/**
 * RGB定义
 * 
 * @author VMaster
 */
public class RGB {
	public int R;
	public int G;
	public int B;

	public RGB(int R, int G, int B) {
		this.R = R;
		this.G = G;
		this.B = B;
	}

	/**
	 * 通过像素值计算RGB值
	 * 
	 * @param pixel 像素值
	 */
	public static RGB calcRGB(int pixel) {
		int r = (pixel & 0Xff0000) >> 16;
		int g = (pixel & 0Xff00) >> 8;
		int b = (pixel & 0Xff);

		return new RGB(r, g, b);
	}
}