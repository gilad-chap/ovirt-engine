
package org.ovirt.engine.ui.common.widget.uicommon.popup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.event.shared.EventBus;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.StringFormat;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.EntityModelWidgetWithInfo;
import org.ovirt.engine.ui.common.widget.dialog.AdvancedParametersExpander;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.EntityModelLabel;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelTypeAheadListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.VncKeyMapRenderer;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.form.key_value.KeyValueWidget;
import org.ovirt.engine.ui.common.widget.parser.MemorySizeParser;
import org.ovirt.engine.ui.common.widget.profile.ProfilesInstanceTypeEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.MemorySizeRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfigMap;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.SerialNumberPolicyWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VmPopupVmInitWidget;
import org.ovirt.engine.ui.common.widget.uicommon.storage.DisksAllocationView;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DataCenterWithCluster;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.TimeZoneModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.external.StringUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;
import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.simpleField;

public abstract class AbstractVmPopupWidget extends AbstractModeSwitchingPopupWidget<UnitVmModel> {

    interface Driver extends SimpleBeanEditorDriver<UnitVmModel, AbstractVmPopupWidget> {
    }

    interface ViewUiBinder extends UiBinder<DialogTabPanel, AbstractVmPopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    protected interface Style extends CssResource {
        String longCheckboxContent();

        String provisioningEditorContent();

        String provisioningRadioContent();

        String cdAttachedLabelWidth();

        String assignedVmsLabel();

        String labelDisabled();

        String generalTabExtendedRightWidgetWidth();

        String cdImageEditor();

        String monitorsStyles();

        String migrationSelectorInner();

        String isVirtioScsiEnabledEditor();
    }

    @UiField
    protected Style style;

    // ==General Tab==
    @UiField
    protected DialogTab generalTab;

    @UiField(provided = true)
    @Path(value = "dataCenterWithClustersList.selectedItem")
    @WithElementId("dataCenterWithCluster")
    public ListModelTypeAheadListBoxEditor<DataCenterWithCluster> dataCenterWithClusterEditor;

    @UiField(provided = true)
    @Path(value = "quota.selectedItem")
    @WithElementId("quota")
    public ListModelTypeAheadListBoxEditor<Quota> quotaEditor;

    @UiField
    @Ignore
    public Label nameLabel;

    @UiField(provided = true)
    @Path(value = "name.entity")
    @WithElementId("name")
    public StringEntityModelTextBoxOnlyEditor nameEditor;

    @UiField(provided = true)
    @Path(value = "templateVersionName.entity")
    @WithElementId("templateVersionName")
    public StringEntityModelTextBoxEditor templateVersionNameEditor;

    @UiField(provided = true)
    @Ignore
    public InfoIcon poolNameIcon;

    @UiField(provided = true)
    @Path(value = "description.entity")
    @WithElementId("description")
    public StringEntityModelTextBoxEditor descriptionEditor;

    @UiField
    @Path(value = "comment.entity")
    @WithElementId("comment")
    public StringEntityModelTextBoxEditor commentEditor;

    @UiField(provided = true)
    @Path(value = "baseTemplate.selectedItem")
    @WithElementId("baseTemplate")
    public ListModelTypeAheadListBoxEditor<VmTemplate> baseTemplateEditor;

    @UiField(provided = true)
    @Path(value = "template.selectedItem")
    @WithElementId("template")
    public ListModelTypeAheadListBoxEditor<VmTemplate> templateEditor;

    @UiField(provided = true)
    @Path(value = "OSType.selectedItem")
    @WithElementId("osType")
    public ListModelListBoxEditor<Integer> oSTypeEditor;

    @UiField(provided = true)
    @Path(value = "vmType.selectedItem")
    @WithElementId("vmType")
    public ListModelListBoxEditor<VmType> vmTypeEditor;

    @UiField(provided = true)
    @Path(value = "isDeleteProtected.entity")
    @WithElementId("isDeleteProtected")
    public EntityModelCheckBoxEditor isDeleteProtectedEditor;

    @UiField
    public Panel logicalNetworksEditorPanel;

    @UiField
    @Ignore
    @WithElementId("vnicsEditor")
    public ProfilesInstanceTypeEditor profilesInstanceTypeEditor;

    @UiField
    @Ignore
    Label generalWarningMessage;

    // == System ==
    @UiField
    protected DialogTab systemTab;

    @UiField(provided = true)
    @Path(value = "memSize.entity")
    @WithElementId("memSize")
    public EntityModelTextBoxEditor<Integer> memSizeEditor;

    @Path(value = "totalCPUCores.entity")
    @WithElementId("totalCPUCores")
    public StringEntityModelTextBoxOnlyEditor totalvCPUsEditor;

    @UiField(provided = true)
    @Ignore
    public EntityModelWidgetWithInfo totalvCPUsEditorWithInfoIcon;

    @UiField
    @Ignore
    AdvancedParametersExpander vcpusAdvancedParameterExpander;

    @UiField
    @Ignore
    Panel vcpusAdvancedParameterExpanderContent;

    @UiField(provided = true)
    @Path(value = "numOfSockets.selectedItem")
    @WithElementId("numOfSockets")
    public ListModelListBoxEditor<Integer> numOfSocketsEditor;

    @UiField(provided = true)
    @Path(value = "coresPerSocket.selectedItem")
    @WithElementId("coresPerSocket")
    public ListModelListBoxEditor<Integer> corePerSocketEditor;

    @UiField
    @Ignore
    public Label ssoMethodLabel;

    @UiField(provided = true)
    @Path(value = "ssoMethodNone.entity")
    @WithElementId("ssoMethodNone")
    public EntityModelRadioButtonEditor ssoMethodNone;

    @UiField(provided = true)
    @Path(value = "ssoMethodGuestAgent.entity")
    @WithElementId("ssoMethodGuestAgent")
    public EntityModelRadioButtonEditor ssoMethodGuestAgent;

    @UiField(provided = true)
    @Path(value = "isSoundcardEnabled.entity")
    @WithElementId("isSoundcardEnabled")
    public EntityModelCheckBoxEditor isSoundcardEnabledEditor;

    @UiField(provided = true)
    @Path("copyPermissions.entity")
    @WithElementId("copyTemplatePermissions")
    public EntityModelCheckBoxEditor copyTemplatePermissionsEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId("serialNumberPolicy")
    public SerialNumberPolicyWidget serialNumberPolicyEditor;

    // == Pools ==
    @UiField
    protected DialogTab poolTab;

    @UiField(provided = true)
    @Path(value = "poolType.selectedItem")
    @WithElementId("poolType")
    public ListModelListBoxEditor<EntityModel<VmPoolType>> poolTypeEditor;

    @UiField(provided = true)
    @Ignore
    public InfoIcon newPoolPrestartedVmsIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon editPoolPrestartedVmsIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon newPoolMaxAssignedVmsPerUserIcon;

    @UiField(provided = true)
    @Ignore
    public InfoIcon editPoolMaxAssignedVmsPerUserIcon;

    @UiField(provided = true)
    @Path(value = "prestartedVms.entity")
    @WithElementId("prestartedVms")
    public IntegerEntityModelTextBoxOnlyEditor prestartedVmsEditor;

    @UiField(provided = true)
    @Path("maxAssignedVmsPerUser.entity")
    @WithElementId("maxAssignedVmsPerUser")
    public IntegerEntityModelTextBoxOnlyEditor maxAssignedVmsPerUserEditor;

    @UiField
    @Ignore
    public FlowPanel newPoolEditVmsPanel;

    @UiField
    @Ignore
    public FlowPanel newPoolEditMaxAssignedVmsPerUserPanel;

