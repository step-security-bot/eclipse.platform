/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.model;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.xml.sax.Attributes;

public class AntImportNode extends AntTaskNode {
	
	private String fFile= null;
	
	public AntImportNode(Task task, Attributes attributes) {
		super(task);
         fFile= attributes.getValue(IAntModelConstants.ATTR_FILE);
	}
	
	public String getFile() {
		return fFile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.model.AntElementNode#getBaseImageDescriptor()
	 */
	protected ImageDescriptor getBaseImageDescriptor() {
		return AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_IMPORT);
	}
	
	public String getLabel() {
		StringBuffer label= new StringBuffer(getTask().getTaskName());
		label.append(' ');
		label.append(fFile);
		
		if (isExternal()) {
			appendEntityName(label);
		}
		return label.toString();
	}

	/**
	 * Execute the import.
	 * Returns <code>true</code> as the import adds to the Ant model
	 */
	public boolean configure(boolean validateFully) {
		if (configured) {
			return false;
		}
		try {
			getTask().maybeConfigure();
			getTask().execute();
			configured= true;
			return true;
		} catch (BuildException be) {
			handleBuildException(be, AntEditorPreferenceConstants.PROBLEM_IMPORTS);
		}
		return false;
	}
	
	public IFile getIFile() {
		IFile file;
		if (isExternal()) {
			file= AntUtil.getFileForLocation(getFilePath(), null);
		} else {
			String path= getFile();
			file= AntUtil.getFileForLocation(path, getAntModel().getEditedFile().getParentFile());
		}
		return file;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getReferencedElement(int)
	 */
	public String getReferencedElement(int offset) {
		if (fFile != null) {
			String textToSearch= getAntModel().getText(getOffset(), offset - getOffset());
			String attributeString = AntEditorCompletionProcessor.getAttributeStringFromDocumentStringToPrefix(textToSearch);
			if ("file".equals(attributeString)) {  //$NON-NLS-1$//$NON-NLS-2$
				return fFile;
			}
        }
        return null;
	}
}