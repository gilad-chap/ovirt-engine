package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.storage.AbstractStorageView;
import org.ovirt.engine.ui.common.widget.uicommon.storage.FcpStorageView;
import org.ovirt.engine.ui.common.widget.uicommon.storage.IscsiStorageView;
import org.ovirt.engine.ui.uicommonweb.models.storage.IStorageModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.StoragePopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Inject;

public class StoragePopupView extends AbstractModelBoundPopupView<StorageModel>
        implements StoragePopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<StorageModel, StoragePopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, StoragePopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<StoragePopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @Path(value = "name.entity")
    @WithElementId("name")
    EntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId("description")
    EntityModelTextBoxEditor descriptionEditor;

    @UiField
    @Path(value = "comment.entity")
    @WithElementId("comment")
    EntityModelTextBoxEditor commentEditor;

    @UiField(provided = true)
    @Path(value = "dataCenter.selectedItem")
    @WithElementId("dataCenter")
    ListModelListBoxEditor<Object> datacenterListEditor;

    @UiField(provided = true)
    @Path(value = "availableStorageItems.selectedItem")
    @WithElementId("availableStorageItems")
    ListModelListBoxEditor<Object> storageTypeListEditor;

    @UiField(provided = true)
    @Path(value = "format.selectedItem")
    @WithElementId("format")
    ListModelListBoxEditor<Object> formatListEditor;

    @UiField(provided = true)
    @Path(value = "host.selectedItem")
    @WithElementId("host")
    ListModelListBoxEditor<Object> hostListEditor;

    @Ignore
    @UiField
    FlowPanel specificStorageTypePanel;

    @UiField
    Image datacenterAlertIcon;

    @SuppressWarnings("rawtypes")
    @Ignore
    @WithElementId
    AbstractStorageView storageView;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public StoragePopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        asWidget().enableResizeSupport(true);
        localize(constants);
        addStyles();
        driver.initialize(this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void initListBoxEditors(final ApplicationConstants constants) {
        datacenterListEditor = new ListModelListBoxEditor<Object>(new AbstractRenderer<Object>() {
            @Override
            public String render(Object object) {
                String formattedString = ""; //$NON-NLS-1$

                if (object != null) {
                    StoragePool storage = (StoragePool) object;

                    // Get formatted storage type and format using Enum renders
                    String storageType = storage.isLocal() ?  constants.storageTypeLocal() : ""; //$NON-NLS-1$
                    String storageFormatType = storage.getStoragePoolFormatType() == null ? "" : //$NON-NLS-1$
                            (new EnumRenderer<StorageFormatType>()).render(storage.getStoragePoolFormatType());

                    // Add storage type and format if available
                    if (!storageType.isEmpty() || !storageFormatType.isEmpty()) {
                        formattedString = " ("; //$NON-NLS-1$
                        if (storage.isLocal()) {
                            formattedString += storageType;
                        }
                        else {
                            formattedString += storageFormatType;
                        }
                        formattedString += ")"; //$NON-NLS-1$
                    }

                    formattedString = storage.getName() + formattedString;
                }

                return formattedString;
            }
        });

        formatListEditor = new ListModelListBoxEditor<Object>(new EnumRenderer());

        hostListEditor = new ListModelListBoxEditor<Object>(new AbstractRenderer<Object>() {
            @Override
            public String render(Object object) {
                return object == null ? "" : ((VDS) object).getName(); //$NON-NLS-1$
            }
        });

        storageTypeListEditor = new ListModelListBoxEditor<Object>(new AbstractRenderer<Object>() {
            @Override
            public String render(Object object) {
                String formattedString = ""; //$NON-NLS-1$

                if (object != null) {
                    EnumRenderer<StorageType> storageEnumRenderer = new EnumRenderer<StorageType>();
                    EnumRenderer<StorageDomainType> storageDomainEnumRenderer = new EnumRenderer<StorageDomainType>();

                    String storageDomainType = storageDomainEnumRenderer.render(((IStorageModel) object).getRole());
                    String storageType = storageEnumRenderer.render(((IStorageModel) object).getType());

                    formattedString = storageDomainType + " / " + storageType; //$NON-NLS-1$
                }
                return formattedString;
            }
        });
    }

    void addStyles() {
        storageTypeListEditor.setLabelStyleName(style.label());
        storageTypeListEditor.addContentWidgetStyleName(style.storageContentWidget());
        formatListEditor.setLabelStyleName(style.label());
        formatListEditor.addContentWidgetStyleName(style.formatContentWidget());
    }

    void localize(ApplicationConstants constants) {
        nameEditor.setLabel(constants.storagePopupNameLabel());
        descriptionEditor.setLabel(constants.storagePopupDescriptionLabel());
        commentEditor.setLabel(constants.commentLabel());
        datacenterListEditor.setLabel(constants.storagePopupDataCenterLabel());
        storageTypeListEditor.setLabel(constants.storagePopupStorageTypeLabel());
        formatListEditor.setLabel(constants.storagePopupFormatTypeLabel());
        hostListEditor.setLabel(constants.storagePopupHostLabel());
    }

    @Override
    public void edit(StorageModel object) {
        driver.edit(object);

        final StorageModel storageModel = object;
        storageModel.getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                // Reveal the appropriate storage view according to the selected storage type
                revealStorageView(storageModel);
            }
        });

        storageModel.getDataCenterAlert().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                datacenterAlertIcon.setVisible(storageModel.getDataCenterAlert().getIsAvailable());
                datacenterAlertIcon.setTitle((String) storageModel.getDataCenterAlert().getEntity());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void revealStorageView(StorageModel object) {
        IStorageModel model = object.getSelectedItem();

        if (model != null) {

            if (model.getType() == StorageType.NFS) {
                storageView = new NfsStorageView();
            } else if (model.getType() == StorageType.LOCALFS) {
                storageView = new LocalStorageView();
            } else if (model.getType() == StorageType.POSIXFS) {
                storageView = new PosixStorageView();
            } else if (model.getType() == StorageType.GLUSTERFS) {
                storageView = new GlusterStorageView();
            } else if (model.getType() == StorageType.FCP) {
                storageView = new FcpStorageView(true);
            } else if (model.getType() == StorageType.ISCSI) {
                storageView = new IscsiStorageView(true);
            }
        }

        // Re-apply element IDs on 'storageView' change
        ViewIdHandler.idHandler.generateAndSetIds(this);

        // Clear the current storage view
        specificStorageTypePanel.clear();

        // Add the new storage view and call focus on it if needed
        if (storageView != null && model != null) {
            storageView.edit(model);
            specificStorageTypePanel.add(storageView);

            if (!nameEditor.isVisible()) {
                storageView.focus();
            }
        }
    }

    @Override
    public StorageModel flush() {
        return driver.flush();
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);

        if (storageView != null) {
            if (!nameEditor.isVisible()) {
                storageView.focus();
            }
        }
    }

    @Override
    public boolean handleEnterKeyDisabled() {
        return storageView.isSubViewFocused();
    }

    interface WidgetStyle extends CssResource {
        String formatContentWidget();

        String storageContentWidget();

        String label();

        String storageTypeLabel();

        String storageDomainTypeLabel();
    }

}
