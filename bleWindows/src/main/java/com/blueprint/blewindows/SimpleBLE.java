package com.blueprint.blewindows;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * @author 肖嘉威
 * [simpleble](https://github.com/simpleble/simpleble)库0.7.1版本的jna封装
 */
public class SimpleBLE {
    //TODO: Fix lib's path
    static String libName =
            Path.of(System.getProperty("user.dir") + "\\bleWindows\\src\\main\\resources\\simpleble-c.dll")
                    .toString();

    // 错误码枚举
    public static final int SIMPLEBLE_SUCCESS = 0;
    public static final int SIMPLEBLE_FAILURE = 1;

    // 地址类型枚举
    public static final int SIMPLEBLE_ADDRESS_TYPE_UNSPECIFIED = 0;
    public static final int SIMPLEBLE_ADDRESS_TYPE_PUBLIC = 1;
    public static final int SIMPLEBLE_ADDRESS_TYPE_RANDOM = 2;

    // 常量定义
    public static final int SIMPLEBLE_UUID_STR_LEN = 37;
    public static final int SIMPLEBLE_CHARACTERISTIC_MAX_COUNT = 16;
    public static final int SIMPLEBLE_DESCRIPTOR_MAX_COUNT = 16;


    public interface libsimpleble extends Library {

        libsimpleble INSTANCE = Native.load(libName, libsimpleble.class);

        // Adapter functions
        boolean simpleble_adapter_is_bluetooth_enabled();

        int simpleble_adapter_get_count();

        Pointer simpleble_adapter_get_handle(int index);

        boolean simpleble_adapter_scan_for(Pointer handle, int timeout);

        int simpleble_adapter_scan_get_results_count(Pointer handle);

        Pointer simpleble_adapter_scan_get_results_handle(Pointer handle, int index);

        // Peripheral functions - 根据C头文件修正函数签名
        void simpleble_peripheral_release_handle(Pointer handle);

        Pointer simpleble_peripheral_identifier(Pointer handle);

        Pointer simpleble_peripheral_address(Pointer handle);

        int simpleble_peripheral_address_type(Pointer handle);

        short simpleble_peripheral_rssi(Pointer handle);

        short simpleble_peripheral_tx_power(Pointer handle);

        short simpleble_peripheral_mtu(Pointer handle);

        int simpleble_peripheral_connect(Pointer handle);

        int simpleble_peripheral_disconnect(Pointer handle);

        int simpleble_peripheral_is_connected(Pointer handle, boolean[] connected);

        int simpleble_peripheral_is_connectable(Pointer handle, boolean[] connectable);

        int simpleble_peripheral_is_paired(Pointer handle, boolean[] paired);

        int simpleble_peripheral_unpair(Pointer handle);

        // 服务相关
        size_t simpleble_peripheral_services_count(Pointer handle);

        int simpleble_peripheral_services_get(Pointer handle, size_t index, service_t services);

        // 制造商数据
        size_t simpleble_peripheral_manufacturer_data_count(Pointer handle);

        int simpleble_peripheral_manufacturer_data_get(Pointer handle, size_t index, manufacturer_data_t manufacturer_data);

        // 读写操作
        int simpleble_peripheral_read(Pointer handle, uuid_t service, uuid_t characteristic,
                                      PointerByReference data, size_t.ByReference data_length);

        int simpleble_peripheral_write_request(Pointer handle, uuid_t service, uuid_t characteristic,
                                               byte[] data, size_t data_length);

        int simpleble_peripheral_write_command(Pointer handle, uuid_t service, uuid_t characteristic,
                                               byte[] data, size_t data_length);

        // 通知和指示
        int simpleble_peripheral_notify(Pointer handle, uuid_t service, uuid_t characteristic,
                                        NotifyCallback callback, Pointer userdata);

        int simpleble_peripheral_indicate(Pointer handle, uuid_t service, uuid_t characteristic,
                                          NotifyCallback callback, Pointer userdata);

        int simpleble_peripheral_unsubscribe(Pointer handle, uuid_t service, uuid_t characteristic);

        // 描述符操作
        int simpleble_peripheral_read_descriptor(Pointer handle, uuid_t service, uuid_t characteristic,
                                                 uuid_t descriptor, PointerByReference data, size_t.ByReference data_length);

        int simpleble_peripheral_write_descriptor(Pointer handle, uuid_t service, uuid_t characteristic,
                                                  uuid_t descriptor, byte[] data, size_t data_length);

        // 回调设置
        int simpleble_peripheral_set_callback_on_connected(Pointer handle, ConnectionCallback callback, Pointer userdata);

        int simpleble_peripheral_set_callback_on_disconnected(Pointer handle, ConnectionCallback callback, Pointer userdata);

        // 回调接口定义
        interface NotifyCallback extends Callback {
            void invoke(uuid_t service, uuid_t characteristic, Pointer data, long length, Pointer userdata);
        }

        interface ConnectionCallback extends Callback {
            void invoke(Pointer peripheral_handle, Pointer userdata);
        }

        // 结构体定义
        public static class size_t extends IntegerType {
            public static final size_t ZERO = new size_t();

            public static class ByReference extends size_t implements Structure.ByReference {
            }

            private static final long serialVersionUID = 1L;

            public size_t() {
                this(0);
            }

            public size_t(long value) {
                super(Native.SIZE_T_SIZE, value, true);
            }
        }

        public static class uuid_t extends Structure {
            public static class ByValue extends uuid_t implements Structure.ByValue {
            }

            public static class ByReference extends uuid_t implements Structure.ByReference {
            }

            private static final List<String> FIELDS = Arrays.asList("value");

            public byte[] value = new byte[SIMPLEBLE_UUID_STR_LEN];

            public uuid_t() {
            }

            public uuid_t(String uuidStr) {
                System.arraycopy(uuidStr.getBytes(), 0, value, 0, Math.min(uuidStr.length(), SIMPLEBLE_UUID_STR_LEN - 1));
            }

            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }

            @Override
            public String toString() {
                return new String(value).trim();
            }
        }

        public static class descriptor_t extends Structure {
            public static class ByValue extends descriptor_t implements Structure.ByValue {
            }

            private static final List<String> FIELDS = Arrays.asList("uuid");

            public uuid_t uuid = new uuid_t();

            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }

        public static class characteristic_t extends Structure {
            public static class ByValue extends characteristic_t implements Structure.ByValue {
            }

            private static final List<String> FIELDS = Arrays.asList(
                    "uuid",
                    "can_read",
                    "can_write_request",
                    "can_write_command",
                    "can_notify",
                    "can_indicate",
                    "descriptor_count",
                    "descriptors"
            );

            public uuid_t uuid = new uuid_t();
            public byte can_read;
            public byte can_write_request;
            public byte can_write_command;
            public byte can_notify;
            public byte can_indicate;
            public size_t descriptor_count = new size_t();
            public descriptor_t[] descriptors = new descriptor_t[SIMPLEBLE_DESCRIPTOR_MAX_COUNT];

            public characteristic_t() {
                for (int i = 0; i < descriptors.length; i++) {
                    descriptors[i] = new descriptor_t();
                }
            }

            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }

        public static class service_t extends Structure {
            public static class ByValue extends service_t implements Structure.ByValue {
            }

            private static final List<String> FIELDS = Arrays.asList(
                    "uuid",
                    "data_length",
                    "data",
                    "characteristic_count",
                    "characteristics"
            );

            public uuid_t uuid = new uuid_t();
            public size_t data_length = new size_t();
            public byte[] data = new byte[27];
            public size_t characteristic_count = new size_t();
            public characteristic_t[] characteristics = new characteristic_t[SIMPLEBLE_CHARACTERISTIC_MAX_COUNT];

            public service_t() {
                for (int i = 0; i < characteristics.length; i++) {
                    characteristics[i] = new characteristic_t();
                }
            }

            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }

        public static class manufacturer_data_t extends Structure {
            public static class ByValue extends manufacturer_data_t implements Structure.ByValue {
            }

            private static final List<String> FIELDS = Arrays.asList(
                    "manufacturer_id",
                    "data_length",
                    "data"
            );

            public short manufacturer_id;
            public size_t data_length = new size_t();
            public byte[] data = new byte[252];

            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }
    }

    // 封装方法
    public boolean isBluetoothEnabled() {
        return libsimpleble.INSTANCE.simpleble_adapter_is_bluetooth_enabled();
    }

    public int getAdapterCount() {
        return libsimpleble.INSTANCE.simpleble_adapter_get_count();
    }

    public Pointer getAdapterHandle(int index) {
        return libsimpleble.INSTANCE.simpleble_adapter_get_handle(index);
    }

    public boolean scanFor(Pointer adapterHandle, int timeout) {
        return libsimpleble.INSTANCE.simpleble_adapter_scan_for(adapterHandle, timeout);
    }

    public int getScanResultsCount(Pointer adapterHandle) {
        return libsimpleble.INSTANCE.simpleble_adapter_scan_get_results_count(adapterHandle);
    }

    public Pointer getPeripheralHandle(Pointer adapterHandle, int index) {
        return libsimpleble.INSTANCE.simpleble_adapter_scan_get_results_handle(adapterHandle, index);
    }

    public String getPeripheralIdentifier(Pointer peripheralHandle) {
        Pointer result = libsimpleble.INSTANCE.simpleble_peripheral_identifier(peripheralHandle);
        if (result == null) return null;
        String identifier = result.getString(0);
        Native.free(Pointer.nativeValue(result));
        return identifier;
    }

    public String getPeripheralAddress(Pointer peripheralHandle) {
        Pointer result = libsimpleble.INSTANCE.simpleble_peripheral_address(peripheralHandle);
        if (result == null) return null;
        String address = result.getString(0);
        Native.free(Pointer.nativeValue(result));
        return address;
    }

    public int getPeripheralAddressType(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_address_type(peripheralHandle);
    }

    public short getPeripheralRssi(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_rssi(peripheralHandle);
    }

    public short getPeripheralTxPower(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_tx_power(peripheralHandle);
    }

    public short getPeripheralMtu(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_mtu(peripheralHandle);
    }

    public void releasePeripheral(Pointer peripheralHandle) {
        libsimpleble.INSTANCE.simpleble_peripheral_release_handle(peripheralHandle);
    }

    public boolean isPeripheralConnectable(Pointer peripheralHandle) {
        boolean[] result = new boolean[1];
        int status = libsimpleble.INSTANCE.simpleble_peripheral_is_connectable(peripheralHandle, result);
        return status == SIMPLEBLE_SUCCESS && result[0];
    }

    public boolean isPeripheralConnected(Pointer peripheralHandle) {
        boolean[] result = new boolean[1];
        int status = libsimpleble.INSTANCE.simpleble_peripheral_is_connected(peripheralHandle, result);
        return status == SIMPLEBLE_SUCCESS && result[0];
    }

    public boolean isPeripheralPaired(Pointer peripheralHandle) {
        boolean[] result = new boolean[1];
        int status = libsimpleble.INSTANCE.simpleble_peripheral_is_paired(peripheralHandle, result);
        return status == SIMPLEBLE_SUCCESS && result[0];
    }

    public boolean connectToPeripheral(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_connect(peripheralHandle) == SIMPLEBLE_SUCCESS;
    }

    public boolean disconnectPeripheral(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_disconnect(peripheralHandle) == SIMPLEBLE_SUCCESS;
    }

    public boolean unpairPeripheral(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_unpair(peripheralHandle) == SIMPLEBLE_SUCCESS;
    }

    public long getPeripheralServicesCount(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_services_count(peripheralHandle).longValue();
    }

    public libsimpleble.service_t getPeripheralService(Pointer peripheralHandle, int index) {
        libsimpleble.service_t service = new libsimpleble.service_t();
        int status = libsimpleble.INSTANCE.simpleble_peripheral_services_get(peripheralHandle, new libsimpleble.size_t(index), service);
        return status == SIMPLEBLE_SUCCESS ? service : null;
    }

    public long getManufacturerDataCount(Pointer peripheralHandle) {
        return libsimpleble.INSTANCE.simpleble_peripheral_manufacturer_data_count(peripheralHandle).longValue();
    }

    public libsimpleble.manufacturer_data_t getManufacturerData(Pointer peripheralHandle, int index) {
        libsimpleble.manufacturer_data_t data = new libsimpleble.manufacturer_data_t();
        int status = libsimpleble.INSTANCE.simpleble_peripheral_manufacturer_data_get(peripheralHandle, new libsimpleble.size_t(index), data);
        return status == SIMPLEBLE_SUCCESS ? data : null;
    }

    public byte[] readCharacteristic(Pointer peripheralHandle, String serviceUuid, String characteristicUuid) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        PointerByReference dataRef = new PointerByReference();
        libsimpleble.size_t.ByReference lengthRef = new libsimpleble.size_t.ByReference();

        int status = libsimpleble.INSTANCE.simpleble_peripheral_read(peripheralHandle, service, characteristic, dataRef, lengthRef);
        if (status != SIMPLEBLE_SUCCESS) return null;

        Pointer dataPtr = dataRef.getValue();
        if (dataPtr == null) return null;

        int length = (int) lengthRef.longValue();
        byte[] result = dataPtr.getByteArray(0, length);
        Native.free(Pointer.nativeValue(dataPtr));
        return result;
    }

    public boolean writeCharacteristicRequest(Pointer peripheralHandle, String serviceUuid, String characteristicUuid, byte[] data) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        int status = libsimpleble.INSTANCE.simpleble_peripheral_write_request(peripheralHandle, service, characteristic, data, new libsimpleble.size_t(data.length));
        return status == SIMPLEBLE_SUCCESS;
    }

    public boolean writeCharacteristicCommand(Pointer peripheralHandle, String serviceUuid, String characteristicUuid, byte[] data) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        int status = libsimpleble.INSTANCE.simpleble_peripheral_write_command(peripheralHandle, service, characteristic, data, new libsimpleble.size_t(data.length));
        return status == SIMPLEBLE_SUCCESS;
    }

    public boolean subscribeToNotifications(Pointer peripheralHandle, String serviceUuid, String characteristicUuid, libsimpleble.NotifyCallback callback, Pointer userdata) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        int status = libsimpleble.INSTANCE.simpleble_peripheral_notify(peripheralHandle, service, characteristic, callback, userdata);
        return status == SIMPLEBLE_SUCCESS;
    }

    public boolean subscribeToIndications(Pointer peripheralHandle, String serviceUuid, String characteristicUuid, libsimpleble.NotifyCallback callback, Pointer userdata) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        int status = libsimpleble.INSTANCE.simpleble_peripheral_indicate(peripheralHandle, service, characteristic, callback, userdata);
        return status == SIMPLEBLE_SUCCESS;
    }

    public boolean unsubscribeFromCharacteristic(Pointer peripheralHandle, String serviceUuid, String characteristicUuid) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        int status = libsimpleble.INSTANCE.simpleble_peripheral_unsubscribe(peripheralHandle, service, characteristic);
        return status == SIMPLEBLE_SUCCESS;
    }

    public byte[] readDescriptor(Pointer peripheralHandle, String serviceUuid, String characteristicUuid, String descriptorUuid) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        libsimpleble.uuid_t descriptor = new libsimpleble.uuid_t(descriptorUuid);
        PointerByReference dataRef = new PointerByReference();
        libsimpleble.size_t.ByReference lengthRef = new libsimpleble.size_t.ByReference();

        int status = libsimpleble.INSTANCE.simpleble_peripheral_read_descriptor(peripheralHandle, service, characteristic, descriptor, dataRef, lengthRef);
        if (status != SIMPLEBLE_SUCCESS) return null;

        Pointer dataPtr = dataRef.getValue();
        if (dataPtr == null) return null;

        int length = (int) lengthRef.longValue();
        byte[] result = dataPtr.getByteArray(0, length);
        Native.free(Pointer.nativeValue(dataPtr));
        return result;
    }

    public boolean writeDescriptor(Pointer peripheralHandle, String serviceUuid, String characteristicUuid, String descriptorUuid, byte[] data) {
        libsimpleble.uuid_t service = new libsimpleble.uuid_t(serviceUuid);
        libsimpleble.uuid_t characteristic = new libsimpleble.uuid_t(characteristicUuid);
        libsimpleble.uuid_t descriptor = new libsimpleble.uuid_t(descriptorUuid);
        int status = libsimpleble.INSTANCE.simpleble_peripheral_write_descriptor(peripheralHandle, service, characteristic, descriptor, data, new libsimpleble.size_t(data.length));
        return status == SIMPLEBLE_SUCCESS;
    }

    public boolean setConnectionCallback(Pointer peripheralHandle, libsimpleble.ConnectionCallback callback, Pointer userdata) {
        int status = libsimpleble.INSTANCE.simpleble_peripheral_set_callback_on_connected(peripheralHandle, callback, userdata);
        return status == SIMPLEBLE_SUCCESS;
    }

    public boolean setDisconnectionCallback(Pointer peripheralHandle, libsimpleble.ConnectionCallback callback, Pointer userdata) {
        int status = libsimpleble.INSTANCE.simpleble_peripheral_set_callback_on_disconnected(peripheralHandle, callback, userdata);
        return status == SIMPLEBLE_SUCCESS;
    }

}