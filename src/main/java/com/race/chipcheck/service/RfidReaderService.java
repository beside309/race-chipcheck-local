package com.race.chipcheck.service;

import com.race.chipcheck.transport.JSerialCommTransport;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 解析EPC（从JSerialCommTransport的回调中提取）
     * 这里使用简化逻辑，实际解析已在JSerialCommTransport中完成
     */
    private String parseEpc(byte[] message) {
        if (message == null || message.length < 2) {
            return null;
        }

        // 检查是否是RFID协议（以 "RF" 0x52 0x46 开头）
        if (message[0] == 0x52 && message[1] == 0x46) {
            // 查找EPC数据（标记为 01 08）
            for (int i = 0; i < message.length - 10; i++) {
                if (message[i] == 0x01 && message[i + 1] == 0x08) {
                    int epcStart = i + 2;
                    int epcLength = 8;

                    if (epcStart + epcLength <= message.length) {
                        // 提取EPC并转为十六进制字符串
                        StringBuilder epcHex = new StringBuilder();
                        for (int j = 0; j < epcLength; j++) {
                            String hex = Integer.toHexString(message[epcStart + j] & 0xFF);
                            if (hex.length() == 1) {
                                epcHex.append('0');
                            }
                            epcHex.append(hex);
                        }
                        return epcHex.toString().toUpperCase();
                    }
                }
            }

            // 如果没找到标准格式，尝试从固定位置提取
            if (message.length >= 20) {
                int epcStart = 12;
                int epcLength = 8;

                StringBuilder epcHex = new StringBuilder();
                for (int j = 0; j < epcLength; j++) {
                    String hex = Integer.toHexString(message[epcStart + j] & 0xFF);
                    if (hex.length() == 1) {
                        epcHex.append('0');
                    }
                    epcHex.append(hex);
                }
                return epcHex.toString().toUpperCase();
            }
        }

        return null;
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
