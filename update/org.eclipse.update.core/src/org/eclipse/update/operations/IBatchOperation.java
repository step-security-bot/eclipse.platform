/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.operations;

/**
 * A batch operation is needed to execute a group of feature operations, such as installing a set of features.
 * In fact, for installing features, it is recommended to wrap individual feature install operations as a 
 * batch operation, to ensure proper validation and configuration saving.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 */
public interface IBatchOperation  extends IOperation {
	/**
	 * Returns the batched operations.
	 * @return the batched operations
	 */
	public abstract IFeatureOperation[] getOperations();
}