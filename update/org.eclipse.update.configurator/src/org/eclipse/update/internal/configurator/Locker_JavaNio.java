/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

/**
 * Internal class.
 */
public class Locker_JavaNio implements Locker {
	private File lockFile;
	private RandomAccessFile raf;
	private FileLock fileLock;

	public Locker_JavaNio(File lockFile) {
		this.lockFile = lockFile;
	}

	public synchronized boolean lock() throws IOException {
		raf = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
		try{
			fileLock = raf.getChannel().tryLock();
		} catch(OverlappingFileLockException e) {
			fileLock = null;
		} finally {
			if (fileLock != null)
				return true;
			raf.close();
			raf = null;
		}
		return false;
	}

	public synchronized void release() {
		if (fileLock != null) {
			try {
				fileLock.release();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileLock = null;
		}
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			raf = null;
		}
		if (lockFile != null) {
			lockFile.delete();
			lockFile = null;
		}
	}
}
