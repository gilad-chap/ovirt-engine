package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaSanityParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.quota.QuotaVdsDependent;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.StoragePoolValidator;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.VmWatchdogValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddVmTemplateParameters;
import org.ovirt.engine.core.common.action.CreateImageTemplateParameters;
import org.ovirt.engine.core.common.action.UpdateVmVersionParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.PermissionDAO;
import org.ovirt.engine.core.utils.collections.MultiValueMapUtils;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@DisableInPrepareMode
@NonTransactiveCommandAttribute(forceCompensation = true)
@LockIdNameAttribute
public class AddVmTemplateCommand<T extends AddVmTemplateParameters> extends VmTemplateCommand<T>
        implements QuotaStorageDependent, QuotaVdsDependent {

    private final List<DiskImage> mImages = new ArrayList<DiskImage>();
    private List<PermissionSubject> permissionCheckSubject;
    protected Map<Guid, DiskImage> diskInfoDestinationMap;
    protected Map<Guid, List<DiskImage>> sourceImageDomainsImageMap;
    private boolean isVmInDb;
    private boolean pendingAsyncTasks;

    private static final String BASE_TEMPLATE_VERSION_NAME = "base version";
    private static Map<Guid, String> updateVmsJobIdMap = new ConcurrentHashMap<Guid, String>();

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected AddVmTemplateCommand(Guid commandId) {
        super(commandId);
    }

    public AddVmTemplateCommand(T parameters) {
        super(parameters);
        super.setVmTemplateName(parameters.getName());
        VmStatic parameterMasterVm = parameters.getMasterVm();
        if (parameterMasterVm != null) {
            super.setVmId(parameterMasterVm.getId());
            setVdsGroupId(parameterMasterVm.getVdsGroupId());

            // API backward compatibility
            if (parameters.isSoundDeviceEnabled() == null) {
                parameters.setSoundDeviceEnabled(parameterMasterVm.getVmType() == VmType.Desktop);
            }

            if (getParameters().isConsoleEnabled() == null) {
                parameters.setConsoleEnabled(false);
            }
        }
        if (getVm() != null) {
            updateVmDevices();
            updateVmDisks();
            setStoragePoolId(getVm().getStoragePoolId());
            isVmInDb = true;
        } else if (getVdsGroup() != null && parameterMasterVm != null) {
            VM vm = new VM(parameterMasterVm, new VmDynamic(), null);
            vm.setDisplayType(parameterMasterVm.getDefaultDisplayType());
            vm.setVdsGroupCompatibilityVersion(getVdsGroup().getcompatibility_version());
            setVm(vm);
            setStoragePoolId(getVdsGroup().getStoragePoolId());
        }
        updateDiskInfoDestinationMap();
    }

    protected void updateDiskInfoDestinationMap() {
        diskInfoDestinationMap = getParameters().getDiskInfoDestinationMap();
        if (diskInfoDestinationMap == null) {
            diskInfoDestinationMap = new HashMap<Guid, DiskImage>();
        }
        sourceImageDomainsImageMap = new HashMap<Guid, List<DiskImage>>();
        for (DiskImage image : mImages) {
            MultiValueMapUtils.addToMap(image.getStorageIds().get(0), image, sourceImageDomainsImageMap);
            if (!diskInfoDestinationMap.containsKey(image.getId())) {
                diskInfoDestinationMap.put(image.getId(), image);
            }
        }
    }

    protected void updateVmDevices() {
        VmDeviceUtils.setVmDevices(getVm().getStaticData());
    }

    protected void updateVmDisks() {
        VmHandler.updateDisksFromDb(getVm());
        VmHandler.filterImageDisksForVM(getVm(), false, false, true);
        mImages.addAll(getVm().getDiskList());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            if (isVmInDb) {
                if (pendingAsyncTasks) {
                    return getSucceeded() ? AuditLogType.USER_ADD_VM_TEMPLATE : AuditLogType.USER_FAILED_ADD_VM_TEMPLATE;
                } else {
                    return getSucceeded() ? AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_SUCCESS : AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_FAILURE;
                }
            } else {
                return getSucceeded() ? AuditLogType.USER_ADD_VM_TEMPLATE_SUCCESS : AuditLogType.USER_ADD_VM_TEMPLATE_FAILURE;
            }

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_SUCCESS
                    : AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_FAILURE;

        default:
            return AuditLogType.USER_ADD_VM_TEMPLATE_FINISHED_FAILURE;
        }
    }

    @Override
    protected void buildChildCommandInfos() {
        Guid vmSnapshotId = Guid.newGuid();
        for (DiskImage diskImage : mImages) {
            addChildCommandInfo(diskImage.getImageId(), VdcActionType.CreateImageTemplate, buildChildCommandParameters(diskImage, vmSnapshotId));
        }
    }

    private CreateImageTemplateParameters buildChildCommandParameters(DiskImage diskImage, Guid vmSnapshotId) {
        CreateImageTemplateParameters createParams = new CreateImageTemplateParameters(diskImage.getImageId(),
                getVmTemplateId(), getVmTemplateName(), getVmId());
        createParams.setStorageDomainId(diskImage.getStorageIds().get(0));
        createParams.setVmSnapshotId(vmSnapshotId);
        createParams.setEntityInfo(getParameters().getEntityInfo());
        createParams.setDestinationStorageDomainId(diskInfoDestinationMap.get(diskImage.getId())
                .getStorageIds()
                .get(0));
        createParams.setDiskAlias(diskInfoDestinationMap.get(diskImage.getId()).getDiskAlias());
        createParams.setParentParameters(getParameters());
        createParams.setQuotaId(getQuotaIdForDisk(diskImage));
        return createParams;
    }

    @Override
    protected void executeCommand() {
        // get vm status from db to check its really down before locking
        // relevant only if template created from vm
        if (isVmInDb) {
            VmDynamic vmDynamic = DbFacade.getInstance().getVmDynamicDao().get(getVmId());
            if (vmDynamic.getStatus() != VMStatus.Down) {
                throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
            }

            VmHandler.lockVm(vmDynamic, getCompensationContext());
        }
        setActionReturnValue(Guid.Empty);
        setVmTemplateId(Guid.newGuid());
        getParameters().setVmTemplateId(getVmTemplateId());
        getParameters().setEntityInfo(new EntityInfo(VdcObjectType.VmTemplate, getVmTemplateId()));

        // set template id as base for new templates
        if (!isTemplateVersion()) {
            getParameters().setBaseTemplateId(getVmTemplateId());
            if (StringUtils.isEmpty(getParameters().getTemplateVersionName())) {
                getParameters().setTemplateVersionName(BASE_TEMPLATE_VERSION_NAME);
            }
        } else {
            String jobId = updateVmsJobIdMap.remove(getParameters().getBaseTemplateId());
            if (jobId != null) {
                log.infoFormat("Cancelling current running update for vms for base template id {0}", getParameters().getBaseTemplateId());
                try {
                    SchedulerUtilQuartzImpl.getInstance().deleteJob(jobId);
                } catch (Exception e) {
                    log.warnFormat("Failed deleting job {0} at cancelRecoveryJob", jobId);
                }
            }
        }

        final Map<Guid, Guid> srcDeviceIdToTargetDeviceIdMapping = new HashMap<>();

        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                addVmTemplateToDb();
                getCompensationContext().stateChanged();
                return null;
            }
        });
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                addPermission();
                addVmTemplateImages(srcDeviceIdToTargetDeviceIdMapping);
                addVmInterfaces(srcDeviceIdToTargetDeviceIdMapping);
                if (isVmInDb) {
                    VmDeviceUtils.copyVmDevices(getVmId(),
                            getVmTemplateId(),
                            srcDeviceIdToTargetDeviceIdMapping,
                            getParameters().isSoundDeviceEnabled(),
                            getParameters().isConsoleEnabled(),
                            getParameters().isVirtioScsiEnabled(),
                            VmDeviceUtils.isBalloonEnabled(getVmId()),
                            false);
                } else {
                    // sending true for isVm in order to create basic devices needed
                    VmDeviceUtils.copyVmDevices(getVmId(),
                            getVmTemplateId(),
                            getVm(),
                            getVmTemplate(),
                            true,
                            Collections.<VmDevice> emptyList(),
                            srcDeviceIdToTargetDeviceIdMapping,
                            getParameters().isSoundDeviceEnabled(),
                            getParameters().isConsoleEnabled(),
                            getParameters().isVirtioScsiEnabled(),
                            getVm().isBalloonEnabled(),
                            false);
                }

                setSucceeded(true);
                return null;
            }
        });

        VmHandler.warnMemorySizeLegal(getVmTemplate(), getVdsGroup().getcompatibility_version());

        // means that there are no asynchronous tasks to execute and that we can
        // end the command synchronously
        pendingAsyncTasks = !getReturnValue().getVdsmTaskIdList().isEmpty();
        if (!pendingAsyncTasks) {
            endSuccessfullySynchronous();
        }
    }

    @Override
    protected boolean canDoAction() {
        if (getVdsGroup() == null) {
            addCanDoActionMessage(VdcBllMessages.VDS_CLUSTER_IS_NOT_VALID);
            return false;
        }

        // A Template cannot be added in a cluster without a defined architecture
        if (getVdsGroup().getArchitecture() == ArchitectureType.undefined) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_CLUSTER_UNDEFINED_ARCHITECTURE);
        }

        if (!VmHandler.isOsTypeSupported(getParameters().getMasterVm().getOsId(),
                getVdsGroup().getArchitecture(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (!isVmPriorityValueLegal(getParameters().getMasterVm().getPriority(), getReturnValue()
                .getCanDoActionMessages())) {
            return false;
        }

        if (isVmInDb && getVm().getStatus() != VMStatus.Down) {
            addCanDoActionMessage(VdcBllMessages.VMT_CANNOT_CREATE_TEMPLATE_FROM_DOWN_VM.toString());
            return false;
        }

        if (isVmTemlateWithSameNameExist(getVmTemplateName())) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NAME_ALREADY_USED);
            return false;
        }

        // Check that the USB policy is legal
        if (!VmHandler.isUsbPolicyLegal(getParameters().getVm().getUsbPolicy(),
                getParameters().getVm().getOs(),
                getVdsGroup(),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // Check if the display type is supported
        if (!VmHandler.isDisplayTypeSupported(getParameters().getMasterVm().getOsId(),
                getParameters().getMasterVm().getDefaultDisplayType(),
                getReturnValue().getCanDoActionMessages(),
                getVdsGroup().getcompatibility_version())) {
            return false;
        }

        if (getParameters().getVm().getSingleQxlPci() &&
                !VmHandler.isSingleQxlDeviceLegal(getParameters().getVm().getDefaultDisplayType(),
                        getParameters().getVm().getOs(),
                        getReturnValue().getCanDoActionMessages(),
                        getVdsGroup().getcompatibility_version())) {
            return false;
        }

        if (Boolean.TRUE.equals(getParameters().isVirtioScsiEnabled()) &&
                !FeatureSupported.virtIoScsi(getVdsGroup().getcompatibility_version())) {
            return failCanDoAction(VdcBllMessages.VIRTIO_SCSI_INTERFACE_IS_NOT_AVAILABLE_FOR_CLUSTER_LEVEL);
        }

        // Check if the watchdog model is supported
        if (getParameters().getWatchdog() != null) {
            if (!validate((new VmWatchdogValidator(getParameters().getMasterVm().getOsId(),
                    getParameters().getWatchdog(),
                    getVdsGroup().getcompatibility_version())).isModelCompatibleWithOs())) {
                return false;
            }
        }

        if (isTemplateVersion()) {
            VmTemplate userSelectedBaseTemplate = getVmTemplateDAO().get(getParameters().getBaseTemplateId());
            if (userSelectedBaseTemplate == null) {
                return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
            } else if (!userSelectedBaseTemplate.isBaseTemplate()) {
                // currently template version cannot be base template
                return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_VERSION_CANNOT_BE_BASE_TEMPLATE);

            }
        }

        return imagesRelatedChecks() && AddVmCommand.checkCpuSockets(getParameters().getMasterVm().getNumOfSockets(),
                getParameters().getMasterVm().getCpuPerSocket(), getVdsGroup()
                .getcompatibility_version().toString(), getReturnValue().getCanDoActionMessages());
    }

    private boolean isTemplateVersion() {
        return getParameters().getBaseTemplateId() != null;
    }

    private boolean imagesRelatedChecks() {
        // images related checks
        if (!mImages.isEmpty()) {
            if (!getVm().getStoragePoolId().equals(getVdsGroup().getStoragePoolId())) {
                addCanDoActionMessage(VdcBllMessages.VDS_CLUSTER_IS_NOT_VALID);
                return false;
            }
            if (!validateVmNotDuringSnapshot()) {
                return false;
            }

            if (!validate(new StoragePoolValidator(getStoragePool()).isUp())) {
                return false;
            }

            List<DiskImage> diskImagesToCheck = ImagesHandler.filterImageDisks(mImages, true, false, true);
            DiskImagesValidator diskImagesValidator = new DiskImagesValidator(diskImagesToCheck);
            if (!validate(diskImagesValidator.diskImagesNotIllegal()) ||
                    !validate(diskImagesValidator.diskImagesNotLocked())) {
                return false;
            }

            MultipleStorageDomainsValidator storageDomainsValidator =
                    new MultipleStorageDomainsValidator(getStoragePoolId(), sourceImageDomainsImageMap.keySet());
            if (!validate(storageDomainsValidator.allDomainsExistAndActive())) {
                return false;
            }

            Map<Guid, StorageDomain> storageDomains = new HashMap<Guid, StorageDomain>();
            Set<Guid> destImageDomains = getStorageGuidSet();
            destImageDomains.removeAll(sourceImageDomainsImageMap.keySet());
            for (Guid destImageDomain : destImageDomains) {
                StorageDomain storage = DbFacade.getInstance().getStorageDomainDao().getForStoragePool(
                        destImageDomain, getVm().getStoragePoolId());
                if (storage == null) {
                    // if storage is null then we need to check if it doesn't exist or
                    // domain is not in the same storage pool as the vm
                    if (DbFacade.getInstance().getStorageDomainStaticDao().get(destImageDomain) == null) {
                        addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_NOT_EXIST.toString());
                    } else {
                        addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_NOT_IN_STORAGE_POOL);
                    }
                    return false;
                }
                if (storage.getStatus() == null || storage.getStatus() != StorageDomainStatus.Active) {
                    addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL.toString());
                    return false;
                }

                if (storage.getStorageDomainType().isIsoOrImportExportDomain()) {

                    addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_TYPE_ILLEGAL);
                    return false;
                }
                storageDomains.put(destImageDomain, storage);
            }
            // update vm snapshots for storage free space check
            ImagesHandler.fillImagesBySnapshots(getVm());

            Map<StorageDomain, Integer> domainMap =
                    StorageDomainValidator.getSpaceRequirementsForStorageDomains(
                            ImagesHandler.filterImageDisks(getVm().getDiskMap().values(), true, false, true),
                            storageDomains,
                            diskInfoDestinationMap);

            for (Map.Entry<StorageDomain, Integer> entry : domainMap.entrySet()) {
                if (!doesStorageDomainhaveSpaceForRequest(entry.getKey(), entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean doesStorageDomainhaveSpaceForRequest(StorageDomain storageDomain, long spaceForRequest) {
        return validate(new StorageDomainValidator(storageDomain).isDomainHasSpaceForRequest(spaceForRequest));
    }

    protected boolean validateVmNotDuringSnapshot() {
        return validate(new SnapshotsValidator().vmNotDuringSnapshot(getVmId()));
    }

    private Set<Guid> getStorageGuidSet() {
        Set<Guid> destImageDomains = new HashSet<Guid>();
        for (DiskImage diskImage : diskInfoDestinationMap.values()) {
            destImageDomains.add(diskImage.getStorageIds().get(0));
        }
        return destImageDomains;
    }

    protected void addVmTemplateToDb() {
        // TODO: add timezone handling
        setVmTemplate(
                new VmTemplate(
                        0,
                        new Date(),
                        getParameters().getDescription(),
                        getParameters().getMasterVm().getComment(),
                        getParameters().getMasterVm().getMemSizeMb(),
                        getVmTemplateName(),
                        getParameters().getMasterVm().getNumOfSockets(),
                        getParameters().getMasterVm().getCpuPerSocket(),
                        getParameters().getMasterVm().getOsId(),
                        getParameters().getMasterVm().getVdsGroupId(),
                        getVmTemplateId(),
                        getParameters().getMasterVm().getNumOfMonitors(),
                        getParameters().getMasterVm().getSingleQxlPci(),
                        VmTemplateStatus.Locked.getValue(),
                        getParameters().getMasterVm().getUsbPolicy().getValue(),
                        getParameters().getMasterVm().getTimeZone(),
                        getParameters().getMasterVm().getNiceLevel(),
                        getParameters().getMasterVm().getCpuShares(),
                        getParameters().getMasterVm().isFailBack(),
                        getParameters().getMasterVm().getDefaultBootSequence(),
                        getParameters().getMasterVm().getVmType(),
                        getParameters().getMasterVm().isSmartcardEnabled(),
                        getParameters().getMasterVm().isDeleteProtected(),
                        getParameters().getMasterVm().getSsoMethod(),
                        getParameters().getMasterVm().getTunnelMigration(),
                        getParameters().getMasterVm().getVncKeyboardLayout(),
                        getParameters().getMasterVm().getMinAllocatedMem(),
                        getParameters().getMasterVm().isStateless(),
                        getParameters().getMasterVm().isRunAndPause(),
                        getUserId(),
                        getParameters().getTemplateType(),
                        getParameters().getMasterVm().isAutoStartup(),
                        getParameters().getMasterVm().getPriority(),
                        getParameters().getMasterVm().getDefaultDisplayType(),
                        getParameters().getMasterVm().getInitrdUrl(),
                        getParameters().getMasterVm().getKernelUrl(),
                        getParameters().getMasterVm().getKernelParams(),
                        getParameters().getMasterVm().getQuotaId(),
                        getParameters().getMasterVm().getDedicatedVmForVds(),
                        getParameters().getMasterVm().getMigrationSupport(),
                        getParameters().getMasterVm().isAllowConsoleReconnect(),
                        getParameters().getMasterVm().getIsoPath(),
                        getParameters().getMasterVm().getMigrationDowntime(),
                        getParameters().getBaseTemplateId(),
                        getParameters().getTemplateVersionName(),
                        getParameters().getMasterVm().getSerialNumberPolicy(),
                        getParameters().getMasterVm().getCustomSerialNumber()));
        DbFacade.getInstance().getVmTemplateDao().save(getVmTemplate());
        getCompensationContext().snapshotNewEntity(getVmTemplate());
        setActionReturnValue(getVmTemplate().getId());
        // Load Vm Init from DB and set it to the template
        VmHandler.updateVmInitFromDB(getParameters().getMasterVm(), false);
        getVmTemplate().setVmInit(getParameters().getMasterVm().getVmInit());
        VmHandler.addVmInitToDB(getVmTemplate());
    }

    protected void addVmInterfaces(Map<Guid, Guid> srcDeviceIdToTargetDeviceIdMapping) {
        List<VmNic> interfaces = getVmNicDao().getAllForVm(getParameters().getMasterVm().getId());
        for (VmNic iface : interfaces) {
            VmNic iDynamic = new VmNic();
            iDynamic.setId(Guid.newGuid());
            iDynamic.setVmTemplateId(getVmTemplateId());
            iDynamic.setName(iface.getName());
            iDynamic.setVnicProfileId(iface.getVnicProfileId());
            iDynamic.setSpeed(VmInterfaceType.forValue(iface.getType()).getSpeed());
            iDynamic.setType(iface.getType());
            iDynamic.setLinked(iface.isLinked());
            getVmNicDao().save(iDynamic);
            srcDeviceIdToTargetDeviceIdMapping.put(iface.getId(), iDynamic.getId());
        }
    }

    protected void addVmTemplateImages(Map<Guid, Guid> srcDeviceIdToTargetDeviceIdMapping) {
        for (DiskImage diskImage : mImages) {
            // The return value of this action is the 'copyImage' task GUID:
            VdcReturnValueBase retValue = executeChildCommand(diskImage.getImageId());

            if (!retValue.getSucceeded()) {
                throw new VdcBLLException(retValue.getFault().getError(), retValue.getFault().getMessage());
            }

            getReturnValue().getVdsmTaskIdList().addAll(retValue.getInternalVdsmTaskIdList());
            DiskImage newImage = (DiskImage) retValue.getActionReturnValue();
            srcDeviceIdToTargetDeviceIdMapping.put(diskImage.getId(), newImage.getId());
        }
    }


    private Guid getVmIdFromImageParameters(){
        return ((CreateImageTemplateParameters)getParameters().getImagesParameters().get(0)).getVmId();
    }

    @Override
    protected void endSuccessfully() {
        setVmTemplateId(getParameters().getVmTemplateId());
        setVmId(getVmIdFromImageParameters());
        isVmInDb = getVm() != null;

        getVmStaticDAO().incrementDbGeneration(getVmTemplateId());
        for (VdcActionParametersBase p : getParameters().getImagesParameters()) {
            Backend.getInstance().endAction(VdcActionType.CreateImageTemplate, p);
        }
        if (reloadVmTemplateFromDB() != null) {
            endDefaultOperations();
        }
        checkTrustedService();
        setSucceeded(true);
    }

    private void checkTrustedService() {
        AuditLogableBase logable = new AuditLogableBase();
        logable.addCustomValue("VmName", getVmName());
        logable.addCustomValue("VmTemplateName", getVmTemplateName());
        if (getVm().isTrustedService() && !getVmTemplate().isTrustedService()) {
            AuditLogDirector.log(logable, AuditLogType.USER_ADD_VM_TEMPLATE_FROM_TRUSTED_TO_UNTRUSTED);
        }
        else if (!getVm().isTrustedService() && getVmTemplate().isTrustedService()) {
            AuditLogDirector.log(logable, AuditLogType.USER_ADD_VM_TEMPLATE_FROM_UNTRUSTED_TO_TRUSTED);
        }
    }

    private void endSuccessfullySynchronous() {
        if (reloadVmTemplateFromDB() != null) {
            endDefaultOperations();
        }
        setSucceeded(true);
    }

    private void endDefaultOperations() {
        endUnlockOps();

        // in case of new version of a template, update vms marked to use latest
        if (isTemplateVersion()) {
            String jobId = SchedulerUtilQuartzImpl.getInstance().scheduleAOneTimeJob(this, "onTimerHandleVdsRecovering", new Class[0],
                    new Object[0], 0, TimeUnit.SECONDS);
            updateVmsJobIdMap.put(getParameters().getBaseTemplateId(), jobId);
        }
    }

    @OnTimerMethodAnnotation("onTimerHandleVdsRecovering")
    public void onTimerHandleVdsRecovering() {
        for (Guid vmId : getVmDAO().getVmIdsForVersionUpdate(getParameters().getBaseTemplateId())) {
            // if the job was removed, stop executing, we probably have new version creation going on
            if (!updateVmsJobIdMap.containsKey(getParameters().getBaseTemplateId())) {
                break;
            }
            UpdateVmVersionParameters params = new UpdateVmVersionParameters(vmId);
            params.setSessionId(getParameters().getSessionId());
            // execute in new transaction, as failure here should not fail template creation
            params.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
            getBackend().runInternalAction(VdcActionType.UpdateVmVersion, params);
        }
        updateVmsJobIdMap.remove(getParameters().getBaseTemplateId());
    }

    private void endUnlockOps() {
        if (isVmInDb) {
            VmHandler.unLockVm(getVm());
        }
        VmTemplateHandler.unlockVmTemplate(getVmTemplateId());
    }

    private VmTemplate reloadVmTemplateFromDB() {
        // set it to null to reload the template from the db
        setVmTemplate(null);
        return getVmTemplate();
    }

    @Override
    protected void endWithFailure() {
        // We evaluate 'VmTemplate' so it won't be null in the last 'if'
        // statement.
        // (a template without images doesn't exist in the 'vm_template_view').
        setVmTemplateId(getParameters().getVmTemplateId());
        setVmId(getVmIdFromImageParameters());

        for (VdcActionParametersBase p : getParameters().getImagesParameters()) {
            p.setTaskGroupSuccess(false);
            Backend.getInstance().endAction(VdcActionType.CreateImageTemplate, p);
        }

        // if template exist in db remove it
        if (getVmTemplate() != null) {
            DbFacade.getInstance().getVmTemplateDao().remove(getVmTemplateId());
            removeNetwork();
        }

        if (!getVmId().equals(Guid.Empty) && getVm() != null) {
            VmHandler.unLockVm(getVm());
        }

        setSucceeded(true);
    }

    /**
     * in case of non-existing cluster the backend query will return a null
     */
    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        if (permissionCheckSubject == null) {
            permissionCheckSubject = new ArrayList<PermissionSubject>();
            Guid storagePoolId = getVdsGroup() == null ? null : getVdsGroup().getStoragePoolId();
            permissionCheckSubject.add(new PermissionSubject(storagePoolId,
                    VdcObjectType.StoragePool,
                    getActionType().getActionGroup()));

            // host-specific parameters can be changed by administration role only
            if (getParameters().getMasterVm().getDedicatedVmForVds() != null ||
                    !StringUtils.isEmpty(getParameters().getMasterVm().getCpuPinning())) {
                permissionCheckSubject.add(
                        new PermissionSubject(storagePoolId,
                                VdcObjectType.StoragePool,
                                ActionGroup.EDIT_ADMIN_TEMPLATE_PROPERTIES));
            }
        }

        return permissionCheckSubject;
    }

    private void addPermission() {
        UniquePermissionsSet permissionsToAdd = new UniquePermissionsSet();

        addPermissionForTemplate(permissionsToAdd, getCurrentUser().getId(), PredefinedRoles.TEMPLATE_OWNER);
        // if the template is for public use, set EVERYONE as a TEMPLATE_USER.
        if (getParameters().isPublicUse()) {
            addPermissionForTemplate(permissionsToAdd, MultiLevelAdministrationHandler.EVERYONE_OBJECT_ID, PredefinedRoles.TEMPLATE_USER);
        } else {
            addPermissionForTemplate(permissionsToAdd, getCurrentUser().getId(), PredefinedRoles.TEMPLATE_USER);
        }

        copyVmPermissions(permissionsToAdd);

        if (!permissionsToAdd.isEmpty()) {
            List<Permissions> permissionsList = permissionsToAdd.asPermissionList();
            MultiLevelAdministrationHandler.addPermission(permissionsList.toArray(new Permissions[permissionsList.size()]));
        }
    }

    private void copyVmPermissions(UniquePermissionsSet permissionsToAdd) {
        if (!isVmInDb || !getParameters().isCopyVmPermissions()) {
            return;
        }

        PermissionDAO dao = getDbFacade().getPermissionDao();

        List<Permissions> vmPermissions = dao.getAllForEntity(getVmId(), getCurrentUser().getId(), false);

        for (Permissions vmPermission : vmPermissions) {
            permissionsToAdd.addPermission(vmPermission.getad_element_id(), vmPermission.getrole_id(),
                    getParameters().getVmTemplateId(), VdcObjectType.VmTemplate);
        }

    }

    private void addPermissionForTemplate(UniquePermissionsSet permissionsToAdd, Guid userId, PredefinedRoles role) {
        permissionsToAdd.addPermission(userId, role.getId(), getParameters().getVmTemplateId(), VdcObjectType.VmTemplate);
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_TEMPLATE);
    }

    private Guid getQuotaIdForDisk(DiskImage diskImage) {
        // If the DiskInfoDestinationMap is available and contains information about the disk
        if (getParameters().getDiskInfoDestinationMap() != null
                && getParameters().getDiskInfoDestinationMap().get(diskImage.getId()) != null) {
            return  getParameters().getDiskInfoDestinationMap().get(diskImage.getId()).getQuotaId();
        }
        return diskImage.getQuotaId();
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<QuotaConsumptionParameter>();

        for (DiskImage disk : getVm().getDiskList()) {
            list.add(new QuotaStorageConsumptionParameter(
                    getQuotaIdForDisk(disk),
                    null,
                    QuotaStorageConsumptionParameter.QuotaAction.CONSUME,
                    disk.getStorageIds().get(0),
                    (double)disk.getSizeInGigabytes()));
        }
        return list;
    }

    private Guid getQuotaId() {
        return getParameters().getMasterVm().getQuotaId();
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaVdsConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<QuotaConsumptionParameter>();
        list.add(new QuotaSanityParameter(getQuotaId(), null));
        return list;
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        if (isTemplateVersion()) {
            return Collections.singletonMap(getParameters().getBaseTemplateId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.TEMPLATE, VdcBllMessages.ACTION_TYPE_FAILED_OBJECT_LOCKED));
        }
        return super.getSharedLocks();
    }
}
