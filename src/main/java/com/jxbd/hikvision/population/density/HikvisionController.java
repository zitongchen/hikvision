package com.jxbd.hikvision.population.density;//package com.jxbd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Zitong Chen
 * @date 2019/1/7
 */
@Controller
@RequestMapping(value = "/hikvision")
public class HikvisionController {

    @Autowired
    private HikvisionService hikvisionService;


    @GetMapping()
    public String index(){
        return "hikvision/index";
    }

    @PostMapping("/arming")
    @ResponseBody
    public Map<String, String> Arming(String ip, Short port, String account, String password) {

        hikvisionService.setupAlarm(ip, port, account, password);
        Map<String, String> map = new HashMap<>(1);
        map.put("test", "布防");
        return map;
    }

    @GetMapping("/closeAlarm")
    @ResponseBody
    public Map<String, String> closeAlarm(String ip) {
        hikvisionService.closeAlarm(ip);
        Map<String, String> map = new HashMap<>(1);
        map.put("test", "撤防");
        return map;
    }

}
