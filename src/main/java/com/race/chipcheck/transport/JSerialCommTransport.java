package com.race.chipcheck.transport;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于 jSerialComm 的串口传输实现
 * 替代原来的 RXTX 实现
 */
public class JSerialCommTransport {
    private static final Logger logger = LoggerFactory.getLogger(JSerialCommTransport.class);

    private SerialPort serialPort;
    private String portName;
    private int baudRate;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean isConnected = false;
    private final BlockingQueue<byte[]> receiveQueue = new LinkedBlockingQueue<>();
    private Thread readThread;
    private Object appNotify; // 用于回调通知

    public JSerialCommTransport(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }
    
    /**
     * 设置应用通知回调
     */
    public void setAppNotify(Object appNotify) {
        this.appNotify = appNotify;
    }

    public boolean open() {
        try {
            // 获取串口
            serialPort = SerialPort.getCommPort(portName);
            if (serialPort == null) {
                logger.error("Serial port {} not found", portName);
                return false;
            }

            // 配置串口参数
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING |
                            SerialPort.TIMEOUT_WRITE_BLOCKING,
                    1000, 1000
            );

            // 打开串口
            if (!serialPort.openPort()) {
                logger.error("Failed to open serial port: {}", portName);
                return false;
            }

            // 获取流
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            // 监听“端口拔出”事件，拔掉读卡器后能及时更新连接状态
            serialPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                        logger.warn("检测到串口已拔出: {}", portName);
                        notifyReaderDisconnected("串口已拔出");
                        close();
                    }
                }
            });

            // 启动数据读取线程
            startReadThread();

            isConnected = true;
            logger.info("Serial port {} opened successfully with baud rate {}", portName, baudRate);
            return true;

        } catch (Exception e) {
            logger.error("Error opening serial port: {}", e.getMessage(), e);
            close();
            return false;
        }
    }

    private void startReadThread() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];

            while (isConnected && !Thread.currentThread().isInterrupted()) {
                try {
                    if (serialPort.bytesAvailable() > 0) {
                        int bytesRead = inputStream.read(buffer);
                        if (bytesRead > 0) {
                            byte[] data = new byte[bytesRead];
                            System.arraycopy(buffer, 0, data, 0, bytesRead);
                            receiveQueue.put(data);

                            // 解析并打印 RFID 数据包
                            String hexString = bytesToHex(data, bytesRead);
                            String tagInfo = parseRfidTagData(data, bytesRead);
                            
                            logger.info("Received {} bytes - Hex: {}", bytesRead, hexString);
                            if (!tagInfo.isEmpty()) {
                                logger.info("  Tag Info: {}", tagInfo);
                                
                                // 提取 EPC 并触发回调
                                String epc = extractEpcFromTagInfo(tagInfo);
                                if (epc != null && !epc.isEmpty() && appNotify != null) {
                                    triggerNotifyRecvTags(data, 0, epc);
                                }
                            }
                        }
                    }
                    Thread.sleep(1); // 减少CPU占用
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    if (isConnected) {
                        logger.error("Error reading from serial port: {}", e.getMessage());
                        notifyReaderDisconnected(e.getMessage());
                    }
                    break;
                } catch (Exception e) {
                    logger.error("Unexpected error in read thread: {}", e.getMessage());
                    notifyReaderDisconnected(e.getMessage());
                    break;
                }
            }
        }, "SerialPort-ReadThread-" + portName);

        readThread.setDaemon(true);
        readThread.start();
    }

    /**
     * 当串口读线程发生异常或端口被拔出时，通知上层读卡器“已断开”
     */
    private void notifyReaderDisconnected(String errorMessage) {
        isConnected = false;
        if (appNotify == null) return;
        try {
            // 优先调用 onError(String)
            try {
                java.lang.reflect.Method errorMethod = appNotify.getClass().getMethod("onError", String.class);
                errorMethod.setAccessible(true);
                errorMethod.invoke(appNotify, errorMessage);
            } catch (NoSuchMethodException ignore) {
                // 忽略，兼容没有实现 onError 的情况
            }
            // 再调用 onReaderDisconnected()
            try {
                java.lang.reflect.Method disconnectedMethod = appNotify.getClass().getMethod("onReaderDisconnected");
                disconnectedMethod.setAccessible(true);
                disconnectedMethod.invoke(appNotify);
            } catch (NoSuchMethodException ignore) {
                // 忽略，没有该方法就不调用
            }
        } catch (Exception e) {
            logger.error("通知读卡器断开连接失败: {}", e.getMessage());
        }
    }

    /**
     * 将字节数组转换为可读字符串（显示ASCII字符，不可打印字符显示为点）
     */
    private String bytesToReadableString(byte[] data, int length) {
        if (data == null || length <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            byte b = data[i];
            // ASCII 可打印字符范围：32-126
            if (b >= 32 && b <= 126) {
                sb.append((char) b);
            } else {
                // 不可打印字符显示为点
                sb.append('.');
            }
        }
        return sb.toString();
    }

    /**
     * 解析 RFID 标签数据包，提取 EPC 等信息
     * 数据包格式：RF [协议头] ... [EPC数据] ...
     * 根据你的数据：52 46 02 00 00 80 00 0f 50 0d 01 08 c2 4b b8 05 aa 00 39 89 ...
     * EPC 数据在偏移 12 位置开始：c2 4b b8 05 aa 00 39 89 = C24BB805AA003989
     * 
     * 数据包可能包含多个标签，每个标签格式：01 08 [8字节EPC] [其他数据]
     * 注意：会对重复的 EPC 进行去重，只显示唯一的标签
     */
    private String parseRfidTagData(byte[] data, int length) {
        if (data == null || length < 2) {
            return "";
        }

        Set<String> uniqueEpcs = new HashSet<>();
        
        // 检查是否是 RFID 协议（以 "RF" 0x52 0x46 开头）
        if (data[0] == 0x52 && data[1] == 0x46) {
            // 查找所有 EPC 数据（可能包含多个标签）
            // EPC 标记：01 08 表示 EPC 长度为 8 字节
            for (int i = 0; i < length - 10; i++) {
                // 查找 EPC 起始标记 01 08
                if (data[i] == 0x01 && data[i + 1] == 0x08) {
                    int epcStart = i + 2;
                    int epcLength = 8; // EPC 长度固定为 8 字节
                    
                    if (epcStart + epcLength <= length) {
                        // 提取 EPC 数据
                        byte[] epcData = new byte[epcLength];
                        System.arraycopy(data, epcStart, epcData, 0, epcLength);
                        String epcHex = bytesToHex(epcData, epcLength).replace(" ", "").toUpperCase();
                        
                        // 使用 Set 自动去重
                        uniqueEpcs.add(epcHex);
                    }
                }
            }
            
            // 如果没找到标准格式，尝试从固定位置提取（兼容其他格式）
            if (uniqueEpcs.isEmpty() && length >= 20) {
                // 尝试从偏移 12 开始提取 8 字节作为 EPC（第一个标签）
                int epcStart = 12;
                int epcLength = 8;
                
                if (epcStart + epcLength <= length) {
                    byte[] epcData = new byte[epcLength];
                    System.arraycopy(data, epcStart, epcData, 0, epcLength);
                    String epcHex = bytesToHex(epcData, epcLength).replace(" ", "").toUpperCase();
                    uniqueEpcs.add(epcHex);
                }
            }
        }
        
        // 构建返回信息，只显示唯一的 EPC
        if (uniqueEpcs.isEmpty()) {
            return "";
        }
        
        StringBuilder info = new StringBuilder();
        int index = 1;
        for (String epc : uniqueEpcs) {
            if (index > 1) {
                info.append(" | ");
            }
            if (uniqueEpcs.size() == 1) {
                // 只有一个标签时，直接显示 EPC
                info.append(String.format("EPC: %s", epc));
            } else {
                // 多个标签时，显示编号
                info.append(String.format("Tag%d EPC: %s", index, epc));
            }
            index++;
        }
        
        if (uniqueEpcs.size() > 1) {
            info.insert(0, String.format("Found %d unique tags: ", uniqueEpcs.size()));
        }
        
        return info.toString();
    }
    
    /**
     * 从标签信息中提取 EPC（格式：EPC: C24BB805AA003989）
     */
    private String extractEpcFromTagInfo(String tagInfo) {
        if (tagInfo == null || tagInfo.isEmpty()) {
            return null;
        }
        
        // 查找 "EPC: " 后面的内容
        int epcIndex = tagInfo.indexOf("EPC: ");
        if (epcIndex >= 0) {
            String epc = tagInfo.substring(epcIndex + 5).trim();
            // 如果包含多个标签，只取第一个
            int pipeIndex = epc.indexOf(" | ");
            if (pipeIndex > 0) {
                epc = epc.substring(0, pipeIndex);
            }
            return epc;
        }
        return null;
    }
    
    /**
     * 触发 NotifyRecvTags 回调
     */
    private void triggerNotifyRecvTags(byte[] message, int startIndex, String epc) {
        try {
            if (appNotify != null) {
                // 使用反射调用 NotifyRecvTags 方法（appNotify 可能是匿名内部类，需 setAccessible 避免 IllegalAccessException）
                java.lang.reflect.Method method = appNotify.getClass().getMethod("NotifyRecvTags", byte[].class, int.class);
                method.setAccessible(true);
                method.invoke(appNotify, message, startIndex);
                logger.debug("触发 NotifyRecvTags 回调: EPC={}", epc);
            }
        } catch (Exception e) {
            logger.error("触发 NotifyRecvTags 回调失败", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] data, int length) {
        if (data == null || length <= 0) {
            return "";
        }

        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(data[i] & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
            if (i < length - 1) {
                hexString.append(" ");
            }
        }
        return hexString.toString();
    }

    public boolean sendData(byte[] data) {
        if (!isConnected || outputStream == null) {
            logger.error("Serial port not connected");
            return false;
        }

        try {
            outputStream.write(data);
            outputStream.flush();
            logger.debug("Sent {} bytes to serial port", data.length);
            return true;
        } catch (IOException e) {
            logger.error("Error sending data to serial port: {}", e.getMessage());
            return false;
        }
    }

    public byte[] receiveData() {
        try {
            return receiveQueue.poll();
        } catch (Exception e) {
            logger.error("Error receiving data: {}", e.getMessage());
            return null;
        }
    }

    public byte[] receiveData(int timeoutMs) {
        try {
            return receiveQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            logger.error("Error receiving data with timeout: {}", e.getMessage());
            return null;
        }
    }

    public void close() {
        isConnected = false;

        if (readThread != null) {
            readThread.interrupt();
            try {
                readThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (serialPort != null) {
            try {
                serialPort.removeDataListener();
            } catch (Exception ignored) {
                // 未添加过监听器或已移除时忽略
            }
            if (serialPort.isOpen()) {
                serialPort.closePort();
                logger.info("Serial port {} closed", portName);
            }
        }

        receiveQueue.clear();
    }

    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }

    public String getPortName() {
        return portName;
    }

    public int getBaudRate() {
        return baudRate;
    }
}