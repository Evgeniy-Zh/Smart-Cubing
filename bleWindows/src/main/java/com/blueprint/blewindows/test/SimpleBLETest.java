package com.blueprint.blewindows.test;

import com.blueprint.blewindows.SimpleBLE;
import com.sun.jna.Pointer;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author 肖嘉威
 * @date 2025/6/13 10:23
 */
public class SimpleBLETest {

    public static void main(String[] args) {
        SimpleBLE simpleBLE = new SimpleBLE();
        boolean bluetoothEnabled = simpleBLE.isBluetoothEnabled();
        System.out.println("ble is enable:" + bluetoothEnabled);
        if (!bluetoothEnabled) return;
        Pointer adapterHandle = simpleBLE.getAdapterHandle(0);
        simpleBLE.scanFor(adapterHandle, 3000);
        int scanResultsCount = simpleBLE.getScanResultsCount(adapterHandle);
        for (int i = 0; i < scanResultsCount; i++) {
            Pointer peripheralHandle = simpleBLE.getPeripheralHandle(adapterHandle, i);
            testESP32test(simpleBLE, peripheralHandle);
        }
        System.out.println("scan count:" + scanResultsCount);
    }

    public static SimpleBLE.libsimpleble.NotifyCallback notifyCallback;
    private static final String characteristicReadUuid = "28be4cb6-cd67-11e9-a32f-2a2ae2dbcce4";
    private static final String characteristicWriteUuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dc4179";

    private static void testESP32test(SimpleBLE simpleBLE, Pointer peripheralHandle) {
        String peripheralIdentifier = simpleBLE.getPeripheralIdentifier(peripheralHandle);
        String peripheralAddress = simpleBLE.getPeripheralAddress(peripheralHandle);

        System.out.println("peripheralIdentifier: " + peripheralIdentifier + ",peripheralAddress: " + peripheralAddress);
        if (Objects.equals(peripheralIdentifier, "GANicDDJ")) {
            boolean connectRes = simpleBLE.connectToPeripheral(peripheralHandle);
            System.out.println("connect " + peripheralAddress + " result:" + connectRes);
            if (connectRes) {
//               强引用指向，防止被jvm垃圾回收
                notifyCallback = new SimpleBLE.libsimpleble.NotifyCallback() {
                    @Override
                    public void invoke(SimpleBLE.libsimpleble.uuid_t service, SimpleBLE.libsimpleble.uuid_t characteristic, Pointer data, long length, Pointer userdata) {
                        byte[] byteArray = data.getByteArray(0L, Math.toIntExact(length));
//                                    System.out.println("content: " + data.getString(0));
                        System.out.println("len:" + length + ",data:" + Arrays.toString(byteArray) + ",time:" + LocalDateTime.now());
                    }
                };
                simpleBLE.subscribeToNotifications(peripheralHandle,
                        serviceUuid, characteristicReadUuid,
                        notifyCallback, null);

                simpleBLE.writeCharacteristicCommand(
                        peripheralHandle,
                        "6e400001-b5a3-f393-e0a9-e50e24dc4179",
                        "28be4a4a-cd67-11e9-a32f-2a2ae2dbcce4",
                        new byte[20]
                );
                try {
                    System.out.println("notify ESP32test, " + LocalDateTime.now());
                    Thread.sleep(60 * 1000 * 100);
                    System.out.println("unsubscribe ESP32test, " + LocalDateTime.now());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long start = System.currentTimeMillis();
                simpleBLE.unsubscribeFromCharacteristic(peripheralHandle, serviceUuid, characteristicReadUuid);
                System.out.println("unsubscribe time:" + (System.currentTimeMillis() - start) + "ms");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                start = System.currentTimeMillis();
                boolean b1 = simpleBLE.disconnectPeripheral(peripheralHandle);
                System.out.println("disconnect " + peripheralAddress + " result:" + b1 + ", disconnect time:" + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

}
