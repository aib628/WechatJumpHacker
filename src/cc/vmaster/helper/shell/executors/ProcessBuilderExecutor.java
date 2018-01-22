package cc.vmaster.helper.shell.executors;

import java.io.IOException;

public class ProcessBuilderExecutor implements ICommandExecutor {

	@Override
	public Process execute(String command) throws IOException {
		return new ProcessBuilder(command).start();
	}

	@Override
	public Process[] execute(String[] commands, boolean groupCommand) throws IOException {
		if (groupCommand) {
			Process[] processes = new Process[1];
			processes[0] = new ProcessBuilder(commands).start();
			return processes;
		}

		else {
			int i = 0;
			Process[] processes = new Process[commands.length];
			for (String command : commands) {
				processes[i++] = new ProcessBuilder(command).start();
			}

			return processes;
		}
	}

}
