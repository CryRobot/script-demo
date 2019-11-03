package com.pikacho.script.service;

import com.pikacho.script.common.Constant;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @Author: CryRobot
 * @Date: 11/2/2019 7:08 PM
 * @Description:
 */
@Component
public class TracerScriptService implements ScriptService {

    public static final String TRACER = "tracer";
    private Stack<Map<String, Object>> stack = new Stack<>();
    private List<Map<String, Object>> tracers = new LinkedList<>();

    /**
     * 取栈顶的元素,获取栈顶的方法名
     *
     * @return
     */
    public String peek() {
        if (stack.isEmpty()) {
            return Constant.DEFAULT_VALUE;
        }
        Map<String, Object> stackTopElement = stack.peek();
        return String.valueOf(stackTopElement.getOrDefault(Constant.METHOD, Constant.DEFAULT_VALUE));
    }

    /**
     * 压入栈中
     *
     * @param content
     */
    public void push(Map<String, Object> content) {
        stack.push(content);
    }

    /**
     * 弹出栈顶的元素，并且加入的链路中。脚本执行完执行
     */
    public void pop() {
        Map<String, Object> content = stack.pop();
        tracers.add(content);
    }

    /**
     * 获取脚本的所有执行轨迹
     * @return
     */
    public List<Map<String, Object>> all() {
        return tracers;
    }


    @Override
    public String getName() {
        return TRACER;
    }

    @Override
    public Object getObject() {
        return new TracerScriptService();
    }
}
