package org.eclipse.update.internal.ui.manager;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;

import java.net.URL;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.custom.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.jface.wizard.*;

import java.text.MessageFormat;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.dialogs.MessageDialog;

public class DetailsForm extends PropertyWebForm {
	// NL keys

	private static final String KEY_PROVIDER = "FeaturePage.provider";
	private static final String KEY_VERSION = "FeaturePage.version";
	private static final String KEY_IVERSION = "FeaturePage.installedVersion";
	private static final String KEY_PENDING_VERSION = "FeaturePage.pendingVersion";
	private static final String KEY_SIZE = "FeaturePage.size";
	private static final String KEY_OS = "FeaturePage.os";
	private static final String KEY_WS = "FeaturePage.ws";
	private static final String KEY_NL = "FeaturePage.nl";
	private static final String KEY_PLATFORMS = "FeaturePage.platforms";
	private static final String KEY_DESC = "FeaturePage.description";
	private static final String KEY_INFO_LINK = "FeaturePage.infoLink";
	private static final String KEY_LICENSE_LINK = "FeaturePage.licenseLink";
	private static final String KEY_COPYRIGHT_LINK = "FeaturePage.copyrightLink";
	private static final String KEY_NOT_INSTALLED = "FeaturePage.notInstalled";
	private static final String KEY_SIZE_VALUE = "FeaturePage.sizeValue";
	private static final String KEY_UNKNOWN_SIZE_VALUE =
		"FeaturePage.unknownSizeValue";
	private static final String KEY_DO_UNCONFIGURE =
		"FeaturePage.doButton.unconfigure";
	private static final String KEY_DO_CONFIGURE = "FeaturePage.doButton.configure";
	private static final String KEY_DO_UPDATE = "FeaturePage.doButton.update";
	private static final String KEY_DO_INSTALL = "FeaturePage.doButton.install";
	private static final String KEY_DO_UNINSTALL = "FeaturePage.doButton.uninstall";
	private static final String KEY_OS_WIN32 = "FeaturePage.os.win32";
	private static final String KEY_OS_LINUX = "FeaturePage.os.linux";
	private static final String KEY_WS_WIN32 = "FeaturePage.ws.win32";
	private static final String KEY_WS_MOTIF = "FeaturePage.ws.motif";
	private static final String KEY_WS_GTK = "FeaturePage.ws.gtk";
	private static final String KEY_DIALOG_UTITLE = "FeaturePage.dialog.utitle";
	private static final String KEY_DIALOG_TITLE = "FeaturePage.dialog.title";
	private static final String KEY_DIALOG_CTITLE = "FeaturePage.dialog.ctitle";
	private static final String KEY_DIALOG_UCTITLE = "FeaturePage.dialog.uctitle";
	private static final String KEY_DIALOG_UMESSAGE = "FeaturePage.dialog.umessage";
	private static final String KEY_DIALOG_MESSAGE = "FeaturePage.dialog.message";
	private static final String KEY_DIALOG_CMESSAGE = "FeaturePage.dialog.cmessage";
	private static final String KEY_DIALOG_UCMESSAGE = "FeaturePage.dialog.ucmessage";
	//	

	private Label imageLabel;
	private Label providerLabel;
	private Label versionLabel;
	private Label installedVersionLabel;
	private Label sizeLabel;
	private Label osLabel;
	private Label wsLabel;
	private Label nlLabel;
	private Label descriptionText;
	private URL infoLinkURL;
	private SelectableFormLabel infoLinkLabel;
	private InfoLink licenseLink;
	private InfoLink copyrightLink;
	private ReflowGroup supportedPlatformsGroup;
	private Image providerImage;
	private Button uninstallButton;
	private Button doButton;
	private IFeature currentFeature;
	private IFeatureAdapter currentAdapter;
	private ModelListener modelListener;
	private Hashtable imageCache = new Hashtable();
	private HyperlinkHandler sectionHandler;
	private boolean alreadyInstalled;
	private IFeature[] installedFeatures;
	private boolean newerVersion;

