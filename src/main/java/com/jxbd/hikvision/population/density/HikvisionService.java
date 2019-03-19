package com.jxbd.hikvision.population.density;
import com.alibaba.fastjson.JSONObject;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zitong Chen
 * @date 2019/1/5
 */
@Slf4j
@Component
public class HikvisionService {
    private static HCNetSDK hCNetSDK;
    {
        if(Platform.isWindows()){
            hCNetSDK = (HCNetSDK) Native.loadLibrary("HCNetSDK",
                    HCNetSDK.class);
        }
        if(Platform.isLinux()){
            hCNetSDK = (HCNetSDK) Native.loadLibrary("hcnetsdk",
                    HCNetSDK.class);
        }
    }
    /**
     * net_dvr 是否初始化
     */
    private static Boolean netDvrInit = false;
    /**
     * 设备注册时的配置信息
     */
    private static HCNetSDK.NET_DVR_DEVICEINFO_V30 strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();

    /**
     * 多个设备的信息
     */
    private static Map<String, NetDvrItem> netDvrItems = new HashMap<>(16);

    /**
     * 报警回调函数实现，多次声明此函数，后面的声明会把前面的声明覆盖，因此在主次摄像头的时候只需要设置一次
     */
    FMSGCallBack_V31 fMSFCallBack_V31;

    public void AlarmDataHandle(NativeLong lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        String sAlarmType = new String();
        String[] newRow = new String[3];
        //报警时间
        Date today = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String[] sIP = new String[2];
        //lCommand是传的报警类型
        switch (lCommand.intValue()) {
            // 智能检测通用报警，区域人数统计报警Json数据
            case HCNetSDK.COMM_VCA_ALARM:
                String str = pAlarmInfo.getString(0);
                String jsonStr = str.split("--MIME_boundary")[1];
                String[] jsonArr = jsonStr.split("\r\n\r\n");
                String message = jsonArr[1];
                JSONObject jsonObject = (JSONObject) JSONObject.parse(message);
                JSONObject data = new JSONObject();
                data.put("ipAddress", jsonObject.getString("ipAddress"));
                data.put("macAddress", jsonObject.getString("macAddress"));
                data.put("dateTime", jsonObject.getString("dateTime"));
                data.put("eventType", jsonObject.getString("eventType"));

                // 获取区域的人数
                String count = jsonObject.getJSONObject("RegionCapture").getJSONObject("humanCounting").getString("count");
                data.put("humanCount", count);

                log.info("区域关注度摄像头信息入库信息：" + data);
                break;
            // 行为分析信息上传 在开发文档中的 NET_DVR_SetupAlarmChan_V50 有详细说明
            case HCNetSDK.COMM_ALARM_RULE:
                HCNetSDK.NET_VCA_RULE_ALARM strVcaAlarm = new HCNetSDK.NET_VCA_RULE_ALARM();
                strVcaAlarm.write();
                Pointer pVcaInfo = strVcaAlarm.getPointer();
                pVcaInfo.write(0, pAlarmInfo.getByteArray(0, strVcaAlarm.size()), 0, strVcaAlarm.size());
                strVcaAlarm.read();

                switch (strVcaAlarm.struRuleInfo.wEventTypeEx)
                {
                    case 1:
                        sAlarmType = sAlarmType + new String("：穿越警戒面") + ", " +
                                "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                        break;
                    case 2:
                        sAlarmType = sAlarmType + new String("：目标进入区域") + ", " +
                                "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                        break;
                    case 3:
                        sAlarmType = sAlarmType + new String("：目标离开区域") + ", " +
                                "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                        break;
                    case 13:
                        sAlarmType = sAlarmType + new String("：物品遗留") + ", " +
                                "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                        break;
                    default:
                        sAlarmType = sAlarmType + new String("：其他行为分析报警，事件类型：")
                                + strVcaAlarm.struRuleInfo.wEventTypeEx +
                                "_wPort:" + strVcaAlarm.struDevInfo.wPort +
                                "_byChannel:" + strVcaAlarm.struDevInfo.byChannel +
                                "_byIvmsChannel:" +  strVcaAlarm.struDevInfo.byIvmsChannel +
                                "_Dev IP：" + new String(strVcaAlarm.struDevInfo.struDevIP.sIpV4);
                        break;
                }
                System.out.println(sAlarmType);
                newRow[0] = dateFormat.format(today);
                //报警类型
                newRow[1] = sAlarmType;
                //报警设备IP地址
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                newRow[2] = sIP[0];


                if(strVcaAlarm.dwPicDataLen>0)
                {
                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String newName = sf.format(new Date());
                    FileOutputStream fout;
                    try {
                        fout = new FileOutputStream("d:\\var\\"+ new String(pAlarmer.sDeviceIP).trim()
                                + "wEventTypeEx[" + strVcaAlarm.struRuleInfo.wEventTypeEx + "]_"+ newName +"_vca.jpg");
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = strVcaAlarm.pImage.getByteBuffer(offset, strVcaAlarm.dwPicDataLen);
                        byte [] bytes = new byte[strVcaAlarm.dwPicDataLen];
                        buffers.rewind();
                        buffers.get(bytes);
                        fout.write(bytes);
                        fout.close();
                    }catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                break;
            default:
                System.out.println(pAlarmInfo.getString(0));
                log.info("海康摄像头未知类型的报警信息：lCommand = " + lCommand.intValue());
                break;
        }
    }

    /**
     * 回调接口的实现类
     */
    private class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
        /**
         * 报警信息回调函数
         *
         * @param lCommand
         * @param pAlarmer
         * @param pAlarmInfo
         * @param dwBufLen
         * @param pUser
         * @return
         */
        @Override
        public boolean invoke(NativeLong lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
            return true;
        }
    }

