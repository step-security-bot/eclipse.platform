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
package org.eclipse.update.core.model;

import java.net.*;

/**
 * Site archive model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.ArchiveReference
 * @since 2.0
 */
public class ArchiveReferenceModel extends ModelObject {

	private String path;
	private String urlString;
	private URL url;

	/**
	 * Creates a uninitialized model object.
	 *  
	 * @since 2.0
	 */
	public ArchiveReferenceModel() {
		super();
	}

	/**
	 * Retrieve the site archive "symbolic" path
	 *
	 * @return path, or <code>null</code>
	 * @since 2.0
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the unresolved URL string for the archive.
	 *
	 * @return url string, or <code>null</code>
	 * @since 2.0
	 */
	public String getURLString() {
		return urlString;
	}

	/**
	 * Returns the resolved URL for the archive.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Sets the site archive "symbolic" path.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param path archive "symbolic" path.
	 * @since 2.0
	 */
	public void setPath(String path) {
		assertIsWriteable();
		this.path = path;
	}

	/**
	 * Sets the unresolved URL string for the archive.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param urlString unresolved url string.
	 * @since 2.0
	 */
	public void setURLString(String urlString) {
		assertIsWriteable();
		this.urlString = urlString;
		this.url = null;
	}

	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundleURL resource bundle URL
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base,URL bundleURL)
		throws MalformedURLException {
		// resolve local elements
		url = resolveURL(base, bundleURL, urlString);
	}
}