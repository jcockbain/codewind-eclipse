/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.util.List;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ImagePushRegistryInfo;
import org.eclipse.codewind.core.internal.connection.RegistryInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RegistryManagementDialog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class ManageRegistriesAction extends SelectionProviderAction {

	protected CodewindConnection connection;
	
	public ManageRegistriesAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.RegMgmtActionLabel);
		setImageDescriptor(CodewindUIPlugin.getDefaultIcon());
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindConnection) {
				connection = (CodewindConnection)obj;
				setEnabled(connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			Logger.logError("ManageRegistriesAction ran but no Codewind connection was selected"); //$NON-NLS-1$
			return;
		}
		try {
			List<RegistryInfo> regList = connection.requestRegistryList();
			ImagePushRegistryInfo pushReg = connection.requestGetPushRegistry();
			RegistryManagementDialog regDialog = new RegistryManagementDialog(Display.getDefault().getActiveShell(), connection, regList, pushReg);
			if (regDialog.open() == Window.OK && regDialog.hasChanges()) {
				Job job = new Job(Messages.RegUpdateTask) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						return regDialog.updateRegistries(monitor);
					}
				};
				job.schedule();
			}
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.RegListErrorTitle, NLS.bind(Messages.RegListErrorMsg, e));
		}
	}
}