    /**
     * 单个设备的登陆信息
     */
    @Data
    private class NetDvrItem {
        private String ip;
        private Short port;
        private String userName;
        private String password;
        /**
         * 用户句柄
         */
        private NativeLong deviceId = new NativeLong(-1);
        /**
         * 报警布防句柄
         */
        private NativeLong alarmHandle = new NativeLong(-1);
    }

    /**
     * 布防
     *
     * @param ip
     * @param port
     * @param userName
     * @param password
     * @throws InterruptedException
     */
    public void setupAlarm(String ip, Short port, String userName, String password) {
        // 初始化 SDK
        if (!netDvrInit) {
            netDvrInit = hCNetSDK.NET_DVR_Init();
        }

        // 设置设备信息
        NetDvrItem netDvrItem = netDvrItems.get(ip);
        if (netDvrItem == null) {
            netDvrItem = new NetDvrItem();
            netDvrItem.setIp(ip);
            netDvrItem.setPort(port);
            netDvrItem.setUserName(userName);
            netDvrItem.setPassword(password);
        }
        NativeLong deviceId = netDvrItem.getDeviceId();
        NativeLong alarmHandle = netDvrItem.getAlarmHandle();

        if (deviceId.longValue() < 0) {
            // 用户注册设备
            deviceId = hCNetSDK.NET_DVR_Login_V30(ip, port, userName, password, strDeviceInfo);
            if (deviceId.longValue() != -1) {
                log.info("海康摄像头注册成功，IP: " + ip);
                // 更新设备的设备句柄
                netDvrItem.setDeviceId(deviceId);
            } else {
                log.error("海康摄像头注册失败，IP: " + ip);
            }
        }

        //尚未布防,需要布防
        if (alarmHandle.intValue() < 0) {
            if (fMSFCallBack_V31 == null) {
                fMSFCallBack_V31 = new FMSGCallBack_V31();
                Pointer pUser = null;
                // 设置报警回调函数
                if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                    log.error("海康摄像头设置回调函数【fMSFCallBack_V31】失败");
                }
            }
            // 设置布防的配置信息
            HCNetSDK.NET_DVR_SETUPALARM_PARAM strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            strAlarmInfo.dwSize = strAlarmInfo.size();
            // 布防优先级：0- 一等级（高），1- 二等级（中），2- 三等级（低）
            strAlarmInfo.byLevel = 1;
            strAlarmInfo.byAlarmInfoType = 1;
            // 布防类型：0-客户端布防，1-实时布防
            strAlarmInfo.byDeployType = 1;
            // 报警图片数据类型，按位表示：0 二进制方式 1 URL 方式
            strAlarmInfo.byAlarmTypeURL = 1;
            strAlarmInfo.write();
            // 设置布防；建立报警上传通道，获取报警等信息。
            alarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(deviceId, strAlarmInfo);
            if (alarmHandle.intValue() != -1) {
                log.info("海康摄像头布防成功，IP: " + ip);

            } else {
                log.error("海康摄像头布防失败，IP: " + ip);
            }
            // 更新设备的布防句柄
            netDvrItem.setAlarmHandle(alarmHandle);
            // 更新设备的信息
            netDvrItems.put(ip, netDvrItem);
        }
    }

    /**
     * 设备的报警撤防
     */
    public void closeAlarm(String ip) {
        NetDvrItem netDvrItem = netDvrItems.get(ip);
        if (netDvrItem == null) {
            return;
        }

        NativeLong alarmHandle = netDvrItem.getAlarmHandle();
        NativeLong deviceId = netDvrItem.getAlarmHandle();
        if (alarmHandle.intValue() > -1) {
            if (hCNetSDK.NET_DVR_CloseAlarmChan_V30(alarmHandle)) {
                log.info("海康摄像头撤防成功，IP: " + ip);
            } else {
                log.error("海康摄像头撤防失败，IP: " + ip);
            }
        }

        // 注销该摄像头
        if (deviceId.intValue() > -1) {
            if (hCNetSDK.NET_DVR_Logout(deviceId)) {
                netDvrItems.remove(ip);
                log.info("海康摄像头注销成功，IP: " + ip);
            } else {
                // 注销失败但撤防成功，因此设置 alarmHandle 为 -1
                netDvrItem.setAlarmHandle(new NativeLong(-1));
                netDvrItems.put(ip, netDvrItem);
                log.info("海康摄像头注销失败，IP: " + ip);
            }
        }

        // 若是所以的摄像头注销了，那么释放 SDK 的资源
        if (netDvrItems.size() == 0) {
            fMSFCallBack_V31 = null;
            // 释放 SDK 资源成功，设置 netDvrInit 为 false 即未初始化 SDK 资源
            netDvrInit = hCNetSDK.NET_DVR_Cleanup() ? false : true;
            log.info("释放海康威视 SDK 资源！");
        }
    }
}