    @UiField
    @Ignore
    public Label prestartedLabel;

    @UiField(provided = true)
    @Path("numOfDesktops.entity")
    @WithElementId("numOfVms")
    public EntityModelTextBoxEditor<Integer> numOfVmsEditor;

    @UiField
    @Ignore
    public FlowPanel editPoolEditVmsPanel;

    @UiField
    public FlowPanel editIncreaseVmsPanel;

    @UiField
    @Ignore
    public FlowPanel editPoolIncraseNumOfVmsPanel;

    @UiField
    public FlowPanel editPrestartedVmsPanel;
    @UiField
    @Ignore
    public FlowPanel editPoolEditMaxAssignedVmsPerUserPanel;

    @UiField
    @Ignore
    public Label editPrestartedVmsLabel;

    @UiField(provided = true)
    @Path("prestartedVms.entity")
    @WithElementId("editPrestartedVms")
    public IntegerEntityModelTextBoxOnlyEditor editPrestartedVmsEditor;

    @UiField(provided = true)
    @Path("numOfDesktops.entity")
    @WithElementId("incraseNumOfVms")
    public EntityModelTextBoxOnlyEditor<Integer> incraseNumOfVmsEditor;

    @UiField(provided = true)
    @Path("maxAssignedVmsPerUser.entity")
    @WithElementId("editMaxAssignedVmsPerUser")
    public IntegerEntityModelTextBoxOnlyEditor editMaxAssignedVmsPerUserEditor;

    @UiField(provided = true)
    @Path("assignedVms.entity")
    public ValueLabel<Integer> outOfxInPool;

    @UiField(provided = true)
    @Path(value = "timeZone.selectedItem")
    @WithElementId("timeZone")
    public ListModelListBoxEditor<TimeZoneModel> timeZoneEditor;

    // ==Initial run Tab==
    @UiField
    protected DialogTab initialRunTab;

    @UiField(provided = true)
    @Path(value = "domain.selectedItem")
    @WithElementId("domain")
    public ListModelListBoxEditor<String> domainEditor;

    @UiField
    @Path(value = "vmInitEnabled.entity")
    @WithElementId("vmInitEnabled")
    public EntityModelCheckBoxEditor vmInitEnabledEditor;

    @UiField
    @Ignore
    public VmPopupVmInitWidget vmInitEditor;

    // ==Console Tab==
    @UiField
    protected DialogTab consoleTab;

    @UiField(provided = true)
    @Path(value = "displayProtocol.selectedItem")
    @WithElementId("displayProtocol")
    public ListModelListBoxEditor<EntityModel<DisplayType>> displayProtocolEditor;

    @UiField(provided = true)
    @Path(value = "vncKeyboardLayout.selectedItem")
    @WithElementId("vncKeyboardLayout")
    public ListModelListBoxEditor<String> vncKeyboardLayoutEditor;

    @UiField(provided = true)
    @Path(value = "usbPolicy.selectedItem")
    @WithElementId("usbPolicy")
    public ListModelListBoxEditor<UsbPolicy> usbSupportEditor;

    @UiField(provided = true)
    @Path(value = "numOfMonitors.selectedItem")
    @WithElementId("numOfMonitors")
    public ListModelListBoxEditor<Integer> numOfMonitorsEditor;

    @UiField(provided = true)
    @Path(value = "isSingleQxlEnabled.entity")
    @WithElementId("isSingleQxlEnabled")
    public EntityModelCheckBoxEditor isSingleQxlEnabledEditor;

    @UiField(provided = true)
    @Path(value = "isStateless.entity")
    @WithElementId("isStateless")
    public EntityModelCheckBoxEditor isStatelessEditor;

    @UiField(provided = true)
    @Path(value = "isRunAndPause.entity")
    @WithElementId("isRunAndPause")
    public EntityModelCheckBoxEditor isRunAndPauseEditor;

    @UiField(provided = true)
    @Path(value = "isSmartcardEnabled.entity")
    @WithElementId("isSmartcardEnabled")
    public EntityModelCheckBoxEditor isSmartcardEnabledEditor;

    @UiField(provided = true)
    @Path(value = "allowConsoleReconnect.entity")
    @WithElementId("allowConsoleReconnect")
    public EntityModelCheckBoxEditor allowConsoleReconnectEditor;

    @UiField(provided = true)
    @Path(value = "isConsoleDeviceEnabled.entity")
    @WithElementId("isConsoleDeviceEnabled")
    public EntityModelCheckBoxEditor isConsoleDeviceEnabledEditor;

    @UiField
    @Path(value = "spiceProxy.entity")
    @WithElementId
    public StringEntityModelTextBoxEditor spiceProxyEditor;

    @UiField(provided = true)
    @Ignore
    public EntityModelWidgetWithInfo spiceProxyEnabledCheckboxWithInfoIcon;

    @Path(value = "spiceProxyEnabled.entity")
    @WithElementId
    public EntityModelCheckBoxOnlyEditor spiceProxyOverrideEnabledEditor;

    // ==Host Tab==
    @UiField
    protected DialogTab hostTab;

    @UiField(provided = true)
    @Path(value = "hostCpu.entity")
    @WithElementId("hostCpu")
    public EntityModelCheckBoxEditor hostCpuEditor;

    @UiField(provided = true)
    @Path(value = "migrationMode.selectedItem")
    @WithElementId("migrationMode")
    public ListModelListBoxEditor<MigrationSupport> migrationModeEditor;

    @UiField(provided = true)
    @Path(value = "overrideMigrationDowntime.entity")
    @WithElementId("overrideMigrationDowntime")
    public EntityModelCheckBoxOnlyEditor overrideMigrationDowntimeEditor;

    @UiField(provided = true)
    public InfoIcon migrationDowntimeInfoIcon;

    @UiField(provided = true)
    @Path(value = "migrationDowntime.entity")
    @WithElementId("migrationDowntime")
    public IntegerEntityModelTextBoxOnlyEditor migrationDowntimeEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId("specificHost")
    public RadioButton specificHost;

    @UiField
    @Ignore
    public Label specificHostLabel;

    @UiField(provided = true)
    public InfoIcon nonEditableWhileVmNotDownInfo;

    @UiField(provided = true)
    @Path(value = "defaultHost.selectedItem")
    @WithElementId("defaultHost")
    public ListModelListBoxEditor<VDS> defaultHostEditor;

    @UiField(provided = true)
    @Path(value = "isAutoAssign.entity")
    @WithElementId("isAutoAssign")
    public EntityModelRadioButtonEditor isAutoAssignEditor;

    @UiField(provided = true)
    InfoIcon cpuPinningInfo;

    @UiField(provided = true)
    @Path(value = "cpuPinning.entity")
    @WithElementId("cpuPinning")
    public StringEntityModelTextBoxOnlyEditor cpuPinning;

    @UiField(provided = true)
    @Path(value = "cpuSharesAmountSelection.selectedItem")
    @WithElementId("cpuSharesAmountSelection")
    public ListModelListBoxOnlyEditor<UnitVmModel.CpuSharesAmount> cpuSharesAmountSelectionEditor;

    @UiField
    @Ignore
    public Label cpuSharesEditor;

    @UiField(provided = true)
    @Path(value = "cpuSharesAmount.entity")
    @WithElementId("cpuSharesAmount")
    public IntegerEntityModelTextBoxOnlyEditor cpuSharesAmountEditor;

    // ==High Availability Tab==
    @UiField
    protected DialogTab highAvailabilityTab;

    @UiField(provided = true)
    @Path(value = "isHighlyAvailable.entity")
    @WithElementId("isHighlyAvailable")
    public EntityModelCheckBoxEditor isHighlyAvailableEditor;

