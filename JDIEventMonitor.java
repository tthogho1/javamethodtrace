import com.sun.jdi.request.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;

public class JDIEventMonitor extends Thread {

	private final String[] excludes = { "java.*", "javax.*", "sun.*", "com.sun.*","org.jboss.*","org.wildfly.*" };
	private final VirtualMachine vm;

	public JDIEventMonitor(VirtualMachine vm) {
		this.vm = vm;
	}

	private void setEventRequests() {
		EventRequestManager mgr = vm.eventRequestManager();
		MethodEntryRequest menr = mgr.createMethodEntryRequest();
		for (int i = 0; i < excludes.length; ++i) {
			menr.addClassExclusionFilter(excludes[i]);
		}
		menr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		menr.enable();
	}

	private boolean connected = true; 

	public void run() {
		setEventRequests();
		EventQueue queue = vm.eventQueue();
		while (connected) {
			try {
				EventSet eventSet = queue.remove();
				for (Event event : eventSet) {
					if (event instanceof MethodEntryEvent) {
						methodEntryEvent((MethodEntryEvent) event);
					}
				}
				eventSet.resume();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (VMDisconnectedException discExc) {
				handleDisconnectedException();
				break;
			}
		}
	}
	
	private void methodEntryEvent(MethodEntryEvent event) {
		Method meth = event.method();
		String threadName = event.thread().name();
		String className = meth.declaringType().name();
		if (meth.isConstructor()) {
			System.out.println(threadName + "entered " + className + " constructor");
		}else {
			System.out.println(threadName + "entered " + className + "." + meth.name() + "()" + " " + meth.location().lineNumber());
		}
	}

	private synchronized void handleDisconnectedException() {
		EventQueue queue = vm.eventQueue();
		while (connected) {
			try {
				EventSet eventSet = queue.remove();
				for (Event event : eventSet) {
					if (event instanceof VMDeathEvent)
						vmDeathEvent((VMDeathEvent) event);
					else if (event instanceof VMDisconnectEvent)
						vmDisconnectEvent((VMDisconnectEvent) event);
				}
				eventSet.resume();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean vmDied;
	private void vmDeathEvent(VMDeathEvent event) {
		vmDied = true;
		System.out.println("-- The application has exited --");
	}

	private void vmDisconnectEvent(VMDisconnectEvent event) {
		connected = false;
		if (!vmDied)
			System.out.println("- The application has been disconnected -");
	}
}
