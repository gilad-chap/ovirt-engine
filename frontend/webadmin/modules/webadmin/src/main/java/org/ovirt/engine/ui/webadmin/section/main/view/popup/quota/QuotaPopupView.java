package org.ovirt.engine.ui.webadmin.section.main.view.popup.quota;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.IVdcQueryableCellTable;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.form.Slider;
import org.ovirt.engine.ui.common.widget.form.Slider.SliderValueChange;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.quota.QuotaPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.widget.table.column.NullableButtonCell;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.inject.Inject;

public class QuotaPopupView extends AbstractModelBoundPopupView<QuotaModel> implements QuotaPopupPresenterWidget.ViewDef, SliderValueChange {

    private static final String GRACE_CLUSTER = "GRACE_CLUSTER"; //$NON-NLS-1$

    private static final String THRESHOLD_CLUSTER = "THRESHOLD_CLUSTER"; //$NON-NLS-1$

    private static final String GRACE_STORAGE = "GRACE_STORAGE"; //$NON-NLS-1$

    private static final String THRESHOLD_STORAGE = "THRESHOLD_STORAGE"; //$NON-NLS-1$

    private static final String MAX_COLOR = "#4E9FDD"; //$NON-NLS-1$

    private static final String MIN_COLOR = "#AFBF27"; //$NON-NLS-1$

    @UiField
    WidgetStyle style;

