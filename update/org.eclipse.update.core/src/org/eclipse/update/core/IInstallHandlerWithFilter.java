/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

/**
 * Custom install handler.
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2. This
 * provisional API was never promoted to stable API, and may be removed from a future release of the platform.
 */
public interface IInstallHandlerWithFilter extends IInstallHandler{

		public boolean acceptNonPluginData(INonPluginEntry data);
		
}