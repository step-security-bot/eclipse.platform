/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.intro.impl.model;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Bundle convenience methods.
 */
public class BundleUtil {

    private static String NL_TAG = "$nl$/"; //$NON-NLS-1$


    /**
     * Utility method to validate the state of a bundle. Log invalid bundles to
     * log file.
     */
    public static boolean bundleHasValidState(Bundle bundle) {
        if (bundle == null || bundle.getState() == Bundle.UNINSTALLED
                || bundle.getState() == Bundle.INSTALLED) {

            if (bundle == null)
                Log.error("Intro tried accessing a NULL bundle.", null); //$NON-NLS-1$
            else {
                String msg = StringUtil
                    .concat("Intro tried accessing Bundle: ", getBundleHeader( //$NON-NLS-1$
                        bundle, Constants.BUNDLE_NAME), " vendor: ", //$NON-NLS-1$
                        getBundleHeader(bundle, Constants.BUNDLE_VENDOR),
                        " bundle state: ", String.valueOf(bundle.getState())).toString(); //$NON-NLS-1$
                Log.error(msg, null);
            }
            return false;
        }

        return true;
    }

    /**
     * Retrieves the given key from the bundle header.
     * 
     * @param bundle
     * @param key
     * @return
     */
    public static String getBundleHeader(Bundle bundle, String key) {
        return (String) bundle.getHeaders().get(key);
    }


    public static Bundle getBundleFromConfigurationElement(
            IConfigurationElement cfg) {
        return Platform.getBundle(cfg.getNamespace());
    }


    /**
     * Get the resourcelocation, but do not force an $nl$ on it.
     * 
     * @param resource
     * @param element
     * @return
     */
    public static String getResourceLocation(String resource,
            IConfigurationElement element) {
        Bundle bundle = getBundleFromConfigurationElement(element);
        return getResolvedResourceLocation(resource, bundle, false);
    }



    /**
     * Returns the fully qualified location of the passed resource string from
     * the declaring plugin. If the plugin is not defined, or file could not be
     * loaded from the plugin, the resource is returned as is.
     * 
     * @param resource
     * @return
     */
    public static String getResolvedResourceLocation(String resource,
            IConfigurationElement element) {
        Bundle bundle = getBundleFromConfigurationElement(element);
        return getResolvedResourceLocation(resource, bundle, true);
    }


    /**
     * Returns the fully qualified location of the passed resource string from
     * the passed plugin id. If the file could not be loaded from the plugin,
     * the resource is returned as is.
     * 
     * @param resource
     * @return
     */
    public static String getResolvedResourceLocation(String resource,
            String pluginId) {
        Bundle bundle = Platform.getBundle(pluginId);
        return getResolvedResourceLocation(resource, bundle, true);
    }


    /**
     * Shorthand util method.
     * 
     * @param resource
     * @return
     */
    public static String getResolvedResourceLocation(String resource,
            Bundle bundle) {
        return getResolvedResourceLocation(resource, bundle, true);
    }

    public static String getResolvedResourceLocation(String base,
            String resource, Bundle bundle) {
        // quick exits.
        if (resource == null)
            return null;

        String fullResource = new Path(base).append(resource).toString();
        return getResolvedResourceLocation(fullResource, bundle, true);
    }

    public static String getResolvedResourceLocation(String resource,
            Bundle bundle, boolean forceNLResolve) {
        // quick exits.
        if (resource == null)
            return null;

        if (bundle == null || !bundleHasValidState(bundle))
            return resource;

        URL localLocation = null;
        try {
            // we need to resolve this URL.
            String copyResource = resource;
            if (forceNLResolve && !copyResource.startsWith(NL_TAG)) {
                if (copyResource.startsWith("/")
                        || copyResource.startsWith("\\"))
                    copyResource = resource.substring(1);
                copyResource = NL_TAG + copyResource;
            }
            IPath resourcePath = new Path(copyResource);
            localLocation = Platform.find(bundle, resourcePath);
            if (localLocation == null) {
                // localLocation can be null if the passed resource could not
                // be found relative to the plugin. log fact, return resource,
                // as is.
                String msg = StringUtil.concat("Could not find resource: ", //$NON-NLS-1$
                    resource, " in ", getBundleHeader( //$NON-NLS-1$
                        bundle, Constants.BUNDLE_NAME)).toString();
                Log.warning(msg);
                return resource;
            }
            localLocation = Platform.asLocalURL(localLocation);
            return localLocation.toExternalForm();
        } catch (Exception e) {
            String msg = StringUtil.concat("Failed to load resource: ", //$NON-NLS-1$
                resource, " from ", getBundleHeader(bundle, //$NON-NLS-1$
                    Constants.BUNDLE_NAME)).toString();
            Log.error(msg, e);
            return resource;
        }
    }




    /** *** used by Intro parser ***** */
    /*
     * Uti method to return an URL to a plugin relative resource.
     */
    public static URL getResourceAsURL(String resource, String pluginId) {
        Bundle bundle = Platform.getBundle(pluginId);
        URL localLocation = localLocation = Platform.find(bundle, new Path(
            resource));
        return localLocation;
    }




    /** ********************* Used by HTML generator ****************** */
    /**
     * Get the absolute path of the given bundle, in the form
     * file:/path_to_plugin
     * 
     * @param bundle
     * @return
     */
    public static String getResolvedBundleLocation(Bundle bundle) {
        try {
            URL bundleLocation = bundle.getEntry(""); //$NON-NLS-1$
            if (bundleLocation == null)
                return null;
            bundleLocation = Platform.asLocalURL(bundleLocation);
            return bundleLocation.toExternalForm();
        } catch (IllegalStateException e) {
            Log.error("Failed to access bundle: " //$NON-NLS-1$
                    + bundle.getSymbolicName(), e);
            return null;
        } catch (IOException e) {
            Log.error("Failed to resolve URL path for bundle: " //$NON-NLS-1$
                    + bundle.getSymbolicName(), e);
            return null;
        }
    }

    /**
     * Get the absolute path of the bundle with id <code>bundleId</code>. If
     * no such bundle is found, return null.
     * 
     * @param bundleId
     * @return
     */
    public static String getResolvedBundleLocation(String bundleId) {
        Bundle bundle = Platform.getBundle(bundleId);
        if (bundle == null)
            return null;
        return getResolvedBundleLocation(bundle);
    }

}
