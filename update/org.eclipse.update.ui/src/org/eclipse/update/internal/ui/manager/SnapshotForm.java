package org.eclipse.update.internal.ui.manager;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.update.core.*;
import org.eclipse.update.ui.internal.model.*;
import org.eclipse.swt.custom.BusyIndicator;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import java.util.Date;

public class SnapshotForm extends PropertyWebForm {
	private IInstallConfiguration currentConfiguration;
	private Label dateLabel;
	private Label currentLabel;
	private ActivitySection activitySection;
	
public SnapshotForm(UpdateFormPage page) {
	super(page);
}

public void dispose() {
	super.dispose();
}

public void initialize(Object modelObject) {
	setHeadingText("Snapshot Page");
	setHeadingImage(UpdateUIPluginImages.get(UpdateUIPluginImages.IMG_FORM_BANNER));
	setHeadingUnderlineImage(UpdateUIPluginImages.get(UpdateUIPluginImages.IMG_FORM_UNDERLINE));
	super.initialize(modelObject);
}

protected void createContents(Composite parent) {
	HTMLTableLayout layout = new HTMLTableLayout();
	parent.setLayout(layout);
	layout.leftMargin = layout.rightMargin = 10;
	layout.topMargin = 10;
	layout.horizontalSpacing = 0;
	layout.verticalSpacing = 0;
	layout.numColumns = 1;
	
	FormWidgetFactory factory = getFactory();
	
	dateLabel = createProperty(parent, "Created On");
	currentLabel = createProperty(parent, "\nCurrent Configuration");
	factory.createLabel(parent,null);
	
	activitySection = new ActivitySection((UpdateFormPage)getPage());
	Control control = activitySection.createControl(parent, factory);
	TableData td = new TableData();
	td.align = TableData.FILL;
	td.valign = TableData.TOP;
	//td.colspan = 2;
	control.setLayoutData(td);
	
	registerSection(activitySection);
}

protected Object createPropertyLayoutData() {
	TableData td = new TableData();
	td.indent = 10;
	return td;
}

public void expandTo(Object obj) {
	if (obj instanceof IInstallConfiguration) {
		inputChanged((IInstallConfiguration)obj);
	}
}

private void inputChanged(IInstallConfiguration configuration) {
	setHeadingText(configuration.getLabel());
	Date date = configuration.getCreationDate();
	dateLabel.setText(date.toString());
	String isCurrent = configuration.isCurrent()?"Yes": "No";
	currentLabel.setText(isCurrent);
	
	// reflow
	dateLabel.getParent().layout();
	activitySection.configurationChanged(configuration);
	((Composite)getControl()).layout();
	getControl().redraw();
	currentConfiguration = configuration;
}

}