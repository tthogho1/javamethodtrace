import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.AttachingConnector;

/**
 * Hello world example for Java Debugging API i.e. JDI. Very basic & simple
 * example.
 * 
 * @author ravik
 *
 */
public class JDIMEthodTrace {

	public static void main(String[] args) throws Exception {

		// int lineNumberToPutBreakpoint = 18;
		VirtualMachine vm = null;
		try {

			AttachingConnector connector = null;
			connector = getConnector();
			vm = connect(connector);
			monitorJVM(vm);

		} catch (Exception e) {
			
			e.printStackTrace();

		}

	}

	private static AttachingConnector getConnector() {
		VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();
		for (Connector connector : vmManager.attachingConnectors()) {
			System.out.println(connector.name());
			if ("com.sun.jdi.SocketAttach".equals(connector.name())) {
				return (AttachingConnector) connector;
			}
		}
		throw new IllegalStateException();
	}

	private static VirtualMachine connect(AttachingConnector connector) throws Exception {
		Map<String, Connector.Argument> args = connector.defaultArguments();
		args.get("hostname").setValue("localhost");
		args.get("port").setValue("8787");

		return connector.attach(args);
	}

	private static void monitorJVM(VirtualMachine vm) {
		// start JDI event handler which displays trace info
		JDIEventMonitor watcher = new JDIEventMonitor(vm);

		watcher.start();
		/*
		 * redirect VM's output and error streams to the system output and error streams
		 */
		Process process = vm.process();
		Thread errRedirect = new StreamRedirecter("error reader", process.getErrorStream(), System.err);
		Thread outRedirect = new StreamRedirecter("output reader", process.getInputStream(), System.out);
		errRedirect.start();
		outRedirect.start();
		vm.resume(); // start the application
		try {
			watcher.join(); // Wait until JDI watcher terminates
			errRedirect.join(); // make sure all outputs have been forwarded
			outRedirect.join();
		} catch (InterruptedException e) {
		}
	}

}