	class ModelListener implements IUpdateModelChangedListener {
		/**
		 * @see IUpdateModelChangedListener#objectAdded(Object, Object)
		 */
		public void objectAdded(Object parent, Object child) {
			if (child instanceof PendingChange) {
				PendingChange job = (PendingChange) child;
				if (job.getFeature().equals(currentFeature)) {
					refresh();
				}
			}
		}

		/**
		 * @see IUpdateModelChangedListener#objectRemoved(Object, Object)
		 */
		public void objectRemoved(Object parent, Object child) {
			if (child instanceof PendingChange) {
				PendingChange job = (PendingChange) child;
				if (job.getFeature().equals(currentFeature)) {
					doButton.setEnabled(true);
				}
			}
		}

		/**
		 * @see IUpdateModelChangedListener#objectChanged(Object, String)
		 */
		public void objectChanged(Object object, String property) {
		}
	}

	abstract class LinkListener implements IHyperlinkListener {
		public abstract URL getURL();
		public void linkActivated(Control linkLabel) {
			URL url = getURL();
			if (url != null)
				openURL(url.toString());
		}
		public void linkEntered(Control linkLabel) {
			URL url = getURL();
			if (url != null)
				showStatus(url.toString());
		}
		public void linkExited(Control linkLabel) {
			showStatus(null);
		}

		private void showStatus(String text) {
			IViewSite site = getPage().getView().getViewSite();
			IStatusLineManager sm = site.getActionBars().getStatusLineManager();
			sm.setMessage(text);
		}
	}

	abstract class ReflowGroup extends ExpandableGroup {
		public void expanded() {
			reflow();
			updateSize();
		}
		public void collapsed() {
			reflow();
			updateSize();
		}
		protected SelectableFormLabel createTextLabel(
			Composite parent,
			FormWidgetFactory factory) {
			SelectableFormLabel label = super.createTextLabel(parent, factory);
			label.setFont(JFaceResources.getBannerFont());
			return label;
		}
		protected HyperlinkHandler getHyperlinkHandler(FormWidgetFactory factory) {
			return sectionHandler;
		}
	}

	public DetailsForm(UpdateFormPage page) {
		super(page);
		providerImage = UpdateUIPluginImages.DESC_PROVIDER.createImage();
		modelListener = new ModelListener();
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addUpdateModelChangedListener(modelListener);
		sectionHandler = new HyperlinkHandler();
	}

	public void dispose() {
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(modelListener);
		providerImage.dispose();
		for (Enumeration enum = imageCache.elements(); enum.hasMoreElements();) {
			Image image = (Image) enum.nextElement();
			image.dispose();
		}
		imageCache.clear();
		sectionHandler.dispose();
		super.dispose();
	}

	public void initialize(Object modelObject) {
		setHeadingText("");
		setHeadingImage(UpdateUIPluginImages.get(UpdateUIPluginImages.IMG_FORM_BANNER));
		setHeadingUnderlineImage(
			UpdateUIPluginImages.get(UpdateUIPluginImages.IMG_FORM_UNDERLINE));
		super.initialize(modelObject);
	}

	private void configureSectionHandler(FormWidgetFactory factory) {
		sectionHandler.setHyperlinkUnderlineMode(HyperlinkHandler.UNDERLINE_NEVER);
		sectionHandler.setBackground(factory.getBackgroundColor());
		sectionHandler.setForeground(
			factory.getColor(factory.COLOR_COMPOSITE_SEPARATOR));
	}

