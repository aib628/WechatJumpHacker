package cc.vmaster.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 
 * 加载配置文件工具类,用以获取资源URL或者文件输入流
 * 
 * @author Sunshine
 *
 */
public class IOUtils {

	/**
	 * 获取文件URL
	 * 
	 * @param resourcePath 文件绝对地址
	 * @return 文件URL
	 */
	public static URL getURL(String resourcePath) {
		File file = new File(resourcePath);
		if (file.exists() && !file.isDirectory()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * 根据路径取得资源URL。 Class.getResource()有两种用法,一种是以“/”开头资源绝对路径,一种是相对于Class的相对路径。
	 * ClassLoader.getResource()表示相对于ClassPath的的绝对路径。
	 * 
	 * @param clazz 调用者Class对象,为保持和调用者相对ClassPath路径的一致性
	 * @param path 资源路径名称,可以使用相对包、相对ClassPath、绝对ClassPath及classpath:xxxx形式的路径
	 * @return 资源URL
	 */
	public static URL getURL(Class<?> clazz, String resourcePath) {
		URL url = getURL(resourcePath);// 先以文件绝对路径方式获取
		if (url != null) {
			return url;
		}

		// 以“classpath：”前缀开头的路径表示资源位置项目根目录，与以“/”开头的资源位置相同。
		if (resourcePath.toLowerCase().startsWith("classpath:")) {
			resourcePath = "/" + resourcePath.replaceFirst("classpath:", "");
		}

		// 第一步：Class.getResource()用法一：以“/”开头的资源绝对路径
		if (resourcePath.startsWith("/")) {
			url = clazz.getResource(resourcePath);
		}

		// 第二步：资源路径不以“/”开头。或者使用上述方式未获取到资源时，尝试使用Class.getResource()用法二获取
		else if (url == null) {
			url = clazz.getResource(resourcePath);
		}

		// 第三步：如果url依旧为null。即上述两种方式均未成功获取到，则最后尝试使用ClassLoader获取，其实质与上下文绝对资源路径相同
		if (url == null) {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			if (loader == null) {
				loader = clazz.getClassLoader();
			}

			url = loader.getResource(resourcePath);
		}

		// 第四步：如果Url依旧为空，尝试创建文件判断是否存在，然后转换为URL返回
		if (url == null) {
			try {
				File file = new File(clazz.getResource("/").getPath() + resourcePath);
				if (file.exists()) {
					return file.toURI().toURL();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return url;
	}

	/**
	 * 根据路径取得资源输入流。
	 * 
	 * @param clazz 调用者Class对象,为保持和调用者相对ClassPath路径的一致性
	 * @param resourcePath 资源路径名称,可以使用相对包、相对ClassPath、绝对ClassPath及classpath:xxxx形式的路径
	 * @return 资源输入流
	 * @throws IOException 输入流获取异常
	 */
	public static InputStream getInputStream(Class<?> clazz, String resourcePath) {
		URL url = getURL(clazz, resourcePath);
		if (url == null) {
			return null;
		}

		try {
			return url.openStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 获取文件输入流
	 * 
	 * @param resourcePath 文件绝对路径
	 * @return 文件输入流。文件不存在或为文件夹时返回Null
	 */
	public static InputStream getInputStream(String resourcePath) {
		File resourceFile = new File(resourcePath);
		return getInputStream(resourceFile);
	}

	/**
	 * 获取指定文件输入流
	 * 
	 * @param resourceFile 指定文件
	 * @return 文件输入流。文件不存在或为文件夹时返回Null
	 */
	public static InputStream getInputStream(File resourceFile) {
		if (resourceFile == null) {
			return null;
		}

		if (!resourceFile.exists() || resourceFile.isDirectory()) {
			return null;
		}

		try {
			return new FileInputStream(resourceFile);
		} catch (FileNotFoundException e) {
			return null; // cannot be happened
		}
	}

}
