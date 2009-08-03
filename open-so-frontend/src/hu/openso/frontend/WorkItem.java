/*
 * Classname            : hu.openso.frontend.WorkItem
 * Version information  : 1.0
 * Date                 : 2009.08.03.
 * Copyright notice     : GE Consumer & Industrial
 */

package hu.openso.frontend;

/**
 * Workitem interface for simple SwingWorker task management.
 * @author karnokd, 2009.08.03.
 * @version $Revision 1.0$
 */
public interface WorkItem extends Runnable {
	/** Called in EDT when the worker operation is completed. */
	void done();
}
