package cc.vmaster;

public interface IPhone {

	/**
	 * 屏幕宽度
	 */
	public int getWidth();

	/**
	 * 屏幕调试
	 */
	public int getHeight();

	/**
	 * 起始坐标
	 */
	public int[] getBeginPoint();

	/**
	 * 结束坐标
	 */
	public int[] getEndPoint();

}