    // TODO: Priority is a ListModel which is rendered as RadioBox
    @UiField(provided = true)
    @Ignore
    @WithElementId("priority")
    public EntityModelCellTable<ListModel> priorityEditor;

    @UiField(provided = true)
    @Path(value = "watchdogModel.selectedItem")
    @WithElementId("watchdogModel")
    public ListModelListBoxEditor<String> watchdogModelEditor;

    @UiField(provided = true)
    @Path(value = "watchdogAction.selectedItem")
    @WithElementId("watchdogAction")
    public ListModelListBoxEditor<String> watchdogActionEditor;

    // ==Resource Allocation Tab==
    @UiField
    protected DialogTab resourceAllocationTab;

    @UiField
    protected FlowPanel storageAllocationPanel;

    @UiField
    protected HorizontalPanel provisionSelectionPanel;

    @UiField
    protected FlowPanel disksAllocationPanel;

    @UiField
    @Ignore
    @WithElementId("provisioning")
    public ListModelListBoxEditor provisioningEditor;

    @UiField(provided = true)
    @Path(value = "minAllocatedMemory.entity")
    @WithElementId("minAllocatedMemory")
    public EntityModelTextBoxEditor<Integer> minAllocatedMemoryEditor;

    @UiField(provided = true)
    @Path(value = "memoryBalloonDeviceEnabled.entity")
    EntityModelCheckBoxEditor isMemoryBalloonDeviceEnabled;

    @UiField(provided = true)
    @Path(value = "provisioningThin_IsSelected.entity")
    @WithElementId("provisioningThin")
    public EntityModelRadioButtonEditor provisioningThinEditor;

    @UiField(provided = true)
    @Path(value = "provisioningClone_IsSelected.entity")
    @WithElementId("provisioningClone")
    public EntityModelRadioButtonEditor provisioningCloneEditor;

    @UiField(provided = true)
    @Path(value = "isVirtioScsiEnabled.entity")
    EntityModelCheckBoxEditor isVirtioScsiEnabled;

    @UiField(provided = true)
    @Ignore
    public InfoIcon isVirtioScsiEnabledInfoIcon;

    @UiField
    @Ignore
    Label disksAllocationLabel;

    @UiField(provided = true)
    @Ignore
    @WithElementId("disksAllocation")
    public DisksAllocationView disksAllocationView;

    // ==Boot Options Tab==
    @UiField
    protected DialogTab bootOptionsTab;

    @UiField(provided = true)
    @Path(value = "firstBootDevice.selectedItem")
    @WithElementId("firstBootDevice")
    public ListModelListBoxEditor<EntityModel<BootSequence>> firstBootDeviceEditor;

    @UiField(provided = true)
    @Path(value = "secondBootDevice.selectedItem")
    @WithElementId("secondBootDevice")
    public ListModelListBoxEditor<EntityModel<BootSequence>> secondBootDeviceEditor;

    @UiField(provided = true)
    @Path(value = "cdImage.selectedItem")
    @WithElementId("cdImage")
    public ListModelListBoxEditor<String> cdImageEditor;

    @UiField(provided = true)
    @Path(value = "cdAttached.entity")
    @WithElementId("cdAttached")
    public EntityModelCheckBoxEditor cdAttachedEditor;

    @UiField
    protected FlowPanel linuxBootOptionsPanel;

    @UiField(provided = true)
    @Path(value = "kernel_path.entity")
    @WithElementId("kernelPath")
    public StringEntityModelTextBoxEditor kernel_pathEditor;

    @UiField(provided = true)
    @Path(value = "initrd_path.entity")
    @WithElementId("initrdPath")
    public StringEntityModelTextBoxEditor initrd_pathEditor;

    @UiField(provided = true)
    @Path(value = "kernel_parameters.entity")
    @WithElementId("kernelParameters")
    public StringEntityModelTextBoxEditor kernel_parametersEditor;

    @UiField
    @Ignore
    Label nativeUsbWarningMessage;

    // ==Custom Properties Tab==
    @UiField
    protected DialogTab customPropertiesTab;

    @UiField
    @Ignore
    protected KeyValueWidget<KeyValueModel> customPropertiesSheetEditor;

    private final CommonApplicationMessages messages;

    @UiField
    @Ignore
    protected AdvancedParametersExpander expander;

    @UiField
    @Ignore
    Panel expanderContent;

    @UiField
    @Ignore
    ButtonBase refreshButton;

    private UnitVmModel unitVmModel;

    private final Driver driver = GWT.create(Driver.class);

    private final CommonApplicationTemplates applicationTemplates;

    private final CommonApplicationConstants constants;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AbstractVmPopupWidget(CommonApplicationConstants constants,
            CommonApplicationResources resources,
            final CommonApplicationMessages messages,
            CommonApplicationTemplates applicationTemplates,
            EventBus eventBus) {

        this.messages = messages;
        this.applicationTemplates = applicationTemplates;
        this.constants = constants;

        initListBoxEditors();
        // Contains a special parser/renderer
        memSizeEditor = new EntityModelTextBoxEditor<Integer>(
                new MemorySizeRenderer<Integer>(constants), new MemorySizeParser(), new ModeSwitchingVisibilityRenderer());
        minAllocatedMemoryEditor = new EntityModelTextBoxEditor<Integer>(
                new MemorySizeRenderer<Integer>(constants), new MemorySizeParser(), new ModeSwitchingVisibilityRenderer());

        // TODO: How to align right without creating the widget manually?
        hostCpuEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isHighlyAvailableEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        watchdogModelEditor = new ListModelListBoxEditor<String>(new ModeSwitchingVisibilityRenderer());
        watchdogActionEditor = new ListModelListBoxEditor<String>(new ModeSwitchingVisibilityRenderer());
        isStatelessEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isRunAndPauseEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isDeleteProtectedEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSmartcardEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isConsoleDeviceEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        cdAttachedEditor = new EntityModelCheckBoxEditor(Align.LEFT, new ModeSwitchingVisibilityRenderer());
        allowConsoleReconnectEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSoundcardEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        ssoMethodNone = new EntityModelRadioButtonEditor("ssoMethod", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        ssoMethodGuestAgent = new EntityModelRadioButtonEditor("ssoMethod", new ModeSwitchingVisibilityRenderer());//$NON-NLS-1$
        copyTemplatePermissionsEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isMemoryBalloonDeviceEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer(), true);
        isVirtioScsiEnabled = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        isSingleQxlEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT, new ModeSwitchingVisibilityRenderer());
        cpuPinningInfo =
                new InfoIcon(SafeHtmlUtils.fromTrustedString(applicationTemplates.italicFixedWidth("400px", //$NON-NLS-1$
                        constants.cpuPinningLabelExplanation())
                        .asString()
                        .replaceAll("(\r\n|\n)", "<br />")), resources);//$NON-NLS-1$ //$NON-NLS-2$
        isVirtioScsiEnabledInfoIcon =
                new InfoIcon(applicationTemplates.italicText(constants.isVirtioScsiEnabledInfo()), resources);
        nonEditableWhileVmNotDownInfo =
                new InfoIcon(applicationTemplates.italicText(constants.nonEditableMigrationFieldsWhileVmNotDownInfo()),
                        resources);
        final Integer defaultMaximumMigrationDowntime = (Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.DefaultMaximumMigrationDowntime);
        migrationDowntimeInfoIcon = new InfoIcon(applicationTemplates.italicText(messages.migrationDowntimeInfo(defaultMaximumMigrationDowntime)), resources);
        priorityEditor = new EntityModelCellTable<ListModel>(
                (Resources) GWT.create(ButtonCellTableResources.class));
        disksAllocationView = new DisksAllocationView(constants);
        serialNumberPolicyEditor = new SerialNumberPolicyWidget(eventBus, applicationTemplates, messages, resources, new ModeSwitchingVisibilityRenderer());

