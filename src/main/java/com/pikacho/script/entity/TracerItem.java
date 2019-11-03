package com.pikacho.script.entity;

import lombok.Data;

/**
 * @Author: CryRobot
 * @Date: 11/2/2019 7:11 PM
 * @Description:
 */
@Data
public class TracerItem {

    private String method;
    private String parent;
    private String result;
    private Integer total;
    private String name;
    private String script;
}