	public void createContents(Composite container) {
		HTMLTableLayout layout = new HTMLTableLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		layout.rightMargin = 0;
		GridData gd;

		configureSectionHandler(factory);

		GridLayout glayout = new GridLayout();
		Composite properties = factory.createComposite(container);
		properties.setLayout(glayout);
		glayout.marginWidth = glayout.marginHeight = 0;
		glayout.verticalSpacing = 0;

		providerLabel =
			createProperty(properties, UpdateUIPlugin.getResourceString(KEY_PROVIDER));
		versionLabel =
			createProperty(properties, UpdateUIPlugin.getResourceString(KEY_VERSION));
		installedVersionLabel =
			createProperty(properties, UpdateUIPlugin.getResourceString(KEY_IVERSION));
		sizeLabel =
			createProperty(properties, UpdateUIPlugin.getResourceString(KEY_SIZE));
		supportedPlatformsGroup = new ReflowGroup() {
			public void fillExpansion(Composite expansion, FormWidgetFactory factory) {
				GridLayout layout = new GridLayout();
				expansion.setLayout(layout);
				layout.marginWidth = 0;
				osLabel = createProperty(expansion, UpdateUIPlugin.getResourceString(KEY_OS));
				wsLabel = createProperty(expansion, UpdateUIPlugin.getResourceString(KEY_WS));
				nlLabel = createProperty(expansion, UpdateUIPlugin.getResourceString(KEY_NL));
			}
		};
		supportedPlatformsGroup.setText(
			UpdateUIPlugin.getResourceString(KEY_PLATFORMS));
		new Label(properties, SWT.NULL);
		supportedPlatformsGroup.createControl(properties, factory);

		imageLabel = factory.createLabel(container, null);
		TableData td = new TableData();
		td.align = TableData.CENTER;
		//td.valign = TableData.MIDDLE;
		imageLabel.setLayoutData(td);

		Label label =
			createHeading(container, UpdateUIPlugin.getResourceString(KEY_DESC));
		td = new TableData();
		td.colspan = 2;
		label.setLayoutData(td);
		descriptionText = factory.createLabel(container, null, SWT.WRAP);
		td = new TableData();
		td.colspan = 2;
		td.grabHorizontal = true;
		descriptionText.setLayoutData(td);

		glayout = new GridLayout();
		glayout.numColumns = 5;
		glayout.horizontalSpacing = 20;
		glayout.marginWidth = 10;

		Composite l = factory.createCompositeSeparator(container);
		l.setBackground(factory.getBorderColor());
		td = new TableData();
		td.colspan = 2;
		td.heightHint = 1;
		td.align = TableData.FILL;
		l.setLayoutData(td);

		Composite footer = factory.createComposite(container);
		td = new TableData();
		td.colspan = 2;
		td.align = TableData.FILL;
		td.valign = TableData.FILL;
		footer.setLayoutData(td);
		footer.setLayout(glayout);

		LinkListener listener = new LinkListener() {
			public URL getURL() {
				return infoLinkURL;
			}
		};
		infoLinkLabel = new SelectableFormLabel(footer, SWT.NULL);
		infoLinkLabel.setText(UpdateUIPlugin.getResourceString(KEY_INFO_LINK));
		factory.turnIntoHyperlink(infoLinkLabel, listener);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		infoLinkLabel.setLayoutData(gd);
		licenseLink = new InfoLink((DetailsView) getPage().getView());
		licenseLink.setText(UpdateUIPlugin.getResourceString(KEY_LICENSE_LINK));
		licenseLink.createControl(footer, factory);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		licenseLink.getControl().setLayoutData(gd);
		copyrightLink = new InfoLink((DetailsView) getPage().getView());
		copyrightLink.setText(UpdateUIPlugin.getResourceString(KEY_COPYRIGHT_LINK));
		copyrightLink.createControl(footer, factory);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		copyrightLink.getControl().setLayoutData(gd);

		uninstallButton = factory.createButton(footer, "", SWT.PUSH);
		uninstallButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doUninstall();
			}
		});
		gd =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.grabExcessHorizontalSpace = true;
		uninstallButton.setVisible(false);
		uninstallButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_UNINSTALL));
		uninstallButton.setLayoutData(gd);

		doButton = factory.createButton(footer, "", SWT.PUSH);
		doButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doButtonSelected();
			}
		});
		gd =
			new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		//gd.grabExcessHorizontalSpace = true;
		doButton.setLayoutData(gd);
	}

	public void expandTo(final Object obj) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				if (obj instanceof IFeature) {
					currentAdapter = null;
					currentFeature = (IFeature) obj;
					refresh();
				} else if (obj instanceof IFeatureAdapter) {
					try {
						currentFeature = ((IFeatureAdapter) obj).getFeature();
						currentAdapter = (IFeatureAdapter) obj;
						refresh();
					} catch (CoreException e) {
						UpdateUIPlugin.logException(e);
					}
				} else {
					currentFeature = null;
					currentAdapter = null;
					refresh();
				}
			}
		});
	}

	private String getInstalledVersion(IFeature feature) {
		alreadyInstalled = false;
		VersionedIdentifier vid = feature.getVersionIdentifier();
		String id = vid.getIdentifier();
		Version version = vid.getVersion();
		newerVersion = installedFeatures.length>0;

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < installedFeatures.length; i++) {
			IFeature installedFeature = installedFeatures[i];
			VersionedIdentifier ivid = installedFeature.getVersionIdentifier();
			if (buf.length() > 0)
				buf.append(", ");
			Version iversion = ivid.getVersion();
			buf.append(iversion.toString());
			if (ivid.equals(vid)) {
				alreadyInstalled = true;
			}
			else {
				if (version.compare(iversion)<=0) newerVersion = false;
			}
		}
		if (buf.length() > 0) {
			String versionText = buf.toString();
			UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
			PendingChange change = model.findPendingChange(feature);
			if (change != null) {
				return UpdateUIPlugin.getFormattedMessage(KEY_PENDING_VERSION, versionText);
			} else
				return versionText;
		} else
			return null;
	}

	private IFeature[] getInstalledFeatures(IFeature feature) {
		Vector features = new Vector();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfigurationSite[] isites = config.getConfigurationSites();
			VersionedIdentifier vid = feature.getVersionIdentifier();
			String id = vid.getIdentifier();
			Version version = vid.getVersion();

			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < isites.length; i++) {
				IConfigurationSite isite = isites[i];
				IFeature[] result = UpdateUIPlugin.searchSite(id, isite, true);
				for (int j = 0; j < result.length; j++) {
					IFeature installedFeature = result[j];
					features.add(installedFeature);
				}
			}
		} catch (CoreException e) {
		}
		return (IFeature[]) features.toArray(new IFeature[features.size()]);
	}

	private void refresh() {
		IFeature feature = currentFeature;

		if (feature == null)
			return;

		installedFeatures = getInstalledFeatures(feature);

		setHeadingText(feature.getLabel());
		providerLabel.setText(feature.getProvider());
		versionLabel.setText(feature.getVersionIdentifier().getVersion().toString());
		String installedVersion = getInstalledVersion(feature);
		if (installedVersion == null)
			installedVersion = UpdateUIPlugin.getResourceString(KEY_NOT_INSTALLED);
		installedVersionLabel.setText(installedVersion);
		long size = feature.getInstallSize((ISite) null);
		String format = null;
		if (size != -1) {
			String stext = Long.toString(size);
			String pattern = UpdateUIPlugin.getResourceString(KEY_SIZE_VALUE);
			format = UpdateUIPlugin.getFormattedMessage(pattern, stext);
		} else {
			format = UpdateUIPlugin.getResourceString(KEY_UNKNOWN_SIZE_VALUE);
		}
		sizeLabel.setText(format);
		if (feature.getDescription()!=null)
			descriptionText.setText(feature.getDescription().getAnnotation());
		else
			descriptionText.setText("");
		Image logoImage = loadProviderImage(feature);
		if (logoImage == null)
			logoImage = providerImage;
		imageLabel.setImage(logoImage);
		infoLinkURL = null;
		if (feature.getDescription()!=null)
		   infoLinkURL = feature.getDescription().getURL();
		infoLinkLabel.setVisible(infoLinkURL != null);

		setOS(feature.getOS());
		setWS(feature.getWS());
		setNL(feature.getNL());

		licenseLink.setInfo(feature.getLicense());
		copyrightLink.setInfo(feature.getCopyright());
		doButton.setVisible(getDoButtonVisibility());
		uninstallButton.setVisible(getUninstallButtonVisibility());
		if (doButton.isVisible())
			updateButtonText(newerVersion);
		reflow();
		updateSize();
		((Composite) getControl()).redraw();
	}

	private boolean getDoButtonVisibility() {
		if (currentFeature instanceof MissingFeature) return false;
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		if (model.isPending(currentFeature))
			return false;
		if (currentAdapter instanceof IConfigurationSiteContext) {
			// part of the local configuration
			IConfigurationSiteContext context = (IConfigurationSiteContext)currentAdapter;
			if (!context.getInstallConfiguration().isCurrent())
				return false;
			else
				return true;
		}
		// Random site feature
		if (alreadyInstalled)
			return false;
		// Not installed - check if there are other 
		// features with this ID that are installed
		// and that are newer than this one
		if (installedFeatures.length>0 && !newerVersion)
			return false;
		return true;
	}
	
	private boolean getUninstallButtonVisibility() {
		if (currentFeature instanceof MissingFeature) return false;
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		if (model.isPending(currentFeature))
			return false;
		if (currentAdapter instanceof IConfigurationSiteContext) {
			boolean configured = isConfigured();
			return !configured;
		}
		return false;
	}

	private boolean isConfigured() {
		if (currentAdapter instanceof IConfigurationSiteContext) {
			IConfigurationSiteContext context = (IConfigurationSiteContext) currentAdapter;
			IConfigurationSite csite = context.getConfigurationSite();
			IFeatureReference fref = csite.getSite().getFeatureReference(currentFeature);
			IFeatureReference[] cfeatures = csite.getConfiguredFeatures();
			for (int i = 0; i < cfeatures.length; i++) {
				if (cfeatures[i].equals(fref))
					return true;
			}
		}
		return false;
	}

	private void updateButtonText(boolean update) {
		if (currentAdapter instanceof IConfigurationSiteContext) {
			boolean configured = isConfigured();
			if (configured)
				doButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_UNCONFIGURE));
			else
				doButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_CONFIGURE));
		} else if (update) {
			doButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_UPDATE));
		} else
			doButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_INSTALL));
	}

	private Image loadProviderImage(IFeature feature) {
		Image image = null;
		URL imageURL = feature.getImage();
		if (imageURL == null)
			return null;
		// check table
		image = (Image) imageCache.get(imageURL);
		if (image == null) {
			ImageDescriptor id = ImageDescriptor.createFromURL(imageURL);
			image = id.createImage();
			if (image != null)
				imageCache.put(imageURL, image);
		}
		return image;
	}

	private void reflow() {
		versionLabel.getParent().layout(true);
		doButton.getParent().layout(true);
		imageLabel.getParent().layout(true);
		((Composite) getControl()).layout(true);
	}

	private void setOS(String os) {
		if (os == null)
			osLabel.setText("");
		else {
			String[] array = getTokens(os);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapOS(array[i]));
			}
			osLabel.setText(buf.toString());
		}
	}

	private String mapOS(String key) {
		if (key.equals("OS_WIN32"))
			return UpdateUIPlugin.getResourceString(KEY_OS_WIN32);
		if (key.equals("OS_LINUX"))
			return UpdateUIPlugin.getResourceString(KEY_OS_LINUX);
		return key;
	}

	private String mapWS(String key) {
		if (key.equals("WS_WIN32"))
			return UpdateUIPlugin.getResourceString(KEY_WS_WIN32);
		if (key.equals("WS_MOTIF"))
			return UpdateUIPlugin.getResourceString(KEY_WS_MOTIF);
		if (key.equals("WS_GTK"))
			return UpdateUIPlugin.getResourceString(KEY_WS_GTK);
		return key;
	}

	private String mapNL(String nl) {
		String language, country;

		int loc = nl.indexOf('_');
		if (loc != -1) {
			language = nl.substring(0, loc);
			country = nl.substring(loc + 1);
		} else {
			language = nl;
			country = "";
		}
		Locale locale = new Locale(language, country);
		return locale.getDisplayName();
	}

	private void setWS(String ws) {
		if (ws == null)
			wsLabel.setText("");
		else {
			String[] array = getTokens(ws);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapWS(array[i]));
			}
			wsLabel.setText(buf.toString());
		}
	}

	private void setNL(String nl) {
		if (nl == null)
			nlLabel.setText("");
		else {
			String[] array = getTokens(nl);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapNL(array[i]));
			}
			nlLabel.setText(buf.toString());
		}
	}

	private String[] getTokens(String source) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(source, ",");
		while (stok.hasMoreTokens()) {
			String tok = stok.nextToken();
			result.add(tok);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	private void openURL(final String url) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				DetailsView dv = (DetailsView) getPage().getView();
				dv.showURL(url);
			}
		});
	}
	
	private void doUninstall() {
		executeJob(PendingChange.UNINSTALL);
	}

	private void executeJob(int mode) {
		if (currentFeature != null) {
			final PendingChange job = createPendingChange(mode);
			BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
				public void run() {
					InstallWizard wizard = new InstallWizard(job);
					WizardDialog dialog =
						new InstallWizardDialog(UpdateUIPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					dialog.getShell().setSize(500, 500);
					dialog.open();
					if (wizard.isSuccessfulInstall())
						showRestartMessage(job);
				}
			});
		}
	}
	
	private void doButtonSelected() {
		if (currentFeature != null) {
			int mode;
			if (currentAdapter instanceof IConfigurationSiteContext) {
				boolean configured = isConfigured();
				if (configured)
					mode = PendingChange.UNCONFIGURE;
				else
					mode = PendingChange.CONFIGURE;
			} else {
				mode = PendingChange.INSTALL;
			}
			executeJob(mode);
		}
	}

	private PendingChange createPendingChange(int type) {
		if (type == PendingChange.INSTALL && installedFeatures.length > 0) {
			return new PendingChange(installedFeatures[0], currentFeature);
		} else {
			return new PendingChange(currentFeature, type);
		}
	}

	private void showRestartMessage(PendingChange job) {
		String titleKey;
		String messageKey;

		switch (job.getJobType()) {
			case PendingChange.INSTALL :
				titleKey = KEY_DIALOG_TITLE;
				messageKey = KEY_DIALOG_MESSAGE;
				break;
			case PendingChange.CONFIGURE :
				titleKey = KEY_DIALOG_CTITLE;
				messageKey = KEY_DIALOG_CMESSAGE;
				break;
			case PendingChange.UNCONFIGURE :
				titleKey = KEY_DIALOG_UCTITLE;
				messageKey = KEY_DIALOG_UCMESSAGE;
				break;
			case PendingChange.UNINSTALL :
				titleKey = KEY_DIALOG_UTITLE;
				messageKey = KEY_DIALOG_UMESSAGE;
				break;
			default :
				return;
		}
		String title = UpdateUIPlugin.getResourceString(titleKey);
		String message = UpdateUIPlugin.getResourceString(messageKey);
		MessageDialog.openInformation(
			UpdateUIPlugin.getActiveWorkbenchShell(),
			title,
			message);
	}
}