        initPoolSpecificWidgets(resources, messages);
        initTextBoxEditors();
        initSpiceProxy();
        initTotalVcpus();

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        expander.initWithContent(expanderContent.getElement());
        vcpusAdvancedParameterExpander.initWithContent(vcpusAdvancedParameterExpanderContent.getElement());
        editPrestartedVmsEditor.setKeepTitleOnSetEnabled(true);

        applyStyles();

        localize(constants);

        super.initializeModeSwitching(generalTab);

        generateIds();

        priorityEditor.addEntityModelColumn(new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel model) {
                return model.getTitle();
            }
        }, ""); //$NON-NLS-1$

        driver.initialize(this);
    }

    protected void initSpiceProxy() {
        EntityModelLabel label = new EntityModelLabel();
        label.setText(constants.defineSpiceProxyEnable());
        spiceProxyOverrideEnabledEditor = new EntityModelCheckBoxOnlyEditor();
        spiceProxyEnabledCheckboxWithInfoIcon = new EntityModelWidgetWithInfo(label, spiceProxyOverrideEnabledEditor);
    }

    private void initTotalVcpus() {
        EntityModelLabel label = new EntityModelLabel();
        label.setText(constants.numOfVCPUs());
        totalvCPUsEditor = new StringEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        totalvCPUsEditorWithInfoIcon = new EntityModelWidgetWithInfo(label, totalvCPUsEditor);
        totalvCPUsEditorWithInfoIcon.setExplanation(applicationTemplates.italicText(messages.hotPlugUnplugCpuWarning()));
    }

    public void setSpiceProxyOverrideExplanation(String explanation) {
        spiceProxyEnabledCheckboxWithInfoIcon.setExplanation(applicationTemplates.italicText(explanation));
    }

    private void initTextBoxEditors() {
        templateVersionNameEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        descriptionEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        commentEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        numOfVmsEditor = new IntegerEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        cpuPinning = new StringEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        cpuSharesAmountEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        kernel_pathEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        initrd_pathEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        kernel_parametersEditor = new StringEntityModelTextBoxEditor(new ModeSwitchingVisibilityRenderer());
        nameEditor = new StringEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        prestartedVmsEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        editPrestartedVmsEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        maxAssignedVmsPerUserEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
        editMaxAssignedVmsPerUserEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());
    }

    protected void initPoolSpecificWidgets(CommonApplicationResources resources,
            final CommonApplicationMessages messages) {
        createNumOfDesktopEditors();

        incraseNumOfVmsEditor.setKeepTitleOnSetEnabled(true);
        numOfVmsEditor.setKeepTitleOnSetEnabled(true);

        newPoolPrestartedVmsIcon =
                new InfoIcon(applicationTemplates.italicText(messages.prestartedHelp()), resources);

        editPoolPrestartedVmsIcon =
                new InfoIcon(applicationTemplates.italicText(messages.prestartedHelp()), resources);

        poolNameIcon =
                new InfoIcon(applicationTemplates.italicText(messages.poolNameHelp()), resources);

        newPoolMaxAssignedVmsPerUserIcon =
                new InfoIcon(applicationTemplates.italicText(messages.maxAssignedVmsPerUserHelp()), resources);

        editPoolMaxAssignedVmsPerUserIcon =
                new InfoIcon(applicationTemplates.italicText(messages.maxAssignedVmsPerUserHelp()), resources);

        outOfxInPool = new ValueLabel<Integer>(new AbstractRenderer<Integer>() {

            @Override
            public String render(Integer object) {
                return messages.outOfXVMsInPool(object.toString());
            }

        });

    }

    /**
     * There are two editors which edits the same entity - in the correct subclass make sure that the correct one's
     * value is used to edit the model
     * <p>
     * The default implementation just creates the simple editors
     */
    protected void createNumOfDesktopEditors() {
        incraseNumOfVmsEditor = new IntegerEntityModelTextBoxOnlyEditor();
        numOfVmsEditor = new IntegerEntityModelTextBoxEditor();
    }

    protected abstract void generateIds();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initListBoxEditors() {
        // General tab
        dataCenterWithClusterEditor = new ListModelTypeAheadListBoxEditor<DataCenterWithCluster>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<DataCenterWithCluster>() {

                    @Override
                    public String getReplacementStringNullSafe(DataCenterWithCluster data) {
                        return data.getCluster().getName() + "/" //$NON-NLS-1$
                                + data.getDataCenter().getName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(DataCenterWithCluster data) {
                        String dcDescription =
                                data.getDataCenter().getdescription();

                        return typeAheadNameDescriptionTemplateNullSafe(
                                data.getCluster().getName(),
                                !StringHelper.isNullOrEmpty(dcDescription) ? dcDescription
                                        : data.getDataCenter().getName()
                        );
                    }

                },
                new ModeSwitchingVisibilityRenderer());

        quotaEditor = new ListModelTypeAheadListBoxEditor<Quota>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<Quota>() {

                    @Override
                    public String getReplacementStringNullSafe(Quota data) {
                        return data.getQuotaName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(Quota data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                data.getQuotaName(),
                                data.getDescription()
                        );
                    }

                },
                new ModeSwitchingVisibilityRenderer());

        baseTemplateEditor = new ListModelTypeAheadListBoxEditor<VmTemplate>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<VmTemplate>() {

                    @Override
                    public String getReplacementStringNullSafe(VmTemplate data) {
                        return data.getName();
                    }

                    @Override
                    public String getDisplayStringNullSafe(VmTemplate data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                data.getName(),
                                data.getDescription()
                        );
                    }
                },
                new ModeSwitchingVisibilityRenderer());

        templateEditor = new ListModelTypeAheadListBoxEditor<VmTemplate>(
                new ListModelTypeAheadListBoxEditor.NullSafeSuggestBoxRenderer<VmTemplate>() {

                    @Override
                    public String getReplacementStringNullSafe(VmTemplate data) {
                        return getDisplayableTemplateVersionName(data);
                    }

                    @Override
                    public String getDisplayStringNullSafe(VmTemplate data) {
                        return typeAheadNameDescriptionTemplateNullSafe(
                                getDisplayableTemplateVersionName(data),
                                data.getDescription()
                        );
                    }

                    private String getDisplayableTemplateVersionName(VmTemplate template) {
                        String versionName = template.getTemplateVersionName();
                        if (ConstantsManager.getInstance().getConstants().latestTemplateVersionName().equals(versionName)) {
                            return constants.latest();
                        }

                        versionName = template.getId().equals(template.getBaseTemplateId()) ?
                                constants.baseTemplate() : template.getTemplateVersionName();

                        return (versionName == null ? "" : versionName) //$NON-NLS-1$
                                + StringFormat.format(" (%d)", template.getTemplateVersionNumber()); //$NON-NLS-1$
                    }
                },
                new ModeSwitchingVisibilityRenderer());

        oSTypeEditor = new ListModelListBoxEditor<Integer>(new AbstractRenderer<Integer>() {
            @Override
            public String render(Integer object) {
                return AsyncDataProvider.getOsName(object);
            }
        }, new ModeSwitchingVisibilityRenderer());

        vmTypeEditor = new ListModelListBoxEditor<VmType>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());

        numOfSocketsEditor = new ListModelListBoxEditor<Integer>(new ModeSwitchingVisibilityRenderer());
        corePerSocketEditor = new ListModelListBoxEditor<Integer>(new ModeSwitchingVisibilityRenderer());

        // Pools
        poolTypeEditor = new ListModelListBoxEditor<EntityModel<VmPoolType>>(new NullSafeRenderer<EntityModel<VmPoolType>>() {
            @Override
            public String renderNullSafe(EntityModel<VmPoolType> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        // Windows Sysprep
        domainEditor = new ListModelListBoxEditor<String>(new NullSafeRenderer<String>() {
            @Override
            public String renderNullSafe(String object) {
                return object.toString();
            }
        }, new ModeSwitchingVisibilityRenderer());

        timeZoneEditor = new ListModelListBoxEditor<TimeZoneModel>(new NullSafeRenderer<TimeZoneModel>() {
            @Override
            public String renderNullSafe(TimeZoneModel timeZone) {
                if (timeZone.isDefault()) {
                    return messages.defaultTimeZoneCaption(timeZone.getDisplayValue());
                } else {
                    return timeZone.getDisplayValue();
                }
            }
        }, new ModeSwitchingVisibilityRenderer());

        // Console tab
        displayProtocolEditor = new ListModelListBoxEditor<EntityModel<DisplayType>>(new NullSafeRenderer<EntityModel<DisplayType>>() {
            @Override
            public String renderNullSafe(EntityModel<DisplayType> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        usbSupportEditor =
                new ListModelListBoxEditor<UsbPolicy>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());
        numOfMonitorsEditor = new ListModelListBoxEditor<Integer>(new NullSafeRenderer<Integer>() {
            @Override
            public String renderNullSafe(Integer object) {
                return object.toString();
            }
        }, new ModeSwitchingVisibilityRenderer());

        vncKeyboardLayoutEditor = new ListModelListBoxEditor<String>(new VncKeyMapRenderer(messages), new ModeSwitchingVisibilityRenderer());

        // Host Tab
        specificHost = new RadioButton("runVmOnHostGroup"); //$NON-NLS-1$
        isAutoAssignEditor =
                new EntityModelRadioButtonEditor("runVmOnHostGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        defaultHostEditor = new ListModelListBoxEditor<VDS>(new NullSafeRenderer<VDS>() {
            @Override
            public String renderNullSafe(VDS object) {
                return object.getName();
            }
        }, new ModeSwitchingVisibilityRenderer());

        migrationModeEditor =
                new ListModelListBoxEditor<MigrationSupport>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());

        overrideMigrationDowntimeEditor = new EntityModelCheckBoxOnlyEditor(new ModeSwitchingVisibilityRenderer(), false);
        migrationDowntimeEditor = new IntegerEntityModelTextBoxOnlyEditor(new ModeSwitchingVisibilityRenderer());

        // Resource Allocation
        provisioningThinEditor =
                new EntityModelRadioButtonEditor("provisioningGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$
        provisioningCloneEditor =
                new EntityModelRadioButtonEditor("provisioningGroup", new ModeSwitchingVisibilityRenderer()); //$NON-NLS-1$

        // Boot Options Tab
        firstBootDeviceEditor = new ListModelListBoxEditor<EntityModel<BootSequence>>(new NullSafeRenderer<EntityModel<BootSequence>>() {
            @Override
            public String renderNullSafe(EntityModel<BootSequence> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        secondBootDeviceEditor = new ListModelListBoxEditor<EntityModel<BootSequence>>(new NullSafeRenderer<EntityModel<BootSequence>>() {
            @Override
            public String renderNullSafe(EntityModel<BootSequence> object) {
                return object.getTitle();
            }
        }, new ModeSwitchingVisibilityRenderer());

        cdImageEditor = new ListModelListBoxEditor<String>(new NullSafeRenderer<String>() {
            @Override
            public String renderNullSafe(String object) {
                return object;
            }
        }, new ModeSwitchingVisibilityRenderer());

        cpuSharesAmountSelectionEditor =
                new ListModelListBoxOnlyEditor<UnitVmModel.CpuSharesAmount>(new EnumRenderer(), new ModeSwitchingVisibilityRenderer());
    }

    private String typeAheadNameDescriptionTemplateNullSafe(String name, String description) {
        return applicationTemplates.typeAheadNameDescription(
                name != null ? name : "",
                description != null ? description : "")
                .asString();
    }

    protected void localize(CommonApplicationConstants constants) {
        // Tabs
        highAvailabilityTab.setLabel(constants.highAvailVmPopup());
        resourceAllocationTab.setLabel(constants.resourceAllocVmPopup());
        bootOptionsTab.setLabel(constants.bootOptionsVmPopup());
        customPropertiesTab.setLabel(constants.customPropsVmPopup());
        systemTab.setLabel(constants.systemVmPopup());

        // General Tab
        generalTab.setLabel(constants.GeneralVmPopup());
        dataCenterWithClusterEditor.setLabel(constants.hostClusterVmPopup());
        quotaEditor.setLabel(constants.quotaVmPopup());
        nameLabel.setText(constants.nameVmPopup());
        templateVersionNameEditor.setLabel(constants.templateVersionName());
        descriptionEditor.setLabel(constants.descriptionVmPopup());
        commentEditor.setLabel(constants.commentLabel());
        baseTemplateEditor.setLabel(constants.basedOnTemplateVmPopup());
        templateEditor.setLabel(constants.templateSubVersion());

        oSTypeEditor.setLabel(constants.osVmPopup());
        vmTypeEditor.setLabel(constants.optimizedFor());
        isStatelessEditor.setLabel(constants.statelessVmPopup());
        isRunAndPauseEditor.setLabel(constants.runAndPauseVmPopup());
        isDeleteProtectedEditor.setLabel(constants.deleteProtectionPopup());
        isConsoleDeviceEnabledEditor.setLabel(constants.consoleDeviceEnabled());
        copyTemplatePermissionsEditor.setLabel(constants.copyTemplatePermissions());
        isSmartcardEnabledEditor.setLabel(constants.smartcardVmPopup());
        isMemoryBalloonDeviceEnabled.setLabel(constants.memoryBalloonDeviceEnabled());
        isVirtioScsiEnabled.setLabel(constants.isVirtioScsiEnabled());

        // Pools Tab
        poolTab.setLabel(constants.poolVmPopup());
        poolTypeEditor.setLabel(constants.poolTypeVmPopup());
        editPrestartedVmsLabel.setText(constants.prestartedVms());

        prestartedLabel.setText(constants.prestartedPoolPopup());
        numOfVmsEditor.setLabel(constants.numOfVmsPoolPopup());
        maxAssignedVmsPerUserEditor.setLabel(constants.maxAssignedVmsPerUser());
        editMaxAssignedVmsPerUserEditor.setLabel(constants.maxAssignedVmsPerUser());

        // initial run Tab
        initialRunTab.setLabel(constants.initialRunVmPopup());
        domainEditor.setLabel(constants.domainVmPopup());
        timeZoneEditor.setLabel(constants.tzVmPopup());

        vmInitEnabledEditor.setLabel(constants.cloudInitOrSysprep());

        // Console Tab
        consoleTab.setLabel(constants.consoleVmPopup());
        displayProtocolEditor.setLabel(constants.protocolVmPopup());
        vncKeyboardLayoutEditor.setLabel(constants.vncKeyboardLayoutVmPopup());
        usbSupportEditor.setLabel(constants.usbPolicyVmPopup());
        numOfMonitorsEditor.setLabel(constants.monitorsVmPopup());
        allowConsoleReconnectEditor.setLabel(constants.allowConsoleReconnect());
        isSoundcardEnabledEditor.setLabel(constants.soundcardEnabled());
        isSingleQxlEnabledEditor.setLabel(constants.singleQxlEnabled());
        ssoMethodNone.setLabel(constants.none());
        ssoMethodGuestAgent.setLabel(constants.guestAgent());
        spiceProxyEditor.setLabel(constants.overriddenSpiceProxyAddress());

        // Host Tab
        hostTab.setLabel(constants.hostVmPopup());
        isAutoAssignEditor.setLabel(constants.anyHostInClusterVmPopup());
        // specificHostEditor.setLabel("Specific");
        hostCpuEditor.setLabel(constants.useHostCpu());
        cpuPinning.setLabel(constants.cpuPinningLabel());

        // High Availability Tab
        isHighlyAvailableEditor.setLabel(constants.highlyAvailableVmPopup());

        // watchdog
        watchdogActionEditor.setLabel(constants.watchdogAction());
        watchdogModelEditor.setLabel(constants.watchdogModel());

        // Resource Allocation Tab
        provisioningEditor.setLabel(constants.templateProvisVmPopup());
        provisioningThinEditor.setLabel(constants.thinVmPopup());
        provisioningCloneEditor.setLabel(constants.cloneVmPopup());
        minAllocatedMemoryEditor.setLabel(constants.physMemGuarVmPopup());

        // Boot Options
        firstBootDeviceEditor.setLabel(constants.firstDeviceVmPopup());
        secondBootDeviceEditor.setLabel(constants.secondDeviceVmPopup());
        kernel_pathEditor.setLabel(constants.kernelPathVmPopup());
        initrd_pathEditor.setLabel(constants.initrdPathVmPopup());
        kernel_parametersEditor.setLabel(constants.kernelParamsVmPopup());

        // System tab
        memSizeEditor.setLabel(constants.memSizeVmPopup());
        corePerSocketEditor.setLabel(constants.coresPerSocket());
        numOfSocketsEditor.setLabel(constants.numOfSockets());
    }

    protected void applyStyles() {
        hostCpuEditor.addContentWidgetStyleName(style.longCheckboxContent());
        allowConsoleReconnectEditor.addContentWidgetStyleName(style.longCheckboxContent());
        provisioningEditor.addContentWidgetStyleName(style.provisioningEditorContent());
        provisioningThinEditor.addContentWidgetStyleName(style.provisioningRadioContent());
        provisioningCloneEditor.addContentWidgetStyleName(style.provisioningRadioContent());
        cdAttachedEditor.addContentWidgetStyleName(style.cdAttachedLabelWidth());
        cdImageEditor.addContentWidgetStyleName(style.cdImageEditor());
        numOfMonitorsEditor.addContentWidgetStyleName(style.monitorsStyles());
        numOfMonitorsEditor.setStyleName(style.monitorsStyles());
        numOfMonitorsEditor.hideLabel();
        migrationModeEditor.addContentWidgetStyleName(style.migrationSelectorInner());
        isVirtioScsiEnabled.addContentWidgetStyleName(style.isVirtioScsiEnabledEditor());
    }

    @Override
    public void edit(UnitVmModel model) {
        super.edit(model);
        unitVmModel = model;

        priorityEditor.setRowData(new ArrayList<EntityModel>());
        priorityEditor.asEditor().edit(model.getPriority());
        driver.edit(model);
        profilesInstanceTypeEditor.edit(model.getNicsWithLogicalNetworks());
        customPropertiesSheetEditor.edit(model.getCustomPropertySheet());
        vmInitEditor.edit(model.getVmInitModel());
        serialNumberPolicyEditor.edit(model.getSerialNumberPolicy());
        initTabAvailabilityListeners(model);
        initListeners(model);
        hideAlwaysHiddenFields();
    }

    @UiHandler("refreshButton")
    void handleRefreshButtonClick(ClickEvent event) {
        unitVmModel.getBehavior().refreshCdImages();
    }

    protected void setupCustomPropertiesAvailability(UnitVmModel model) {
        changeApplicationLevelVisibility(customPropertiesTab, model.getIsCustomPropertiesTabAvailable());
    }

    protected void initListeners(final UnitVmModel object) {
        // TODO should be handled by the core framework
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;
                if ("IsHostTabValid".equals(propName)) { //$NON-NLS-1$
                    if (object.getIsHostTabValid()) {
                        hostTab.markAsValid();
                    } else {
                        hostTab.markAsInvalid(null);
                    }
                } else if ("IsCustomPropertiesTabAvailable".equals(propName)) { //$NON-NLS-1$
                    setupCustomPropertiesAvailability(object);
                } else if ("IsDisksAvailable".equals(propName)) { //$NON-NLS-1$
                    addDiskAllocation(object);
                }
            }
        });

        object.getIsAutoAssign().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isAutoAssign = object.getIsAutoAssign().getEntity();
                defaultHostEditor.setEnabled(!isAutoAssign);

                // only this is not bind to the model, so needs to listen to the change explicitly
                specificHost.setValue(!isAutoAssign);
            }
        });

        object.getProvisioning().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isProvisioningChangable = object.getProvisioning().getIsChangable();
                provisioningThinEditor.setEnabled(isProvisioningChangable);
                provisioningCloneEditor.setEnabled(isProvisioningChangable);

                boolean isProvisioningAvailable = object.getProvisioning().getIsAvailable();
                changeApplicationLevelVisibility(provisionSelectionPanel, isProvisioningAvailable);

                boolean isDisksAvailable = object.getIsDisksAvailable();
                changeApplicationLevelVisibility(disksAllocationPanel, isDisksAvailable);

                changeApplicationLevelVisibility(storageAllocationPanel, isProvisioningAvailable || isDisksAvailable ||
                    object.getIsVirtioScsiEnabled().getIsAvailable());
            }
        });

        object.getIsVirtioScsiEnabled().getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs e = (PropertyChangedEventArgs) args;

                if (e.propertyName == "IsAvailable") { //$NON-NLS-1$
                    isVirtioScsiEnabledInfoIcon.setVisible(object.getIsVirtioScsiEnabled().getIsAvailable());
                }
            }
        });

        object.getUsbPolicy().getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs e = (PropertyChangedEventArgs) args;

                if (e.propertyName == "SelectedItem") { //$NON-NLS-1$
                    updateUsbNativeMessageVisibility(object);
                }
            }
        });

        updateUsbNativeMessageVisibility(object);

        object.getEditingEnabled().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                Boolean enabled = object.getEditingEnabled().getEntity();
                if (Boolean.FALSE.equals(enabled)) {
                    disableAllTabs();
                    generalWarningMessage.setText(object.getEditingEnabled().getMessage());
                }
            }
        });

        object.getCpuSharesAmountSelection().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if ("IsAvailable".equals(((PropertyChangedEventArgs) args).propertyName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(cpuSharesEditor, object.getCpuSharesAmountSelection().getIsAvailable());
                }
            }
        });

        object.getCloudInitEnabled().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (object.getCloudInitEnabled().getEntity() != null) {
                    vmInitEditor.setCloudInitContentVisible(object.getCloudInitEnabled().getEntity());
                }
            }
        });

        object.getSysprepEnabled().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (object.getSysprepEnabled().getEntity() != null) {
                    boolean sysprepEnabled = object.getSysprepEnabled().getEntity();
                    vmInitEditor.setSyspepContentVisible(object.getSysprepEnabled().getEntity());
                    domainEditor.setVisible(sysprepEnabled);
                }
            }
        });

        object.getDataCenterWithClustersList().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                VDSGroup vdsGroup = object.getSelectedCluster();
                if (vdsGroup != null && vdsGroup.getcompatibility_version() != null) {
                    boolean enabled = AsyncDataProvider.isSerialNumberPolicySupported(vdsGroup.getcompatibility_version().getValue());
                    changeApplicationLevelVisibility(serialNumberPolicyEditor, enabled);
                }
            }
        });
    }

    /**
     * This raises a warning for USB devices that won't persist a VM migration when using Native USB with SPICE in
     * certain, configurable cluster version.
     */
    protected void updateUsbNativeMessageVisibility(final UnitVmModel object) {
        VDSGroup vdsGroup = object.getSelectedCluster();
        changeApplicationLevelVisibility(nativeUsbWarningMessage,
                object.getUsbPolicy().getSelectedItem() == UsbPolicy.ENABLED_NATIVE
                        && vdsGroup != null
                        && vdsGroup.getcompatibility_version() != null
                        && !(Boolean) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.MigrationSupportForNativeUsb,
                                vdsGroup.getcompatibility_version().getValue()));
    }

    private void addDiskAllocation(UnitVmModel model) {
        if (!model.getIsDisksAvailable()) {
            return;
        }
        disksAllocationView.edit(model.getDisksAllocationModel());
        model.getDisksAllocationModel().setDisks(model.getDisks());
    }

    private void initTabAvailabilityListeners(final UnitVmModel vm) {
        // TODO should be handled by the core framework
        vm.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;
                if ("IsWindowsOS".equals(propName)) { //$NON-NLS-1$
                    domainEditor.setEnabled(vm.getIsWindowsOS());
                } else if ("IsGeneralTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsGeneralTabValid()) {
                        generalTab.markAsValid();
                    } else {
                        generalTab.markAsInvalid(null);
                    }
                } else if ("IsSystemTabValid".equals(propName)) {  //$NON-NLS-1$
                    if (vm.getIsSystemTabValid()) {
                        systemTab.markAsValid();
                    } else {
                        systemTab.markAsInvalid(null);
                    }
                } else if ("IsDisplayTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsDisplayTabValid()) {
                        consoleTab.markAsValid();
                    } else {
                        consoleTab.markAsInvalid(null);
                    }
                } else if ("IsAllocationTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsAllocationTabValid()) {
                        resourceAllocationTab.markAsValid();
                    } else {
                        resourceAllocationTab.markAsInvalid(null);
                    }
                } else if ("IsHighlyAvailable".equals(propName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(highAvailabilityTab, vm.getIsHighlyAvailable().getEntity());
                } else if ("IsBootSequenceTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsHighlyAvailable().getEntity()) {
                        bootOptionsTab.markAsValid();
                    } else {
                        bootOptionsTab.markAsInvalid(null);
                    }
                } else if ("IsCustomPropertiesTabValid".equals(propName)) { //$NON-NLS-1$
                    if (vm.getIsCustomPropertiesTabValid()) {
                        customPropertiesTab.markAsValid();
                    } else {
                        customPropertiesTab.markAsInvalid(null);
                    }
                }
                else if ("IsDisksAvailable".equals(propName)) { //$NON-NLS-1$
                    boolean isDisksAvailable = vm.getIsDisksAvailable();
                    changeApplicationLevelVisibility(disksAllocationPanel, isDisksAvailable);

                    boolean isProvisioningAvailable = vm.getProvisioning().getIsAvailable();
                    changeApplicationLevelVisibility(storageAllocationPanel, isProvisioningAvailable
                            || isDisksAvailable || vm.getIsVirtioScsiEnabled().getIsAvailable());

                    if (isDisksAvailable) {
                        // Update warning message by disks status
                        updateDisksWarningByImageStatus(vm.getDisks(), ImageStatus.ILLEGAL);
                        updateDisksWarningByImageStatus(vm.getDisks(), ImageStatus.LOCKED);
                    }
                    else {
                        // Clear warning message
                        generalWarningMessage.setText(""); //$NON-NLS-1$
                    }
                }
            }
        });

        // TODO: Move to a more appropriate method
        vm.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;
                if ("IsLinuxOS".equals(propName)) { //$NON-NLS-1$
                    changeApplicationLevelVisibility(linuxBootOptionsPanel, vm.getIsLinuxOS());
                }
            }
        });

        defaultHostEditor.setEnabled(false);
        specificHost.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                defaultHostEditor.setEnabled(specificHost.getValue());
                ValueChangeEvent.fire(isAutoAssignEditor.asRadioButton(), false);
            }
        });

        // TODO: This is a hack and should be handled cleanly via model property availability
        isAutoAssignEditor.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                defaultHostEditor.setEnabled(false);
            }
        }, ClickEvent.getType());

        vm.getIsAutoAssign().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (!isAutoAssignEditor.asRadioButton().getValue())
                    specificHost.setValue(true, true);
            }
        });

        ssoMethodGuestAgent.asRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (Boolean.TRUE.equals(event.getValue())) {
                    ValueChangeEvent.fire(ssoMethodNone.asRadioButton(), false);
                }
            }
        });

        ssoMethodNone.asRadioButton().addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (Boolean.TRUE.equals(event.getValue())) {
                    ValueChangeEvent.fire(ssoMethodGuestAgent.asRadioButton(), false);
                }
            }
        });
    }

    private void updateDisksWarningByImageStatus(List<DiskModel> disks, ImageStatus imageStatus) {
        ArrayList<String> disksAliases =
                getDisksAliasesByImageStatus(disks, imageStatus);

        if (!disksAliases.isEmpty()) {
            generalWarningMessage.setText(messages.disksStatusWarning(
                    EnumTranslator.createAndTranslate(imageStatus),
                    (StringUtils.join(disksAliases, ", ")))); //$NON-NLS-1$
        }
    }

    private ArrayList<String> getDisksAliasesByImageStatus(List<DiskModel> disks, ImageStatus status) {
        ArrayList<String> disksAliases = new ArrayList<String>();

        if (disks == null) {
            return disksAliases;
        }

        for (DiskModel diskModel : disks) {
            Disk disk = diskModel.getDisk();
            if (disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                    ((DiskImage) disk).getImageStatus() == status) {

                disksAliases.add(disk.getDiskAlias());
            }
        }

        return disksAliases;
    }

    @Override
    public UnitVmModel flush() {
        priorityEditor.flush();
        profilesInstanceTypeEditor.flush();
        vmInitEditor.flush();
        serialNumberPolicyEditor.flush();
        return driver.flush();
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    public interface ButtonCellTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/ButtonCellTable.css" })
        TableStyle cellTableStyle();
    }

    @Override
    public int setTabIndexes(int nextTabIndex) {
        // ==General Tab==
        nextTabIndex = generalTab.setTabIndexes(nextTabIndex);
        quotaEditor.setTabIndex(nextTabIndex++);
        oSTypeEditor.setTabIndex(nextTabIndex++);
        baseTemplateEditor.setTabIndex(nextTabIndex++);
        templateEditor.setTabIndex(nextTabIndex++);

        nameEditor.setTabIndex(nextTabIndex++);
        templateVersionNameEditor.setTabIndex(nextTabIndex++);
        descriptionEditor.setTabIndex(nextTabIndex++);
        commentEditor.setTabIndex(nextTabIndex++);
        isStatelessEditor.setTabIndex(nextTabIndex++);
        isRunAndPauseEditor.setTabIndex(nextTabIndex++);
        isDeleteProtectedEditor.setTabIndex(nextTabIndex++);
        copyTemplatePermissionsEditor.setTabIndex(nextTabIndex++);

        numOfVmsEditor.setTabIndex(nextTabIndex++);
        prestartedVmsEditor.setTabIndex(nextTabIndex++);
        editPrestartedVmsEditor.setTabIndex(nextTabIndex++);
        incraseNumOfVmsEditor.setTabIndex(nextTabIndex++);
        maxAssignedVmsPerUserEditor.setTabIndex(nextTabIndex++);
        editMaxAssignedVmsPerUserEditor.setTabIndex(nextTabIndex++);

        // ==System Tab==
        nextTabIndex = systemTab.setTabIndexes(nextTabIndex);
        memSizeEditor.setTabIndex(nextTabIndex++);
        totalvCPUsEditor.setTabIndex(nextTabIndex++);

        nextTabIndex = vcpusAdvancedParameterExpander.setTabIndexes(nextTabIndex);
        corePerSocketEditor.setTabIndex(nextTabIndex++);
        numOfSocketsEditor.setTabIndex(nextTabIndex++);
        nextTabIndex = serialNumberPolicyEditor.setTabIndexes(nextTabIndex);

        // == Pools ==
        nextTabIndex = poolTab.setTabIndexes(nextTabIndex);
        poolTypeEditor.setTabIndex(nextTabIndex++);

        // ==Initial run Tab==
        nextTabIndex = initialRunTab.setTabIndexes(nextTabIndex);
        timeZoneEditor.setTabIndex(nextTabIndex++);
        domainEditor.setTabIndex(nextTabIndex++);

        // ==Console Tab==
        nextTabIndex = consoleTab.setTabIndexes(nextTabIndex);
        displayProtocolEditor.setTabIndex(nextTabIndex++);
        vncKeyboardLayoutEditor.setTabIndex(nextTabIndex++);
        usbSupportEditor.setTabIndex(nextTabIndex++);
        isSingleQxlEnabledEditor.setTabIndex(nextTabIndex++);
        numOfMonitorsEditor.setTabIndex(nextTabIndex++);
        isSmartcardEnabledEditor.setTabIndex(nextTabIndex++);
        ssoMethodNone.setTabIndex(nextTabIndex++);
        ssoMethodGuestAgent.setTabIndex(nextTabIndex++);
        nextTabIndex = expander.setTabIndexes(nextTabIndex);
        allowConsoleReconnectEditor.setTabIndex(nextTabIndex++);
        isSoundcardEnabledEditor.setTabIndex(nextTabIndex++);
        isConsoleDeviceEnabledEditor.setTabIndex(nextTabIndex++);
        spiceProxyOverrideEnabledEditor.setTabIndex(nextTabIndex++);
        spiceProxyEditor.setTabIndex(nextTabIndex++);

        // ==Host Tab==
        nextTabIndex = hostTab.setTabIndexes(nextTabIndex);
        isAutoAssignEditor.setTabIndex(nextTabIndex++);
        specificHost.setTabIndex(nextTabIndex++);
        defaultHostEditor.setTabIndex(nextTabIndex++);
        migrationModeEditor.setTabIndex(nextTabIndex++);
        overrideMigrationDowntimeEditor.setTabIndex(nextTabIndex++);
        migrationDowntimeEditor.setTabIndex(nextTabIndex++);
        hostCpuEditor.setTabIndex(nextTabIndex++);

        // ==High Availability Tab==
        nextTabIndex = highAvailabilityTab.setTabIndexes(nextTabIndex);
        isHighlyAvailableEditor.setTabIndex(nextTabIndex++);
        priorityEditor.setTabIndex(nextTabIndex++);

        watchdogModelEditor.setTabIndex(nextTabIndex++);
        watchdogActionEditor.setTabIndex(nextTabIndex++);

        // ==Resource Allocation Tab==
        nextTabIndex = resourceAllocationTab.setTabIndexes(nextTabIndex);
        minAllocatedMemoryEditor.setTabIndex(nextTabIndex++);
        provisioningEditor.setTabIndex(nextTabIndex++);
        provisioningThinEditor.setTabIndex(nextTabIndex++);
        provisioningCloneEditor.setTabIndex(nextTabIndex++);
        cpuPinning.setTabIndex(nextTabIndex++);
        cpuSharesAmountEditor.setTabIndex(nextTabIndex++);
        nextTabIndex = disksAllocationView.setTabIndexes(nextTabIndex);

        // ==Boot Options Tab==
        nextTabIndex = bootOptionsTab.setTabIndexes(nextTabIndex);
        firstBootDeviceEditor.setTabIndex(nextTabIndex++);
        secondBootDeviceEditor.setTabIndex(nextTabIndex++);
        cdAttachedEditor.setTabIndex(nextTabIndex++);
        cdImageEditor.setTabIndex(nextTabIndex++);
        kernel_pathEditor.setTabIndex(nextTabIndex++);
        initrd_pathEditor.setTabIndex(nextTabIndex++);
        kernel_parametersEditor.setTabIndex(nextTabIndex++);

        // ==Custom Properties Tab==
        nextTabIndex = customPropertiesTab.setTabIndexes(nextTabIndex);

        return nextTabIndex;
    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        return super.createWidgetConfiguration().
                putAll(allTabs(), simpleField().visibleInAdvancedModeOnly()).
                putAll(adancedFieldsFromGeneralTab(), simpleField().visibleInAdvancedModeOnly()).
                putAll(consoleTabWidgets(), simpleField().visibleInAdvancedModeOnly()).
                update(consoleTab, simpleField()).
                update(numOfMonitorsEditor, simpleField()).
                update(isSingleQxlEnabledEditor, simpleField()).
                putOne(isSoundcardEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(isConsoleDeviceEnabledEditor, simpleField().visibleInAdvancedModeOnly());
    }

    protected List<Widget> consoleTabWidgets() {
        return Arrays.<Widget> asList(
                displayProtocolEditor,
                usbSupportEditor,
                isSmartcardEnabledEditor,
                nativeUsbWarningMessage,
                expander,
                numOfMonitorsEditor,
                vncKeyboardLayoutEditor,
                ssoMethodLabel,
                ssoMethodNone,
                ssoMethodGuestAgent
        );
    }

    protected List<Widget> poolSpecificFields() {
        return Arrays.<Widget> asList(numOfVmsEditor,
                newPoolEditVmsPanel,
                editPoolEditVmsPanel,
                editPoolIncraseNumOfVmsPanel,
                poolTab,
                prestartedVmsEditor,
                poolNameIcon,
                newPoolEditMaxAssignedVmsPerUserPanel,
                editPoolEditMaxAssignedVmsPerUserPanel,
                spiceProxyEditor,
                spiceProxyEnabledCheckboxWithInfoIcon,
                spiceProxyOverrideEnabledEditor);
    }

    protected List<Widget> allTabs() {
        return Arrays.<Widget> asList(initialRunTab,
                consoleTab,
                hostTab,
                resourceAllocationTab,
                bootOptionsTab,
                customPropertiesTab,
                highAvailabilityTab,
                poolTab,
                systemTab);
    }

    protected List<Widget> adancedFieldsFromGeneralTab() {
        return Arrays.<Widget> asList(
                memSizeEditor,
                totalvCPUsEditor,
                vcpusAdvancedParameterExpander,
                copyTemplatePermissionsEditor
                );
    }

    protected void disableAllTabs() {
        generalTab.disableContent();
        poolTab.disableContent();
        initialRunTab.disableContent();
        consoleTab.disableContent();
        hostTab.disableContent();
        highAvailabilityTab.disableContent();
        resourceAllocationTab.disableContent();
        bootOptionsTab.disableContent();
        customPropertiesTab.disableContent();
        systemTab.disableContent();
        oSTypeEditor.setEnabled(false);
        quotaEditor.setEnabled(false);
        dataCenterWithClusterEditor.setEnabled(false);
        templateEditor.setEnabled(false);
        baseTemplateEditor.setEnabled(false);
        vmTypeEditor.setEnabled(false);
    }

}
