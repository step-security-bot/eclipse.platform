/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.synchronize;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.core.*;
import org.eclipse.team.internal.core.subscribers.SyncInfoStatistics;
import org.eclipse.team.internal.core.subscribers.SyncSetChangedEvent;

/**
 * A dynamic collection of {@link SyncInfo} objects that provides
 * change notification to registered listeners. Batching of change notifications
 * can be accomplished using the <code>beginInput/endInput</code> methods. 
 * 
 * @see SyncInfoTree
 * @see SyncInfo
 * @see ISyncInfoSetChangeListener
 * @since 3.0
 */
public class SyncInfoSet {
	// fields used to hold resources of interest
	// {IPath -> SyncInfo}
	private Map resources = Collections.synchronizedMap(new HashMap());

	// keep track of number of sync kinds in the set
	private SyncInfoStatistics statistics = new SyncInfoStatistics();
	
	// keep track of errors that occurred while trying to populate the set
	private Map errors = new HashMap();
	
	private boolean lockedForModification;
	
	/**
	 * Create an empty set.
	 */
	public SyncInfoSet() {
	}

	/**
	 * Create a <code>SyncInfoSet</code> containing the given <code>SyncInfo</code>
	 * instances.
	 * 
	 * @param infos the <code>SyncInfo</code> instances to be contained by this set
	 */
	public SyncInfoSet(SyncInfo[] infos) {
		this();
		// use the internal add since we can't have listeners at this point anyway
		for (int i = 0; i < infos.length; i++) {
			internalAdd(infos[i]);
		}
	}
	
	/**
	 * Return an array of <code>SyncInfo</code> for all out-of-sync resources that are contained by the set.
	 * 
	 * @return an array of <code>SyncInfo</code>
	 */
	public synchronized SyncInfo[] getSyncInfos() {
		return (SyncInfo[]) resources.values().toArray(new SyncInfo[resources.size()]);
	}
	
	/**
	 * Return all out-of-sync resources contained in this set. The default implementation
	 * uses <code>getSyncInfos()</code> to determine the resources contained in the set.
	 * Subclasses may override to optimize.
	 * 
	 * @return all out-of-sync resources contained in the set
	 */
	public IResource[] getResources() {
		SyncInfo[] infos = getSyncInfos();
		List resources = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			resources.add(info.getLocal());
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	/**
	 * Return the <code>SyncInfo</code> for the given resource or <code>null</code>
	 * if the resource is not contained in the set.
	 * 
	 * @param resource the resource
	 * @return the <code>SyncInfo</code> for the resource or <code>null</code> if
	 * the resource is in-sync or doesn't have synchronization information in this set.
	 */
	public synchronized SyncInfo getSyncInfo(IResource resource) {
		return (SyncInfo)resources.get(resource.getFullPath());
	}

	/**
	 * Return the number of out-of-sync resources contained in this set.
	 * 
	 * @return the size of the set.
	 * @see #countFor(int, int)
	 */
	public synchronized int size() {
		return resources.size();		
	}

	/**
	 * Return the number of out-of-sync resources in the given set whose sync kind
	 * matches the given kind and mask (e.g. <code>(SyncInfo#getKind() & mask) == kind</code>).
	 * <p>
	 * For example, this will return the number of outgoing changes in the set:
	 * <pre>
	 *  long outgoing =  countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
	 * </pre>
	 * </p>
	 * @param kind the sync kind
	 * @param mask the sync kind mask
	 * @return the number of matching resources in the set.
	 */
	public long countFor(int kind, int mask) {
		return statistics.countFor(kind, mask);
	}
	
	/**
	 * Returns <code>true</code> if there are any conflicting nodes in the set, and
	 * <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if there are any conflicting nodes in the set, and
	 * <code>false</code> otherwise.
	 */
	public boolean hasConflicts() {
		return countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK) > 0;
	}
	
	/**
	 * Return whether the set is empty.
	 * 
	 * @return <code>true</code> if the set is empty
	 */
	public synchronized boolean isEmpty() {
		return resources.isEmpty();
	}
	
	/**
	 * Add the <code>SyncInfo</code> to the set, replacing any previously existing one.
	 * 
	 * @param info the new <code>SyncInfo</code>
	 */
	protected synchronized void internalAdd(SyncInfo info) {
		Assert.isTrue(!lockedForModification);
		IResource local = info.getLocal();
		IPath path = local.getFullPath();
		SyncInfo oldSyncInfo = (SyncInfo)resources.put(path, info); 
		if(oldSyncInfo == null) {
			statistics.add(info);
		} else {
			statistics.remove(oldSyncInfo);
			statistics.add(info);
		}
	}
	
