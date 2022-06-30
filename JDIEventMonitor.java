import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.EventRequest;

import com.sun.jdi.Method;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;

public class JDIEventMonitor extends Thread {

	private final String[] excludes = { "java.*", "javax.*", "sun.*", "com.sun.*" };
	private final VirtualMachine vm ; // the JVM

	public JDIEventMonitor(VirtualMachine vm) {
		this.vm = vm;
	}
	
	private void setEventRequests() {
		EventRequestManager mgr = vm.eventRequestManager();
		MethodEntryRequest menr = mgr.createMethodEntryRequest();
		for (int i = 0; i < excludes.length; ++i) { // report method entries
			menr.addClassExclusionFilter(excludes[i]);
		}
		menr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		menr.enable();

	} //

	private boolean connected = true; // connected to VM?

	public void run() {
		setEventRequests();
		EventQueue queue = vm.eventQueue();
		while (connected) {
			try {
				EventSet eventSet = queue.remove();
				for (Event event : eventSet) {
					handleEvent(event);
				}
				eventSet.resume();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} // Ignore
			catch (VMDisconnectedException discExc) {
				handleDisconnectedException();
				break;
			}
		}
	} // end of run()

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
			} // ignore
		}
	} // end of handleDisconnectedException()

	private boolean vmDied; // has VM death occurred?

	private void vmDeathEvent(VMDeathEvent event)
	// Notification of VM termination
	{
		vmDied = true;
		System.out.println("-- The application has exited --");
	}

	private void vmDisconnectEvent(VMDisconnectEvent event)
	/*
	 * Notification of VM disconnection, either through normal termination or
	 * because of an exception/error.
	 */
	{
		connected = false;
		if (!vmDied)
			System.out.println("- The application has been disconnected -");
	}

	private void handleEvent(Event event) {
		// method events
		if (event instanceof MethodEntryEvent)
			methodEntryEvent((MethodEntryEvent) event);
	}


	private void methodEntryEvent(MethodEntryEvent event)
	// entered a method but no code executed yet
	{
		Method meth = event.method();
		String className = meth.declaringType().name();
		System.out.println();
		if (meth.isConstructor())
			System.out.println("entered " + className + " constructor");
		else
			System.out.println("entered " + className + "." + meth.name() + "()");
	}
}
