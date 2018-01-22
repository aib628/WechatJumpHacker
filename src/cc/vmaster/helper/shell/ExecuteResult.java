package cc.vmaster.helper.shell;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令执行结果
 * 
 * @author Sunshine
 *
 */
public class ExecuteResult {

	private boolean success = false;
	private final List<StringBuilder> results = new ArrayList<StringBuilder>(1);

	public boolean success() {
		return success;
	}

	public void success(boolean success) {
		this.success = success;
	}

	/**
	 * 获取第一条命令执行结果
	 */
	public String result() {
		return results.get(0).toString();
	}

	/**
	 * 获取第index条命令执行结果
	 * 
	 * @param index 命令索引，索引从1开始
	 * @return 如果索引数不正确，大于命令数，则返回Null
	 */
	public String result(int index) {
		if (results.size() < index) {
			return null;
		}

		return results.get(index).toString();
	}

	/**
	 * 获取所有结果列表
	 */
	public List<String> allResults() {
		List<String> results = new ArrayList<String>(this.results.size());
		for (StringBuilder sb : this.results) {
			results.add(sb.toString());
		}

		return results;
	}

	/**
	 * 保存第index条命令执行结果
	 * 
	 * @param index 命令索引，从1开始
	 * @param result 命令结果
	 * @return 当前命令存储容器
	 */
	public StringBuilder result(int index, String result) {
		if (results.size() < index) {

		}

		if (results.size() == index) {
			results.add(new StringBuilder());
		}

		StringBuilder sb = results.get(index);
		sb.append(result);

		return sb;
	}

	/**
	 * 如果没有则创建命令存储容器，有则使用当前，并保存命令执行结果
	 * 
	 * @param result 命令结果
	 * @return 当前命令存储容器
	 */
	public StringBuilder result(String result) {
		if (results.size() == 0) {
			results.add(new StringBuilder());
		}

		StringBuilder sb = results.get(results.size() - 1);
		sb.append(result);
		return sb;
	}

	/**
	 * 如果没有则创建命令存储容器，有则使用当前，并保存命令执行结果
	 * 
	 * @param result 命令结果
	 * @param createNew 如果已有存储器，是否新建存储器
	 * @return 当前命令存储容器
	 */
	public StringBuilder result(String result, boolean createNew) {
		if (results.size() == 0 || createNew) {
			results.add(new StringBuilder());
		}

		StringBuilder sb = results.get(results.size() - 1);
		sb.append(result);
		return sb;
	}

	/**
	 * 如果没有则创建命令存储容器，有则使用当前
	 * 
	 * @param createNew 如果已有存储器，是否新建存储器
	 * @return 当前命令存储容器
	 */
	public StringBuilder result(boolean createNew) {
		if (results.size() == 0 || createNew) {
			results.add(new StringBuilder());
		}

		return results.get(results.size() - 1);
	}
}
