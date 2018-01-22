package cc.vmaster.helper.shell.executors;

import java.io.IOException;

public class RuntimeExecutor implements ICommandExecutor {

	@Override
	public Process execute(String command) throws IOException {
		return Runtime.getRuntime().exec(command);
	}

	@Override
	public Process[] execute(String[] commands, boolean groupCommand) throws IOException {
		if (groupCommand) {
			Process[] processes = new Process[1];
			processes[0] = Runtime.getRuntime().exec(commands);
			return processes;
		}

		else {
			int i = 0;
			Process[] processes = new Process[commands.length];
			for (String command : commands) {
				processes[i++] = Runtime.getRuntime().exec(command);
			}

			return processes;
		}
	}

}
