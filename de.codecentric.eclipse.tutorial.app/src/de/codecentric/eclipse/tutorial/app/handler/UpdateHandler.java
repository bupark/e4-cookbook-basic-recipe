package de.codecentric.eclipse.tutorial.app.handler;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class UpdateHandler {
	
	@Execute
	public void execute(IProvisioningAgent agent, UISynchronize sync, IWorkbench workbench) {
		
		// update using a progress monitor
        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException,
                    InterruptedException {
                
                update(agent, monitor, sync, workbench);
            }
        };
        
        try {
            new ProgressMonitorDialog(null).run(true, true, runnable);
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
        }
	}
	
	
	private IStatus update(IProvisioningAgent agent, IProgressMonitor monitor, UISynchronize sync, IWorkbench workbench) {
		ProvisioningSession session = new ProvisioningSession(agent);
		// update the whole running profile, otherwise specify IUs
		UpdateOperation operation = new UpdateOperation(session);
		
		SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
        
        //check if updates are available
        IStatus status = operation.resolveModal(sub.newChild(100));
        if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
            showMessage(sync, "Nothing to update");
            return Status.CANCEL_STATUS;
        }
        else {
        	ProvisioningJob provisioningJob = operation.getProvisioningJob(sub.newChild(100));
        	if (provisioningJob != null) {
        		sync.syncExec(new Runnable() {
                    
                    @Override
                    public void run() {
                        boolean performUpdate = MessageDialog.openQuestion(null,
                                "Updates available",
                                "There are updates available. Do you want to install them now?");
                        if (performUpdate) {
                            IStatus updateStatus = provisioningJob.runModal(sub.newChild(100));
                            if (updateStatus.isOK()) {
                                boolean restart = MessageDialog.openQuestion(null,
                                        "Updates installed, restart?",
                                        "Updates have been installed successfully, do you want to restart?");
                                if (restart) {
                                	workbench.restart();
                                }
                            }
                        }
                    }
                });
        	}
        	else {
                if (operation.hasResolved()) {
                    showError(sync, "Couldn't get provisioning job: " + operation.getResolutionResult());
                }
                else {
                    showError(sync, "Couldn't resolve provisioning job");
                }
                return Status.CANCEL_STATUS;
        	}
        }
        

        return Status.OK_STATUS;
	}
		
    private void showMessage(UISynchronize sync, final String message) {
        // as the provision needs to be executed in a background thread
        // we need to ensure that the message dialog is executed in 
        // the UI thread
        sync.syncExec(new Runnable() {
            
            @Override
            public void run() {
                MessageDialog.openInformation(null, "Information", message);
            }
        });
    }
    
    private void showError(UISynchronize sync, final String message) {
        // as the provision needs to be executed in a background thread
        // we need to ensure that the message dialog is executed in 
        // the UI thread
        sync.syncExec(new Runnable() {
            
            @Override
            public void run() {
                MessageDialog.openError(null, "Error", message);
            }
        });
    }
}
