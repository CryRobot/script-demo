package com.pikacho.script.service;

/**
 * @Author: CryRobot
 * @Date: 11/2/2019 6:57 PM
 * @Description: 脚本服务接口
 */
public interface ScriptService {
    /**
     * 服务名
     * @return
     */
    String getName();

    /**
     * 对象实例
     * @return
     */
    Object getObject();
}
