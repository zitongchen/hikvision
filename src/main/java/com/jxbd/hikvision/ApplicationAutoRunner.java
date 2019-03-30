package com.jxbd.hikvision;

import com.jxbd.hikvision.population.density.HikvisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 程序启动后初始化海康摄像头的布防工作
 * @author Zitong Chen
 * @date 2019/3/16
 */
// 暂时取消自动初始化
//@Component
public class ApplicationAutoRunner implements ApplicationRunner {

    /**
     * 项目启动的时候进行布防的海康威视摄像头 ip
     */
    @Value("${hikvision.arming.ips}")
    private String[] ips ;
    @Value("${hikvision.arming.ipPort}")
    private short ipPort;
    @Value("${hikvision.arming.ipUserName}")
    private String ipUserName;
    @Value("${hikvision.arming.ipPassword}")
    private String ipPassword;

    @Autowired
    private HikvisionService hikvisionService;



    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        arming();
    }

    /**
     * 海康威视摄像头的布防
     */
    private void arming(){
        for(String ip : ips){
            hikvisionService.setupAlarm(ip, ipPort, ipUserName, ipPassword);
        }
    }
}
