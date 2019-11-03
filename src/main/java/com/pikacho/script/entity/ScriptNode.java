package com.pikacho.script.entity;

import lombok.Data;

/**
 * @Author: CryRobot
 * @Date: 11/2/2019 7:35 PM
 * @Description:
 */
@Data
public class ScriptNode {
    /**
     * 脚本名称
     */
    private String name;

    /**
     * 脚本
     */
    private String script;

}
