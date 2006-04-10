/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshElementSelectionPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.navigator.*;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class ModelElementSelectionPage extends GlobalRefreshElementSelectionPage implements INavigatorContentServiceListener {
	
	private INavigatorContentService service;
	private ISynchronizationScopeManager manager;

	public ModelElementSelectionPage(IResource[] roots) {
		super("elementSelection"); //$NON-NLS-1$
		setDescription(TeamUIMessages.GlobalRefreshResourceSelectionPage_2); 
		setTitle(TeamUIMessages.GlobalRefreshResourceSelectionPage_3);
		List result = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			result.add(Utils.getResourceMapping(resource));
		}
		manager = new SynchronizationScopeManager(TeamUIMessages.ModelElementSelectionPage_0, (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]), 
						ResourceMappingContext.LOCAL_CONTEXT, true);
		try {
			// TODO
			manager.initialize(new NullProgressMonitor());
		} catch (CoreException e) {
			Utils.handleError(getShell(), e, null, null);
		}
	}

	protected ContainerCheckedTreeViewer createViewer(Composite top) {
		GridData data;
		ContainerCheckedTreeViewer fViewer = new ContainerCheckedTreeViewer(top, SWT.BORDER);
		service = NavigatorContentServiceFactory.INSTANCE.createContentService(CommonViewerAdvisor.TEAM_NAVIGATOR_CONTENT, fViewer);
		service.bindExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(manager.getScope()), true);
		service.getActivationService().activateExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(manager.getScope()), true);
		service.addListener(this);
		data = new GridData(GridData.FILL_BOTH);
		//data.widthHint = 200;
		data.heightHint = 100;
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(service.createCommonContentProvider());
		fViewer.setLabelProvider(new DecoratingLabelProvider(service.createCommonLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateOKStatus();
			}
		});
		fViewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
		fViewer.setInput(manager.getScope());
		return fViewer;
	}
	
	public ResourceMapping[] getSelectedMappings() {
		if (isWorkingSetSelected()) {
			List result = new ArrayList();
			IWorkingSet[] sets = getWorkingSets();
			for (int i = 0; i < sets.length; i++) {
				IWorkingSet set = sets[i];
				result.add(Utils.getResourceMapping(set));
			}
			return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
		}
		if (isWorkspaceSelected()) {
			try {
				ModelProvider provider = ModelProvider.getModelProviderDescriptor(ModelProvider.RESOURCE_MODEL_PROVIDER_ID).getModelProvider();
				ResourceMapping mapping = Utils.getResourceMapping(provider);
				if (mapping != null) {
					return new ResourceMapping[] {mapping };
				}
			} catch (CoreException e) {
				// Shouldn't happen
				TeamUIPlugin.log(e);
			}
			ResourceMapping[] mappings = manager.getScope().getMappings(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
			return mappings;
		}
		List result = new ArrayList();
		Object[] objects = getRootElement();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			ResourceMapping mapping = Utils.getResourceMapping(object);
			if (mapping != null) {
				result.add(mapping);
			}
		}
		return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
	}
	
	public void dispose() {
		service.dispose();
		super.dispose();
	}

	protected void checkAll() {
		getViewer().setCheckedElements(manager.getScope().getModelProviders());
	}

	protected void checkWorkingSetElements() {
		// TODO Auto-generated method stub
		
	}

	public void onLoad(INavigatorContentExtension anExtension) {
		anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE, manager.getScope());
	}

}
