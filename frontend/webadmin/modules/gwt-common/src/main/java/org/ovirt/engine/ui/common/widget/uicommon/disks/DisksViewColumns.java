package org.ovirt.engine.ui.common.widget.uicommon.disks;

import java.util.ArrayList;
import java.util.Date;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.LunDisk;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.widget.table.column.CheckboxColumn;
import org.ovirt.engine.ui.common.widget.table.column.DiskContainersColumn;
import org.ovirt.engine.ui.common.widget.table.column.DiskSizeColumn;
import org.ovirt.engine.ui.common.widget.table.column.DiskStatusColumn;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.FullDateTimeColumn;
import org.ovirt.engine.ui.common.widget.table.column.ImageResourceColumn;
import org.ovirt.engine.ui.common.widget.table.column.StorageDomainsColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.i18n.client.DateTimeFormat;

public class DisksViewColumns {
    private static final CommonApplicationResources resources = GWT.create(CommonApplicationResources.class);
    private static final CommonApplicationConstants constants = GWT.create(CommonApplicationConstants.class);
    private static final CommonApplicationMessages messages = GWT.create(CommonApplicationMessages.class);

    public static final TextColumnWithTooltip<Disk> aliasColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getDiskAlias();
        }
    };

    public static final TextColumnWithTooltip<Disk> idColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getId().toString();
        }
    };

    public static final TextColumnWithTooltip<Disk> qoutaColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {

            String value = "";
            if (object.getDiskStorageType() == DiskStorageType.IMAGE) {
                DiskImage diskImage = (DiskImage) object;
                ArrayList<String> quotaNamesArr = diskImage.getQuotaNames();
                if (quotaNamesArr != null) {
                    value = StringHelper.join(", ", quotaNamesArr.toArray());//$NON-NLS-1$
                }
            }
            return value;
        }
    };

    public static final ImageResourceColumn<Disk> bootableDiskColumn = new ImageResourceColumn<Disk>() {
        @Override
        public ImageResource getValue(Disk object) {
            setTitle(object.isBoot() ? getDefaultTitle() : null);
            return object.isBoot() ? getDefaultImage() : null;
        }

        @Override
        public String getDefaultTitle() {
            return constants.bootableDisk();
        }

        @Override
        public ImageResource getDefaultImage() {
            return resources.bootableDiskIcon();
        }
    };

    public static final ImageResourceColumn<Disk> shareableDiskColumn = new ImageResourceColumn<Disk>() {
        @Override
        public ImageResource getValue(Disk object) {
            setTitle(object.isShareable() ? getDefaultTitle() : null);
            return object.isShareable() ? getDefaultImage() : null;
        }

        @Override
        public String getDefaultTitle() {
            return constants.shareable();
        }

        @Override
        public ImageResource getDefaultImage() {
            return resources.shareableDiskIcon();
        }
    };

    public static final ImageResourceColumn<Disk> readOnlyDiskColumn = new ImageResourceColumn<Disk>() {
        @Override
        public ImageResource getValue(Disk object) {
            setTitle(object.getReadOnly() ? getDefaultTitle() : null);
            return object.getReadOnly() ? getDefaultImage() : null;
        }

        @Override
        public String getDefaultTitle() {
            return constants.readOnly();
        }

        @Override
        public ImageResource getDefaultImage() {
            return resources.readOnlyDiskIcon();
        }
    };

    public static final ImageResourceColumn<Disk> lunDiskColumn = new ImageResourceColumn<Disk>() {
        @Override
        public ImageResource getValue(Disk object) {
            setTitle(object.getDiskStorageType() == DiskStorageType.LUN ? getDefaultTitle() : null);
            return object.getDiskStorageType() == DiskStorageType.LUN ?
                    resources.externalDiskIcon() : null;
        }

        @Override
        public String getDefaultTitle() {
            return constants.lunDisksLabel();
        }

        @Override
        public ImageResource getDefaultImage() {
            return resources.externalDiskIcon();
        }
    };

    public static final ImageResourceColumn<Disk> diskContainersIconColumn = new ImageResourceColumn<Disk>() {
        @Override
        public ImageResource getValue(Disk object) {
            setEnumTitle(object.getVmEntityType());
            if (object.getVmEntityType() == null) {
                return null;
            }
            return object.getVmEntityType().isVmType() ? resources.vmsImage() :
                    object.getVmEntityType().isTemplateType() ? resources.templatesImage() : null;
        }
    };

    public static final DiskStatusColumn diskStatusColumn = new DiskStatusColumn();

    public static final DiskContainersColumn diskContainersColumn = new DiskContainersColumn();

    public static final TextColumnWithTooltip<Disk> diskAlignmentColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            if (object.getLastAlignmentScan() != null) {
                String lastScanDate = DateTimeFormat
                        .getFormat("yyyy-MM-dd, HH:mm").format(object.getLastAlignmentScan()); //$NON-NLS-1$
                setTitle(messages.lastDiskAlignment(lastScanDate));
            } else {
                setTitle(null);
            }
            return object.getAlignment().toString();
        }
    };

    public static final StorageDomainsColumn storageDomainsColumn = new StorageDomainsColumn();

    public static final TextColumnWithTooltip<Disk> storageTypeColumn = new EnumColumn<Disk, StorageType>() {
        @Override
        protected StorageType getRawValue(Disk object) {
            if (object.getDiskStorageType() != DiskStorageType.IMAGE) {
                return null;
            }
            DiskImage disk = (DiskImage) object;

            return disk.getStorageTypes().isEmpty() ? null : disk.getStorageTypes().get(0);
        }
    };

    public static final DiskSizeColumn<Disk> sizeColumn = new DiskSizeColumn<Disk>() {
        @Override
        protected Long getRawValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.IMAGE ?
                    ((DiskImage) object).getSize() :
                    (long) (((LunDisk) object).getLun().getDeviceSize() * Math.pow(1024, 3));
        }
    };

    public static final DiskSizeColumn<Disk> actualSizeColumn = new DiskSizeColumn<Disk>(SizeConverter.SizeUnit.GB) {
        @Override
        protected Long getRawValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.IMAGE ?
                    Math.round(((DiskImage) object).getActualDiskWithSnapshotsSize())
                    : (long) (((LunDisk) object).getLun().getDeviceSize());
        }
    };

    public static final TextColumnWithTooltip<Disk> allocationColumn = new EnumColumn<Disk, VolumeType>() {
        @Override
        protected VolumeType getRawValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.IMAGE ?
                    ((DiskImage) object).getVolumeType() : null;
        }
    };

    public static final TextColumnWithTooltip<Disk> interfaceColumn = new EnumColumn<Disk, DiskInterface>() {
        @Override
        protected DiskInterface getRawValue(Disk object) {
            return object.getDiskInterface();
        }
    };

    public static final TextColumnWithTooltip<Disk> dateCreatedColumn = new FullDateTimeColumn<Disk>() {
        @Override
        protected Date getRawValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.IMAGE ?
                    ((DiskImage) object).getCreationDate() : null;
        }
    };

    public static final TextColumnWithTooltip<Disk> statusColumn = new EnumColumn<Disk, ImageStatus>() {
        @Override
        protected ImageStatus getRawValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.IMAGE ?
                    ((DiskImage) object).getImageStatus() : null;
        }
    };

    public static final TextColumnWithTooltip<Disk> descriptionColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getDiskDescription();
        }
    };

    public static final TextColumnWithTooltip<Disk> lunIdColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.LUN ?
                    ((LunDisk) object).getLun().getLUN_id() : null;
        }
    };

    public static final TextColumnWithTooltip<Disk> lunVendorIdColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.LUN ?
                    ((LunDisk) object).getLun().getVendorId() : null;
        }
    };

    public static final TextColumnWithTooltip<Disk> lunProductIdColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.LUN ?
                    ((LunDisk) object).getLun().getProductId() : null;
        }
    };

    public static final TextColumnWithTooltip<Disk> lunSerialColumn = new TextColumnWithTooltip<Disk>() {
        @Override
        public String getValue(Disk object) {
            return object.getDiskStorageType() == DiskStorageType.LUN ?
                    ((LunDisk) object).getLun().getSerial() : null;
        }
    };

    public static final CheckboxColumn<EntityModel> readOnlyCheckboxColumn = new CheckboxColumn<EntityModel>(
        new FieldUpdater<EntityModel, Boolean>() {
            @Override
            public void update(int idx, EntityModel object, Boolean value) {
                DiskModel diskModel = (DiskModel) object.getEntity();
                diskModel.getDisk().setReadOnly(value);
            }
        }) {
            @Override
            protected boolean canEdit(EntityModel object) {
                    return true;
                }

            @Override
            public Boolean getValue(EntityModel object) {
                DiskModel diskModel = (DiskModel) object.getEntity();
                return diskModel.getDisk().getReadOnly();
            }
    };
}
