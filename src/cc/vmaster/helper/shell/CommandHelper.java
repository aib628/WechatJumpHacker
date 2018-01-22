package cc.vmaster.helper.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import cc.vmaster.helper.shell.executors.ICommandExecutor;
import cc.vmaster.helper.shell.executors.ProcessBuilderExecutor;
import cc.vmaster.helper.shell.executors.RuntimeExecutor;

public class CommandHelper {

	public static final String COMMAND_SU = "su";// 切换Root用户
	public static final String COMMAND_SH = "sh";// 执行SH命令
	public static final String COMMAND_EXIT = "exit\n";// 退出Shell
	public static final String COMMAND_LINE_END = "\n";

	/**
	 * 命令执行器Executor
	 */
	private ICommandExecutor executor;

	/**
	 * 是否使用OutputStream写入命令执行方式
	 */
	private boolean writeCommandMode = false;

	/**
	 * 使用Root用户身份执行
	 */
	private boolean useRoot = false;

	/**
	 * 存储命令执行结果
	 */
	private final ExecuteResult result = new ExecuteResult();

	/**
	 * 唯一构造方法
	 * 
	 * @param executor 设置执行器
	 */
	private CommandHelper(ICommandExecutor executor) {
		this.executor = executor;
	}

	/**
	 * 检查是否拥有Root权限
	 */
	public static boolean checkRootPermission() {
		return executeCommand("echo root").success();
	}

	/**
	 * 选择使用ProcessBuilder作为执行器
	 */
	public static CommandHelper useProcessBuilderExecutor() {
		return new CommandHelper(new ProcessBuilderExecutor());
	}

	/**
	 * 选择使用Runtime作为执行器
	 */
	public static CommandHelper useRuntimeExecutor() {
		return new CommandHelper(new RuntimeExecutor());
	}

	/**
	 * 利用ProcessBuilder执行Shell命令，空格间以字符数组方式表示。如执行点击事件为： input tap x y
	 * 
	 * <pre>
	 * 常用命令： 
	 * HOME：adb shell input keyevent KeyEvent.KEYCODE_HOME 
	 * 点击事件：adb shell input tap x y 
	 * 常按事件：adb shell input swipe 100 250 200 280
	 * </pre>
	 * 
	 * @param command
	 */
	public static ExecuteResult executeCommand(String... commands) {
		CommandHelper helper = useRuntimeExecutor();
		return helper.execute(commands);
	}

	/**
	 * 利用ProcessBuilder执行Shell命令，空格间以字符数组方式表示。如执行点击事件为： input tap x y
	 * 
	 * <pre>
	 * 常用命令： 
	 * HOME：adb shell input keyevent KeyEvent.KEYCODE_HOME 
	 * 点击事件：adb shell input tap x y 
	 * 常按事件：adb shell input swipe 100 250 200 280
	 * </pre>
	 * 
	 * @param command
	 */
	public static ExecuteResult executeCommand(boolean groupCommand, String... commands) {
		CommandHelper helper = useRuntimeExecutor();
		return helper.execute(groupCommand, commands);
	}

	/**
	 * 使用OutputStream写入命令方式来执行命令
	 */
	public CommandHelper writeCommandMode() {
		this.writeCommandMode = true;
		return this;
	}

	/**
	 * 使用Root用户身份执行命令
	 */
	public CommandHelper useRootRun() {
		this.useRoot = true;
		return this;
	}

	/**
	 * 默认非命令组。逐条执行
	 * 
	 * @param commands 命令列表
	 * @return 执行结果
	 */
	public ExecuteResult execute(String... commands) {
		return execute(false, commands);
	}