    @UiField
    @Path(value = "name.entity")
    @WithElementId
    EntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "description.entity")
    @WithElementId
    EntityModelTextBoxEditor descriptionEditor;

    @UiField(provided = true)
    @Path(value = "dataCenter.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> dataCenterEditor;

    @UiField
    @Ignore
    Label memAndCpuLabel;

    @UiField
    @Ignore
    Label storageLabel;

    @UiField(provided = true)
    Slider clusterGraceSlider;

    @UiField(provided = true)
    Slider clusterThresholdSlider;

    @UiField
    @Ignore
    Label clusterThresholdLabel;

    @UiField
    @Ignore
    Label clusterGraceLabel;

    @UiField(provided = true)
    Slider storageGraceSlider;

    @UiField(provided = true)
    Slider storageThresholdSlider;

    @UiField
    @Ignore
    Label storageThresholdLabel;

    @UiField
    @Ignore
    Label storageGraceLabel;

    @UiField(provided = true)
    @Path(value = "globalClusterQuota.entity")
    @WithElementId
    EntityModelRadioButtonEditor globalClusterQuotaRadioButtonEditor;

    @UiField(provided = true)
    @Path(value = "specificClusterQuota.entity")
    @WithElementId
    EntityModelRadioButtonEditor specificClusterQuotaRadioButtonEditor;

    @UiField(provided = true)
    @Path(value = "globalStorageQuota.entity")
    @WithElementId
    EntityModelRadioButtonEditor globalStorageQuotaRadioButtonEditor;

    @UiField(provided = true)
    @Path(value = "specificStorageQuota.entity")
    @WithElementId
    EntityModelRadioButtonEditor specificStorageQuotaRadioButtonEditor;

    @UiField
    @Ignore
    ScrollPanel clusterQuotaTableContainer;

    @Ignore
    private IVdcQueryableCellTable<QuotaVdsGroup, ListModel> quotaClusterTable;

    @Ignore
    private IVdcQueryableCellTable<QuotaStorage, ListModel> quotaStorageTable;

    @UiField
    @Ignore
    ScrollPanel storageQuotaTableContainer;

    private Column<QuotaVdsGroup, Boolean> isClusterInQuotaColumn = null;
    private Column<QuotaStorage, Boolean> isStorageInQuotaColumn = null;

    private QuotaModel model;

    private boolean firstTime = false;

    ArrayList<Guid> selectedClusterGuid = new ArrayList<Guid>();
    ArrayList<Guid> selectedStorageGuid = new ArrayList<Guid>();

    interface Driver extends SimpleBeanEditorDriver<QuotaModel, QuotaPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, QuotaPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<QuotaPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public QuotaPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors();
        initRadioButtonEditors();
        initSliders();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        addStyles();
        Driver.driver.initialize(this);
        initTables(constants);
    }

    private void addStyles() {
    }

    private void initSliders() {
        clusterThresholdSlider = new Slider(2, 0, 100, 80, MIN_COLOR);
        clusterThresholdSlider.setSliderValueChange(THRESHOLD_CLUSTER, this);
        clusterGraceSlider = new Slider(2, 101, 200, 120, MAX_COLOR);
        clusterGraceSlider.setSliderValueChange(GRACE_CLUSTER, this);

        storageThresholdSlider = new Slider(2, 0, 100, 80, MIN_COLOR);
        storageThresholdSlider.setSliderValueChange(THRESHOLD_STORAGE, this);
        storageGraceSlider = new Slider(2, 101, 200, 120, MAX_COLOR);
        storageGraceSlider.setSliderValueChange(GRACE_STORAGE, this);
    }

    private void initTables(ApplicationConstants constants) {
        initQuotaClusterTable(constants);
        initQuotaStorageTable(constants);
    }

    private void initQuotaStorageTable(final ApplicationConstants constants) {
        quotaStorageTable = new IVdcQueryableCellTable<QuotaStorage, ListModel>();
        storageQuotaTableContainer.add(quotaStorageTable);

        isStorageInQuotaColumn = new Column<QuotaStorage, Boolean>(
                new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(QuotaStorage object) {
                if (selectedStorageGuid.contains(object.getStorageId()) || object.getStorageSizeGB() != null) {
                    if (!selectedStorageGuid.contains(object.getStorageId())) {
                        selectedStorageGuid.add(object.getStorageId());
                    }
                    return true;
                }
                return false;
            }
        };

        isStorageInQuotaColumn.setFieldUpdater(new FieldUpdater<QuotaStorage, Boolean>() {
            @Override
            public void update(int index, QuotaStorage object, Boolean value) {
                if (value) {
                    selectedStorageGuid.add(object.getStorageId());
                    object.setStorageSizeGB((long) -1);
                } else {
                    selectedStorageGuid.remove(object.getStorageId());
                    object.setStorageSizeGB(null);
                }
                if ((Boolean) model.getGlobalStorageQuota().getEntity()) {
                    quotaStorageTable.edit(model.getQuotaStorages());
                } else {
                    quotaStorageTable.edit(model.getAllDataCenterStorages());
                }
            }
        });

        quotaStorageTable.addColumn(new TextColumnWithTooltip<QuotaStorage>() {
            @Override
            public String getValue(QuotaStorage object) {
                if (object.getStorageName() == null || object.getStorageName().length() == 0) {
                    return constants.utlQuotaAllStoragesQuotaPopup(); //$NON-NLS-1$
                }
                return object.getStorageName();
            }
        }, constants.storageNameQuota(), "200px"); //$NON-NLS-1$

        quotaStorageTable.addColumn(new TextColumnWithTooltip<QuotaStorage>() {
            @Override
            public String getValue(QuotaStorage object) {
                String str = ""; //$NON-NLS-1$
                if (object.getStorageSizeGB() == null) {
                    return ""; //$NON-NLS-1$
                } else if (object.getStorageSizeGB() == -1) {
                    str = constants.outOfQuota() + constants.unlimitedQuota();
                } else {
                    str = constants.outOfQuota() + object.getStorageSizeGB();
                }
                return object.getStorageSizeGBUsage() + str + " GB"; //$NON-NLS-1$
            }
        }, constants.quota());

        NullableButtonCell editCellButton = new NullableButtonCell();
        Column<QuotaStorage, String> editColumn = new Column<QuotaStorage, String>(editCellButton) {
            @Override
            public String getValue(QuotaStorage object) {
                if ((Boolean) model.getGlobalStorageQuota().getEntity()
                        || ((Boolean) model.getSpecificStorageQuota().getEntity() && selectedStorageGuid.contains(object.getStorageId()))) {
                    return constants.editCellQuota();
                }
                return null;
            }
        };

        quotaStorageTable.addColumn(editColumn, constants.empty(), "50px"); //$NON-NLS-1$
        editColumn.setFieldUpdater(new FieldUpdater<QuotaStorage, String>() {
            @Override
            public void update(int index, QuotaStorage object, String value) {
                model.editQuotaStorage(object);
            }
        });
    }

    private void initQuotaClusterTable(final ApplicationConstants constants) {
        quotaClusterTable = new IVdcQueryableCellTable<QuotaVdsGroup, ListModel>();
        clusterQuotaTableContainer.add(quotaClusterTable);

        isClusterInQuotaColumn = new Column<QuotaVdsGroup, Boolean>(
                new CheckboxCell(true, true)) {
            @Override
            public Boolean getValue(QuotaVdsGroup object) {
                if (selectedClusterGuid.contains(object.getVdsGroupId())
                        || (object.getMemSizeMB() != null && object.getVirtualCpu() != null)) {
                    if (!selectedClusterGuid.contains(object.getVdsGroupId())) {
                        selectedClusterGuid.add(object.getVdsGroupId());
                    }
                    return true;
                }
                return false;
            }
        };

        isClusterInQuotaColumn.setFieldUpdater(new FieldUpdater<QuotaVdsGroup, Boolean>() {
            @Override
            public void update(int index, QuotaVdsGroup object, Boolean value) {
                if (value) {
                    selectedClusterGuid.add(object.getVdsGroupId());
                    object.setVirtualCpu(-1);
                    object.setMemSizeMB((long) -1);
                } else {
                    selectedClusterGuid.remove(object.getVdsGroupId());
                    object.setVirtualCpu(null);
                    object.setMemSizeMB(null);
                }
                if ((Boolean) model.getGlobalClusterQuota().getEntity()) {
                    quotaClusterTable.edit(model.getQuotaClusters());
                } else {
                    quotaClusterTable.edit(model.getAllDataCenterClusters());
                }
            }
        });

        quotaClusterTable.addColumn(new TextColumnWithTooltip<QuotaVdsGroup>() {
            @Override
            public String getValue(QuotaVdsGroup object) {
                if (object.getVdsGroupName() == null || object.getVdsGroupName().length() == 0) {
                    return constants.ultQuotaForAllClustersQuotaPopup(); //$NON-NLS-1$
                }
                return object.getVdsGroupName();
            }
        }, constants.clusterNameQuota(), "200px"); //$NON-NLS-1$

        quotaClusterTable.addColumn(new TextColumnWithTooltip<QuotaVdsGroup>() {
            @Override
            public String getValue(QuotaVdsGroup object) {
                String str = ""; //$NON-NLS-1$
                if (object.getMemSizeMB() == null) {
                    return ""; //$NON-NLS-1$
                } else if (object.getMemSizeMB() == -1) {
                    str = constants.outOfQuota() + constants.unlimitedQuota();
                } else {
                    str = constants.outOfQuota() + object.getMemSizeMB();
                }
                return object.getMemSizeMBUsage() + str + " MB"; //$NON-NLS-1$
            }
        }, constants.quotaOfMemQuota());

        quotaClusterTable.addColumn(new TextColumnWithTooltip<QuotaVdsGroup>() {
            @Override
            public String getValue(QuotaVdsGroup object) {
                String str = ""; //$NON-NLS-1$
                if (object.getVirtualCpu() == null) {
                    return ""; //$NON-NLS-1$
                } else if (object.getVirtualCpu() == -1) {
                    str = constants.outOfQuota() + constants.unlimitedQuota();
                } else {
                    str = constants.outOfQuota() + object.getVirtualCpu();
                }
                return object.getVirtualCpuUsage() + str + " vCPUs"; //$NON-NLS-1$
            }
        }, constants.quotaOfVcpuQuota());

        NullableButtonCell editCellButton = new NullableButtonCell();
        Column<QuotaVdsGroup, String> editColumn = new Column<QuotaVdsGroup, String>(editCellButton) {
            @Override
            public String getValue(QuotaVdsGroup object) {
                if ((Boolean) model.getGlobalClusterQuota().getEntity()
                        || ((Boolean) model.getSpecificClusterQuota().getEntity() && selectedClusterGuid.contains(object.getVdsGroupId()))) {
                    return constants.editCellQuota();
                }
                return null;
            }
        };

        quotaClusterTable.addColumn(editColumn, constants.empty(), "50px"); //$NON-NLS-1$
        editColumn.setFieldUpdater(new FieldUpdater<QuotaVdsGroup, String>() {
            @Override
            public void update(int index, QuotaVdsGroup object, String value) {
                model.editQuotaCluster(object);
            }
        });
    }

    private void initRadioButtonEditors() {
        globalClusterQuotaRadioButtonEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        specificClusterQuotaRadioButtonEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        globalStorageQuotaRadioButtonEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
        specificStorageQuotaRadioButtonEditor = new EntityModelRadioButtonEditor("2"); //$NON-NLS-1$
    }

    private void initListBoxEditors() {
        dataCenterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((storage_pool) object).getname();
            }
        });
    }

    void localize(ApplicationConstants constants) {
        nameEditor.setLabel(constants.nameQuotaPopup());
        descriptionEditor.setLabel(constants.descriptionQuotaPopup());
        dataCenterEditor.setLabel(constants.dataCenterQuotaPopup());
        memAndCpuLabel.setText(constants.memAndCpuQuotaPopup());
        storageLabel.setText(constants.storageQuotaPopup());
        globalClusterQuotaRadioButtonEditor.setLabel(constants.ultQuotaForAllClustersQuotaPopup());
        specificClusterQuotaRadioButtonEditor.setLabel(constants.useQuotaSpecificClusterQuotaPopup());
        globalStorageQuotaRadioButtonEditor.setLabel(constants.utlQuotaAllStoragesQuotaPopup());
        specificStorageQuotaRadioButtonEditor.setLabel(constants.usedQuotaSpecStoragesQuotaPopup());
        clusterGraceLabel.setText(constants.quotaClusterGrace());
        clusterThresholdLabel.setText(constants.quotaClusterThreshold());
        storageGraceLabel.setText(constants.quotaStorageGrace());
        storageThresholdLabel.setText(constants.quotaStorageThreshold());
    }

    @Override
    public void edit(QuotaModel object) {
        this.model = object;
        if (!firstTime) {
            registerHandlers();
            firstTime = true;
            updateSliders();
        }

        quotaClusterTable.edit(object.getQuotaClusters());
        quotaStorageTable.edit(object.getQuotaStorages());
        Driver.driver.edit(object);
    }

    private void registerHandlers() {
        model.getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).PropertyName;
                if ("Window".equals(propName) && model.getWindow() == null) { //$NON-NLS-1$
                    if ((Boolean) model.getSpecificClusterQuota().getEntity()) {
                        quotaClusterTable.edit(model.getAllDataCenterClusters());
                    } else {
                        quotaClusterTable.edit(model.getQuotaClusters());
                    }
                    if ((Boolean) model.getSpecificStorageQuota().getEntity()) {
                        quotaStorageTable.edit(model.getAllDataCenterStorages());
                    } else {
                        quotaStorageTable.edit(model.getQuotaStorages());
                    }
                }
            }
        });

        model.getSpecificClusterQuota().getEntityChangedEvent().addListener(clusterListener);

        model.getSpecificStorageQuota().getEntityChangedEvent().addListener(storageListener);
    }

    final IEventListener clusterListener = new IEventListener() {

        @Override
        public void eventRaised(Event ev, Object sender, EventArgs args) {
            if ((Boolean) model.getSpecificClusterQuota().getEntity()) {
                quotaClusterTable.insertColumn(0, isClusterInQuotaColumn);
                quotaClusterTable.setColumnWidth(isClusterInQuotaColumn, "30px"); //$NON-NLS-1$
                quotaClusterTable.edit(model.getAllDataCenterClusters());
            } else {
                quotaClusterTable.removeColumn(isClusterInQuotaColumn);
                quotaClusterTable.edit(model.getQuotaClusters());
            }
        }
    };

    final IEventListener storageListener = new IEventListener() {

        @Override
        public void eventRaised(Event ev, Object sender, EventArgs args) {
            if ((Boolean) model.getSpecificStorageQuota().getEntity()) {
                quotaStorageTable.insertColumn(0, isStorageInQuotaColumn);
                quotaStorageTable.setColumnWidth(isStorageInQuotaColumn, "30px"); //$NON-NLS-1$
                quotaStorageTable.edit(model.getAllDataCenterStorages());
            } else {
                quotaStorageTable.removeColumn(isStorageInQuotaColumn);
                quotaStorageTable.edit(model.getQuotaStorages());
            }
        }
    };

    @Override
    public QuotaModel flush() {
        quotaClusterTable.flush();
        quotaStorageTable.flush();
        return Driver.driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String textBoxWidth();
    }

    private void updateSliders() {
        clusterThresholdSlider.setValue((Integer) model.getThresholdCluster().getEntity());
        clusterGraceSlider.setValue((Integer) model.getGraceCluster().getEntity() + 100);
        storageThresholdSlider.setValue((Integer) model.getThresholdStorage().getEntity());
        storageGraceSlider.setValue((Integer) model.getGraceStorage().getEntity() + 100);
    }

    @Override
    public void onSliderValueChange(String name, int value) {
        if (name.equals(THRESHOLD_CLUSTER)) {
            model.getThresholdCluster().setEntity(value);
        } else if (name.equals(GRACE_CLUSTER)) {
            model.getGraceCluster().setEntity(value - 100);
        } else if (name.equals(THRESHOLD_STORAGE)) {
            model.getThresholdStorage().setEntity(value);
        } else if (name.equals(GRACE_STORAGE)) {
            model.getGraceStorage().setEntity(value - 100);
        }
    }
}
