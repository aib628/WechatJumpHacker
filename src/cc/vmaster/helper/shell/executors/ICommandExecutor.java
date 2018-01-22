package cc.vmaster.helper.shell.executors;

import java.io.IOException;

public interface ICommandExecutor {

	/**
	 * 单条命令执行
	 * 
	 * @param command Shell命令
	 * @return 命令执行子进程Process
	 * @throws IOException
	 */
	public Process execute(String command) throws IOException;

	/**
	 * 多条命令、命令组执行
	 * 
	 * @param commands 命令组或多条命令
	 * @param groupCommand 是否是组命令，是则一次性发送执行，否则分批执行
	 * @return 命令执行子进程组Process
	 * @throws IOException
	 */
	public Process[] execute(String[] commands, boolean groupCommand) throws IOException;

}