	/**
	 * 多条或命令组执行
	 * 
	 * @param groupCommand 是否组命令，即是否一性次执行，还是逐条执行
	 * @param commands 命令列表
	 * @return 执行结果
	 */
	public ExecuteResult execute(boolean groupCommand, String... commands) {
		try {
			if (commands.length == 1) {
				int exitValue = invokeExecutor(commands[0]);
				result.success(exitValue == 0);
			}

			else {
				int exitValue = invokeExecutor(commands, groupCommand);
				result.success(exitValue == 0);
			}
		} catch (IOException e) {
			e.printStackTrace();
			result.result(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			result.result(e.getMessage());
		}

		return result;
	}

	/**
	 * 
	 * @param command Shell命令
	 * @return 命令执行结果，ExitValue=0表示执行成功
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private int invokeExecutor(String command) throws InterruptedException, IOException {
		Process process = null;
		if (writeCommandMode) {
			if (useRoot) {
				process = executor.execute(COMMAND_SU);
			} else {
				process = executor.execute(COMMAND_SH);
			}

			writeCommand(process.getOutputStream(), command);
		} else {
			process = executor.execute(command);
		}

		return afterExecute(process);
	}

	/**
	 * 调用执行器执行命令
	 * 
	 * @param commands Shell命令组或命令数组
	 * @param groupCommand 是否命令组，是则一次执行。否则分批执行。writeCommandMode模式下，该参数失效，会分批执行，功能与groupCommand=false类似
	 * @return 执行结果，全部命令执行成功才会执行成功，返回使用二进制方式返回，从低往高分别对应命令组执行结果
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private byte invokeExecutor(String[] commands, boolean groupCommand) throws InterruptedException, IOException {
		if (writeCommandMode) {
			Process process = null;
			if (useRoot) {
				process = executor.execute(COMMAND_SU);
			} else {
				process = executor.execute(COMMAND_SH);
			}

			writeCommand(process.getOutputStream(), commands);
			return (byte) ((afterExecute(process) == 0) ? 0x00 : 0x01);
		} else {
			byte exitValues = 0x7F;// 默认全部失败，byte值范围为[-127~127]，因此最大值为0x7F
			Process[] processes = executor.execute(commands, groupCommand);
			for (Process process : processes) {
				int exitValue = afterExecute(process);
				if (exitValue != 0) {
					exitValues = (byte) (exitValues << 1);
				}
			}

			return exitValues;
		}
	}

	/**
	 * 命令执行后处理，并返回执行结果
	 * 
	 * @param process 处理Process对象
	 * @param results 处理结果存放处
	 * @throws InterruptedException
	 */
	private int afterExecute(Process process) throws InterruptedException {
		int exitValue = process.waitFor();

		try {
			if (exitValue == 0) {
				readResult(process, result.result(false));
			}

			else {
				readError(process, result.result(false));
			}
		} finally {
			process.destroy();
		}

		return exitValue;
	}

	/**
	 * 读取命令执行结果信息
	 * 
	 * @param process 处理器
	 * @param results 信息文本
	 * @throws InterruptedException
	 */
	private void readResult(Process process, StringBuilder results) throws InterruptedException {
		Thread readThread = new Thread(new StreamReader(process.getInputStream(), results));

		readThread.start();
		readThread.join();
	}

	/**
	 * 读取命令执行错误信息
	 * 
	 * @param process 处理器
	 * @param results 信息文本
	 * @throws InterruptedException
	 */
	private void readError(Process process, StringBuilder results) throws InterruptedException {
		Thread readThread = new Thread(new StreamReader(process.getErrorStream(), results));

		readThread.start();
		readThread.join();
	}

	/**
	 * 通过写入流方式执行Shell命令
	 * 
	 * @param outputs 输出流
	 * @param commands 命令
	 * @throws IOException
	 */
	private void writeCommand(OutputStream outputs, String... commands) throws IOException {
		try {
			DataOutputStream dataOutputs = new DataOutputStream(outputs);
			for (String command : commands) {
				if (command == null) {
					continue;
				}

				// don't use os.writeBytes(command), avoid chinese charset error
				dataOutputs.write(command.getBytes());
				dataOutputs.writeBytes(COMMAND_LINE_END);
				dataOutputs.flush();
			}

			dataOutputs.writeBytes(COMMAND_EXIT);
			dataOutputs.flush();
		} finally {
			outputs.close();
		}
	}

	/**
	 * 读取线程
	 * 
	 * @author Sunshine
	 */
	private final class StreamReader implements Runnable {

		private InputStream inputs;
		private StringBuilder results;

		public StreamReader(InputStream inputs, StringBuilder results) {
			this.inputs = inputs;
			this.results = results;
		}

		@Override
		public void run() {
			if (inputs == null) {
				return;
			}

			try {
				read(inputs, results);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 读取流内容
		 * 
		 * @param inputs 输入流
		 * @param results 读取结果
		 * @throws IOException 读取IO异常
		 */
		private void read(InputStream inputs, StringBuilder results) throws IOException {
			try {
				InputStreamReader reader = new InputStreamReader(inputs);
				BufferedReader input = new BufferedReader(reader);

				String line = null;
				while ((line = input.readLine()) != null) {
					results.append(line);
				}
			} finally {
				inputs.close();
			}
		}
	}

}
