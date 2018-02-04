package cc.vmaster;

public class Phone implements IPhone {

	private final int width = 1080;
	private final int height = 1920;

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int[] getBeginPoint() {
		return new int[] { width / 16, height / 6 };
	}

	@Override
	public int[] getEndPoint() {
		return new int[] { width * 15 / 16, height * 14 / 15 };
	}
}
