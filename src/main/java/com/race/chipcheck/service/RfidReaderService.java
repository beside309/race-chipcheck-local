package com.race.chipcheck.service;

import com.fazecast.jSerialComm.SerialPort;
import com.race.chipcheck.transport.JSerialCommTransport;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RFID读卡服务（封装串口通信）
 */
public class RfidReaderService {
    private static final Logger logger = LoggerFactory.getLogger(RfidReaderService.class);

    private JSerialCommTransport transport;
    private volatile boolean isConnected = false;
    private final List<TagReadListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 标签读取监听器接口
     */
    public interface TagReadListener {
        void onTagRead(String epc);
        void onConnectionChanged(boolean connected);
        void onError(String message);
    }

    /**
     * 添加监听器
     */
    public void addListener(TagReadListener listener) {
        listeners.add(listener);
    }

    /**
     * 获取系统可用的串口列表
     */
    public static List<String> getAvailablePorts() {
        List<String> portList = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            portList.add(port.getSystemPortName());
        }

        logger.info("检测到 {} 个可用串口", portList.size());
        return portList;
    }

    /**
     * 移除监听器
     */
    public void removeListener(TagReadListener listener) {
        listeners.remove(listener);
    }

    /**
     * 连接RFID读卡器
     */
    public boolean connect(String portName, int baudRate) {
        if (isConnected) {
            logger.warn("读卡器已连接，无需重复连接");
            return true;
        }

        transport = new JSerialCommTransport(portName, baudRate);

        // 创建回调对象
        Object appNotify = new Object() {
            public void NotifyRecvTags(byte[] message, int startIndex) {
                // 解析EPC
                String epc = parseEpc(message);
                if (epc != null && !epc.isEmpty()) {
                    // 在JavaFX线程中通知监听器
                    Platform.runLater(() -> {
                        notifyTagRead(epc);
                    });
                }
            }

            public void onReaderDisconnected() {
                isConnected = false;
                Platform.runLater(() -> {
                    notifyConnectionChanged(false);
                });
            }

            public void onError(String errorMessage) {
                Platform.runLater(() -> {
                    notifyError(errorMessage);
                });
            }
        };

        transport.setAppNotify(appNotify);
        boolean success = transport.open();

        if (success) {
            isConnected = true;
            logger.info("读卡器连接成功：{} (波特率: {})", portName, baudRate);
            notifyConnectionChanged(true);
        } else {
            logger.error("读卡器连接失败：{}", portName);
            notifyConnectionChanged(false);
        }

        return success;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (transport != null) {
            transport.close();
            isConnected = false;
            logger.info("读卡器已断开连接");
            notifyConnectionChanged(false);
        }
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected && transport != null && transport.isConnected();
    }

    /**
     * 解析EPC — 委托给 JSerialCommTransport 的统一解析方法，避免重复解析逻辑
     */
    private String parseEpc(byte[] message) {
        return transport != null ? transport.parseEpc(message) : null;
    }

    /**
     * 通知标签读取
     */
    private void notifyTagRead(String epc) {
        for (TagReadListener listener : listeners) {
            try {
                listener.onTagRead(epc);
            } catch (Exception e) {
                logger.error("通知标签读取失败", e);
            }
        }
    }

    /**
     * 通知连接状态变化
     */
    private void notifyConnectionChanged(boolean connected) {
        for (TagReadListener listener : listeners) {
            try {
                listener.onConnectionChanged(connected);
            } catch (Exception e) {
                logger.error("通知连接状态变化失败", e);
            }
        }
    }

    /**
     * 通知错误
     */
    private void notifyError(String message) {
        for (TagReadListener listener : listeners) {
            try {
                listener.onError(message);
            } catch (Exception e) {
                logger.error("通知错误失败", e);
            }
        }
    }
}
