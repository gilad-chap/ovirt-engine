package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.vm.SerialNumberPolicyVmBaseToUnitBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.pools.PoolModel;
import org.ovirt.engine.ui.uicommonweb.validation.HostWithProtocolAndPortAddressValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public abstract class PoolModelBehaviorBase extends VmModelBehaviorBase<PoolModel> {

    private Event poolModelBehaviorInitializedEvent = new Event("PoolModelBehaviorInitializedEvent", //$NON-NLS-1$
            NewPoolModelBehavior.class);

    public Event getPoolModelBehaviorInitializedEvent()
    {
        return poolModelBehaviorInitializedEvent;
    }

    @Override
    public void initialize(SystemTreeItemModel systemTreeSelectedItem)
    {
        super.initialize(systemTreeSelectedItem);

        getModel().getIsSoundcardEnabled().setIsChangable(true);

        getModel().getDisksAllocationModel().setIsVolumeFormatAvailable(false);
        getModel().getDisksAllocationModel().setIsAliasChangable(true);

        getModel().getProvisioning().setIsAvailable(false);
        getModel().getProvisioning().setEntity(false);

        AsyncDataProvider.getDataCenterByClusterServiceList(new AsyncQuery(getModel(), new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {

                final List<StoragePool> dataCenters = new ArrayList<StoragePool>();
                for (StoragePool a : (ArrayList<StoragePool>) returnValue) {
                    if (a.getStatus() == StoragePoolStatus.Up) {
                        dataCenters.add(a);
                    }
                }

                if (!dataCenters.isEmpty()) {
                    postDataCentersLoaded(dataCenters);
                } else {
                    getModel().disableEditing(ConstantsManager.getInstance().getConstants().notAvailableWithNoUpDC());
                }


            }
        }, getModel().getHash()), true, false);

        getModel().getSpiceProxyEnabled().setEntity(false);
        getModel().getSpiceProxy().setIsChangable(false);

        getModel().getSpiceProxyEnabled().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getModel().getSpiceProxy().setIsChangable(getModel().getSpiceProxyEnabled().getEntity());
            }
        });
    }

    protected void postDataCentersLoaded(final List<StoragePool> dataCenters) {
        AsyncDataProvider.getClusterListByService(
                new AsyncQuery(getModel(), new INewAsyncCallback() {

                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        UnitVmModel model = (UnitVmModel) target;
                        List<VDSGroup> clusters = (List<VDSGroup>) returnValue;
                        List<VDSGroup> filteredClusters = filterClusters(clusters);
                        model.setDataCentersAndClusters(model,
                                dataCenters,
                                filteredClusters, null);
                        initCdImage();
                        getPoolModelBehaviorInitializedEvent().raise(this, EventArgs.EMPTY);
                    }
                }, getModel().getHash()),
                true, false);
    }

    protected abstract List<VDSGroup> filterClusters(List<VDSGroup> clusters);

    protected void setupWindowModelFrom(VmBase vmBase) {
        if (vmBase != null) {
            updateQuotaByCluster(vmBase.getQuotaId(), vmBase.getQuotaName());

            // Copy VM parameters from template.
            getModel().getVmType().setSelectedItem(vmBase.getVmType());
            setSelectedOSType(vmBase, getModel().getSelectedCluster().getArchitecture());
            getModel().getTotalCPUCores().setEntity(Integer.toString(vmBase.getNumOfCpus()));
            getModel().getNumOfSockets().setSelectedItem(vmBase.getNumOfSockets());
            getModel().getNumOfMonitors().setSelectedItem(vmBase.getNumOfMonitors());
            getModel().getIsSingleQxlEnabled().setEntity(vmBase.getSingleQxlPci());
            getModel().getMemSize().setEntity(vmBase.getMemSizeMb());
            getModel().setBootSequence(vmBase.getDefaultBootSequence());
            getModel().getIsHighlyAvailable().setEntity(vmBase.isAutoStartup());
            getModel().getIsDeleteProtected().setEntity(vmBase.isDeleteProtected());
            getModel().selectSsoMethod(vmBase.getSsoMethod());
            getModel().getIsRunAndPause().setEntity(false);

            boolean hasCd = !StringHelper.isNullOrEmpty(vmBase.getIsoPath());

            getModel().getCdImage().setIsChangable(hasCd);
            getModel().getCdAttached().setEntity(hasCd);
            if (hasCd) {
                getModel().getCdImage().setSelectedItem(vmBase.getIsoPath());
            }

            updateTimeZone(vmBase.getTimeZone());
            updateConsoleDevice(vmBase.getId());
            updateVirtioScsiEnabled(vmBase.getId(), vmBase.getOsId());

            // Update domain list
            updateDomain();

            // Update display protocol selected item
            EntityModel<DisplayType> displayProtocol = null;
            boolean isFirst = true;
            for (EntityModel<DisplayType> item : getModel().getDisplayProtocol().getItems())
            {
                if (isFirst)
                {
                    displayProtocol = item;
                    isFirst = false;
                }
                DisplayType dt = item.getEntity();
                if (dt == extractDisplayType(vmBase))
                {
                    displayProtocol = item;
                    break;
                }
            }
            getModel().getDisplayProtocol().setSelectedItem(displayProtocol);
            getModel().getUsbPolicy().setSelectedItem(vmBase.getUsbPolicy());
            getModel().getVncKeyboardLayout().setSelectedItem(vmBase.getVncKeyboardLayout());
            getModel().getIsSmartcardEnabled().setEntity(vmBase.isSmartcardEnabled());
            getModel().setSelectedMigrationDowntime(vmBase.getMigrationDowntime());

            // By default, take kernel params from template.
            getModel().getKernel_path().setEntity(vmBase.getKernelUrl());
            getModel().getKernel_parameters().setEntity(vmBase.getKernelParams());
            getModel().getInitrd_path().setEntity(vmBase.getInitrdUrl());

            if (!vmBase.getId().equals(Guid.Empty))
            {
                getModel().getStorageDomain().setIsChangable(true);

                getModel().setIsBlankTemplate(false);
                initDisks();
            }
            else
            {
                getModel().getStorageDomain().setIsChangable(false);

                getModel().setIsBlankTemplate(true);
                getModel().setIsDisksAvailable(false);
                getModel().setDisks(null);
            }

            getModel().getProvisioning().setEntity(false);

            initPriority(vmBase.getPriority());
            initStorageDomains();

            // use min. allocated memory from the template, if specified
            if (vmBase.getMinAllocatedMem() == 0) {
                updateMinAllocatedMemory();
            } else {
                getModel().getMinAllocatedMemory().setEntity(vmBase.getMinAllocatedMem());
            }

            initSoundCard(vmBase.getId());

            getModel().getAllowConsoleReconnect().setEntity(vmBase.isAllowConsoleReconnect());

            getModel().getVmInitModel().init(vmBase);
            getModel().getVmInitEnabled().setEntity(vmBase.getVmInit() != null);

            BuilderExecutor.build(vmBase, getModel(), new SerialNumberPolicyVmBaseToUnitBuilder());
        }
    }

    @Override
    public void template_SelectedItemChanged() {
        // overrideSerialNumberPolicy if there is a need to do some actions
    }

    protected abstract DisplayType extractDisplayType(VmBase vmBase);

    @Override
    public void postDataCenterWithClusterSelectedItemChanged()
    {
        updateDefaultHost();
        updateCustomPropertySheet();
        updateMinAllocatedMemory();
        updateNumOfSockets();
        updateOSValues();

        if (getModel().getTemplate().getSelectedItem() != null) {
            VmTemplate template = getModel().getTemplate().getSelectedItem();
            updateQuotaByCluster(template.getQuotaId(), template.getQuotaName());
        }
        updateMemoryBalloon();
        updateCpuSharesAvailability();
        updateVirtioScsiAvailability();
    }

    @Override
    public void defaultHost_SelectedItemChanged()
    {
        updateCdImage();
    }

    @Override
    public void provisioning_SelectedItemChanged()
    {
    }

    @Override
    public void oSType_SelectedItemChanged() {
        VmTemplate template = getModel().getTemplate().getSelectedItem();
        Integer osType = getModel().getOSType().getSelectedItem();
        if (template != null && osType != null) {
            updateVirtioScsiEnabled(template.getId(), osType);
        }
    }

    @Override
    public void updateMinAllocatedMemory()
    {
        VDSGroup cluster = (VDSGroup) getModel().getSelectedCluster();
        if (cluster == null)
        {
            return;
        }

        double overCommitFactor = 100.0 / cluster.getmax_vds_memory_over_commit();
        getModel().getMinAllocatedMemory()
                .setEntity((int) (getModel().getMemSize().getEntity() * overCommitFactor));
    }

    public void initCdImage()
    {
        updateCdImage();
    }

    @Override
    public void updateIsDisksAvailable()
    {
        getModel().setIsDisksAvailable(getModel().getDisks() != null
                && getModel().getProvisioning().getIsChangable());
    }

    @Override
    public boolean validate() {
        boolean isNew = getModel().getIsNew();
        int maxAllowedVms = getMaxVmsInPool();
        int assignedVms = getModel().getAssignedVms().asConvertible().integer();

        getModel().getNumOfDesktops().validateEntity(

                new IValidation[]
                {
                        new NotEmptyValidation(),
                        new LengthValidation(4),
                        new IntegerValidation(isNew ? 1 : 0, isNew ? maxAllowedVms : maxAllowedVms - assignedVms)
                });

        getModel().getPrestartedVms().validateEntity(
                new IValidation[]
                {
                        new NotEmptyValidation(),
                        new IntegerValidation(0, assignedVms)
                });

        getModel().getMaxAssignedVmsPerUser().validateEntity(
                new IValidation[]
                {
                        new NotEmptyValidation(),
                        new IntegerValidation(1, Short.MAX_VALUE)
                });

        getModel().setIsGeneralTabValid(getModel().getIsGeneralTabValid()
                && getModel().getName().getIsValid()
                && getModel().getNumOfDesktops().getIsValid()
                && getModel().getPrestartedVms().getIsValid()
                && getModel().getMaxAssignedVmsPerUser().getIsValid());

        getModel().setIsPoolTabValid(true);

        if (getModel().getSpiceProxyEnabled().getEntity()) {
            getModel().getSpiceProxy().validateEntity(new IValidation[]{ new HostWithProtocolAndPortAddressValidation()});
        } else {
            getModel().getSpiceProxy().setIsValid(true);
        }

        return super.validate()
                && getModel().getName().getIsValid()
                && getModel().getNumOfDesktops().getIsValid()
                && getModel().getPrestartedVms().getIsValid()
                && getModel().getMaxAssignedVmsPerUser().getIsValid()
                && getModel().getSpiceProxy().getIsValid();
    }
}