	/**
	 * Remove the resource from the set, updating all internal data structures.
	 * 
	 * @param resource the resource to be removed
	 * @return the <code>SyncInfo</code> that was just removed
	 */
	protected synchronized SyncInfo internalRemove(IResource resource) {
		Assert.isTrue(!lockedForModification);
		IPath path = resource.getFullPath();
		SyncInfo info = (SyncInfo)resources.remove(path);
		if (info != null) {
			statistics.remove(info);
		}
		return info;
	}
	
	/**
	 * Registers the given listener for sync info set notifications. Has
	 * no effect if an identical listener is already registered.
	 * 
	 * @param listener listener to register
	 */
	public void addSyncSetChangedListener(ISyncInfoSetChangeListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Deregisters the given listener for participant notifications. Has
	 * no effect if listener is not already registered.
	 * 
	 * @param listener listener to deregister
	 */
	public void removeSyncSetChangedListener(ISyncInfoSetChangeListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * Reset the sync set so it is empty. Listeners are notified of the change.
	 */
	public void clear() {
		try {
			beginInput();
			errors.clear();
			resources.clear();
			statistics.clear();
			getChangeEvent().reset();
		} finally {
			endInput(null);
		}
	}

	/*
	 * Run the given runnable. This operation
	 * will block other threads from modifying the 
	 * set and postpone any change notifications until after the runnable
	 * has been executed. Mutable subclasses must override.
	 * <p>
	 * The given runnable may be run in the same thread as the caller or
	 * more be run asynchronously in another thread at the discretion of the
	 * subclass implementation. However, it is gaurenteed that two invocations
	 * of <code>run</code> performed in the same thread will be executed in the 
	 * same order even if run in different threads.
	 * </p>
	 * @param runnable a runnable
	 * @param progress a progress monitor or <code>null</code>
	 */
	private void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		try {
			beginInput();
			runnable.run(Policy.subMonitorFor(monitor, 80));
		} catch (CoreException e) {
			addError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, ITeamStatus.SYNC_INFO_SET_ERROR, e.getMessage(), e, null));
		} finally {
			endInput(Policy.subMonitorFor(monitor, 20));
		}
	}
	
	/**
	 * Connect the listener to the sync set in such a fashion that the listener will
	 * be connected the the sync set using <code>addChangeListener</code>
	 * and issued a reset event. This is done to provide a means of connecting to the 
	 * sync set and initializing a model based on the sync set without worrying about 
	 * missing events.
	 * <p>
	 * The reset event may be done in the context of this method invocation or may be
	 * done in another thread at the discretion of the <code>SyncInfoSet</code>
	 * implementation. 
	 * </p><p>
	 * Disconnecting is done by calling <code>removeChangeListener</code>. Once disconnected,
	 * a listener can reconnect to be reinitialized.
	 * </p>
	 * @param listener the listener that should be connected to this set
	 * @param monitor a progress monitor
	 */
	public void connect(final ISyncInfoSetChangeListener listener, IProgressMonitor monitor) {
		run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(null, 100);
					addSyncSetChangedListener(listener);
					listener.syncInfoSetReset(SyncInfoSet.this, Policy.subMonitorFor(monitor, 95));
				} finally {
					monitor.done();
				}
			}
		}, monitor);
	}

	private ILock lock = Platform.getJobManager().newLock();

	private Set listeners = Collections.synchronizedSet(new HashSet());

	private SyncSetChangedEvent changes = createEmptyChangeEvent();

	/**
	 * Add the given <code>SyncInfo</code> to the set. A change event will
	 * be generated unless the call to this method is nested in between calls
	 * to <code>beginInput()</code> and <code>endInput(IProgressMonitor)</code>
	 * in which case the event for this addition and any other sync set
	 * change will be fired in a batched event when <code>endInput</code>
	 * is invoked.
	 * <p>
	 * Invoking this method outside of the above mentioned block will result
	 * in the <code>endInput(IProgressMonitor)</code> being invoked with a null
	 * progress monitor. If responsiveness is required, the client should always
	 * nest sync set modifications within <code>beginInput/endInput</code>.
	 * </p>
	 * @param info the sync info to be added to this set.
	 */
	public void add(SyncInfo info) {
		try {
			beginInput();
			boolean alreadyExists = getSyncInfo(info.getLocal()) != null;
			internalAdd(info);
			if (alreadyExists) {
				getChangeEvent().changed(info);
			} else {
				getChangeEvent().added(info);
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Add all the syncinfo from the given set to this set.
	 * 
	 * @param set the set whose sync info should be added to this set
	 */
	public void addAll(SyncInfoSet set) {
		try {
			beginInput();
			SyncInfo[] infos = set.getSyncInfos();
			for (int i = 0; i < infos.length; i++) {
				add(infos[i]);
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Remove the given local resource from the set.
	 * 
	 * @param resource the local resource to remove
	 */
	public synchronized void remove(IResource resource) {
		try {
			beginInput();
			SyncInfo info = internalRemove(resource);
			getChangeEvent().removed(resource, info);
		} finally {
			endInput(null);
		}
	}

	/**
	 * Remove all the given resources from the set.
	 * 
	 * @param resources the resources to be removed
	 */
	public void removeAll(IResource[] resources) {
		try {
			beginInput();
			for (int i = 0; i < resources.length; i++) {
				remove(resources[i]);			
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Removes all conflicting nodes from this set.
	 */
	public void removeConflictingNodes() {
		rejectNodes(new SyncInfoDirectionFilter(SyncInfo.CONFLICTING));
	}

	/**
	 * Removes all outgoing nodes from this set.
	 */
	public void removeOutgoingNodes() {
		rejectNodes(new SyncInfoDirectionFilter(SyncInfo.OUTGOING));
	}

	/**
	 * Removes all incoming nodes from this set.
	 */
	public void removeIncomingNodes() {
		rejectNodes(new SyncInfoDirectionFilter(SyncInfo.INCOMING));
	}

	/**
	 * Indicate whether the set has nodes matching the given filter.
	 * 
	 * @param filter a sync info filter
	 */
	public boolean hasNodes(FastSyncInfoFilter filter) {
		SyncInfo[] infos = getSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info != null && filter.select(info)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all nodes from this set that do not match the given filter
	 * leaving only those that do match the filter.
	 * 
	 * @param filter a sync info filter
	 */
	public void selectNodes(FastSyncInfoFilter filter) {
		try {
			beginInput();
			SyncInfo[] infos = getSyncInfos();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				if (info == null || !filter.select(info)) {
					remove(info.getLocal());
				}
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Removes all nodes from this set that match the given filter
	 * leaving those that do not match the filter.
	 * 
	 * @param filter a sync info filter
	 */
	public void rejectNodes(FastSyncInfoFilter filter) {
		try {
			beginInput();
			SyncInfo[] infos = getSyncInfos();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				if (info != null && filter.select(info)) {
					remove(info.getLocal());
				}
			}
		} finally {
			endInput(null);
		}
	}

	/**
	 * Return all nodes in this set that match the given filter.
	 * 
	 * @param filter a sync info filter
	 */
	public SyncInfo[] getNodes(FastSyncInfoFilter filter) {
		List result = new ArrayList();
		SyncInfo[] infos = getSyncInfos();
		for (int i = 0; i < infos.length; i++) {
			SyncInfo info = infos[i];
			if (info != null && filter.select(info)) {
				result.add(info);
			}
		}
		return (SyncInfo[]) result.toArray(new SyncInfo[result.size()]);
	}

	/**
	 * Returns <code>true</code> if this sync set has incoming changes.
	 * Note that conflicts are not considered to be incoming changes.
	 * 
	 * @return <code>true</code> if this sync set has incoming changes.
	 */
	public boolean hasIncomingChanges() {
		return countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK) > 0;
	}

	/**
	 * Returns <code>true</code> if this sync set has outgoing changes.
	 * Note that conflicts are not considered to be outgoing changes.
	 * 
	 * @return <code>true</code> if this sync set has outgoing changes.
	 */
	public boolean hasOutgoingChanges() {
		return countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK) > 0;
	}

	/**
	 * This method is used to obtain a lock on the set which ensures thread safety
	 * and batches change notification. If the set is locked by another thread, 
	 * the calling thread will block until the lock 
	 * becomes available. This method uses an <code>org.eclipse.core.runtime.jobs.ILock</code>.
	 * <p>
	 * It is important that the lock is released after it is obtained. Calls to <code>endInput</code>
	 * should be done in a finally block as illustrated in the following code snippet.
	 * <pre>
	 *   try {
	 *       set.beginInput();
	 *       // do stuff
	 *   } finally {
	 *      set.endInput(progress);
	 *   }
	 * </pre>
	 * </p><p>
	 * Calls to <code>beginInput</code> and <code>endInput</code> can be nested and must be matched.
	 * </p>
	 */
	public void beginInput() {
		lock.acquire();
	}

	/**
	 * This method is used to release the lock on this set. The prgress monitor is needed to allow
	 * listeners to perform long-running operations is reponse to the set change. The lock is held
	 * while the listeners are notified so listeners must be cautious in order to avoid deadlock.
	 */
	public void endInput(IProgressMonitor monitor) {
		try {
			if (lock.getDepth() == 1) {
				// Remain locked while firing the events so the handlers 
				// can expect the set to remain constant while they process the events
				fireChanges(Policy.monitorFor(monitor));
			}
		} finally {
			lock.release();
		}
	}

	/**
	 * Reset the changes accumulated so far by this set. This method is not
	 * intended to be invoked or implemented by clients.
	 */
	protected void resetChanges() {
		changes = createEmptyChangeEvent();
	}

	/**
	 * Create an empty change event. Subclass may override to provided specialized event types
	 * 
	 * @return an empty change event
	 */
	protected SyncSetChangedEvent createEmptyChangeEvent() {
		return new SyncSetChangedEvent(this);
	}

	private void fireChanges(final IProgressMonitor monitor) {
		// Use a synchronized block to ensure that the event we send is static
		final SyncSetChangedEvent event;
		synchronized(this) {
			event = getChangeEvent();
			resetChanges();
		}
		// Ensure that the list of listeners is not changed while events are fired.
		// Copy the listeners so that addition/removal is not blocked by event listeners
		if(event.isEmpty() && ! event.isReset()) return;
		ISyncInfoSetChangeListener[] allListeners = getListeners();
		// Fire the events using an ISafeRunnable
		final ITeamStatus[] newErrors = event.getErrors();
		monitor.beginTask(null, 100 + (newErrors.length > 0 ? 50 : 0) * allListeners.length);
		for (int i = 0; i < allListeners.length; i++) {
			final ISyncInfoSetChangeListener listener = allListeners[i];
			Platform.run(new ISafeRunnable() {
				public void handleException(Throwable exception) {
					// don't log the exception....it is already being logged in Platform#run
				}
				public void run() throws Exception {
					try {
						lockedForModification = true;
						if (event.isReset()) {
							listener.syncInfoSetReset(SyncInfoSet.this, Policy.subMonitorFor(monitor, 100));
						} else {
							listener.syncInfoChanged(event, Policy.subMonitorFor(monitor, 100));
						}
						if (newErrors.length > 0) {
							listener.syncInfoSetErrors(SyncInfoSet.this, newErrors, Policy.subMonitorFor(monitor, 50));
						}
					} finally {
						lockedForModification = false;
					}
				}
			});
		}
		monitor.done();
	}

	/**
	 * Return a copy of all the listeners registered with this set
	 * @return the listeners
	 */
	protected ISyncInfoSetChangeListener[] getListeners() {
		ISyncInfoSetChangeListener[] allListeners;
		synchronized(listeners) {
			allListeners = (ISyncInfoSetChangeListener[]) listeners.toArray(new ISyncInfoSetChangeListener[listeners.size()]);
		}
		return allListeners;
	}

	/**
	 * Return the change event that is accumulating the changes to the set.
	 * This can be called by sublasses to access the event.
	 * @return Returns the changes.
	 */
	protected SyncSetChangedEvent getChangeEvent() {
		return changes;
	}
	
	/**
	 * Add the error to the set. Errors should be added to the set when the client 
	 * populating the set cannot determine the <code>SyncInfo</code> for one
	 * or more resources due to an exception or some other problem. Listeners
	 * will be notified that an error occurred and can react accordingly.
	 * <p>
	 * Only one error can be associated with a resource (which is obtained from
	 * the <code>ITeamStatus</code>). It is up to the
	 * client populating the set to ensure that the error associated with a
	 * resource contains all relevent information.
	 * The error will remain in the set until the set is reset.
	 * </p>
	 * @param resource the resource associated with the error or the workspace root
	 * @param status the status that describes the error that occurred.
	 */
	public void addError(ITeamStatus status) {
		try {
			beginInput();
			errors.put(status.getResource(), status);
			getChangeEvent().errorOccurred(status);
		} finally {
			endInput(null);
		}
	}
	
	/**
	 * Return an array of the errors the occurred while populating this set.
	 * The errors will remain with the set until it is reset.
	 * 
	 * @return the errors
	 */
	public ITeamStatus[] getErrors() {
		return (ITeamStatus[]) errors.values().toArray(new ITeamStatus[errors.size()]);
	}

    /**
     * Return an interator over all <code>SyncInfo</code>
     * contained in this set.
     * @return an interator over all <code>SyncInfo</code>
     * contained in this set.
     * @since 3.1
     */
    public Iterator iterator() {
        return resources.values().iterator();
    }
}
