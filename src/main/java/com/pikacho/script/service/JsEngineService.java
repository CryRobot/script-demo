package com.pikacho.script.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikacho.script.common.Constant;
import com.pikacho.script.entity.ScriptNode;
import com.pikacho.script.entity.TracerItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CryRobot
 * @Date: 11/2/2019 6:57 PM
 * @Description:
 */
@Component
public class JsEngineService {

    private Logger LOGGER = LoggerFactory.getLogger(JsEngineService.class);

    private static Pattern pattern = Pattern.compile("\\s?function\\s+(\\w+)\\s?\\(");
    private ScriptEngineManager sem = new ScriptEngineManager();

    private ScriptEngine getScriptEngine() {
        return sem.getEngineByName(Constant.JAVASCRIPT);
    }

    private static Pattern proxy = Pattern.compile("\\s*function\\s+(\\w+)\\s*\\(\\s*\\w+\\s*\\)\\s*\\{\\s*([\\s\\S]+)}", Pattern.DOTALL);
    private static String TEMPLATE_PROXY = "function #funcName#(data) { " +
            " var parent = tracer.peek();" +
            " var span = { 'method': '#funcName#', 'parent': parent, 'name': '#ruleName#', 'script': '#script#'};" +
            " tracer.push(span); " +
            " var proxy = function () {#body#};" +
            " var start = new Date().getTime();" +
            " var result = proxy(); " +
            " var total = new Date().getTime() - start;" +
            " span.result = result;" +
            " span.total = total;" +
            " tracer.pop();" +
            "return result;" +
            "}";


    @Autowired
    List<ScriptService> scriptServiceList;


    private ObjectMapper objectMapper;

    public JsEngineService() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public Object runScriptEngine(String content, String modelType) {
        ScriptEngine engine = getScriptEngine();
        try {
            //获取函数入口节点
            ScriptNode rootNode = initializeEngine(engine, modelType);
            Invocable invocable = (Invocable) engine;
            String entryMain = getFunctionNameByScript(rootNode.getScript());
            //调用脚本方法，返回结果
            Object result = invocable.invokeFunction(entryMain, content);
            LOGGER.info("invokeFunction result : {}", result);
            //获取执行的链路
            TracerScriptService tracer = (TracerScriptService) engine.get(TracerScriptService.TRACER);
            //保存结果
            Map map = objectMapper.convertValue(result, Map.class);
            LOGGER.info("covert engineService propertyModel {}", map);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;

    }


    private List<TracerItem> processExecResult(Object result, List<Map<String, Object>> all) throws Exception {
        final String tracerJson = objectMapper.writeValueAsString(all);
        List<TracerItem> tracers = objectMapper.readValue(tracerJson, new TypeReference<List<TracerItem>>() {
        });
        return tracers;
    }


    /**
     *
     * @param engine
     * @param modelType
     * @return
     */
    private ScriptNode initializeEngine(ScriptEngine engine, String modelType) {
        ScriptNode rootNode = getMainEntryNode(modelType);
        List<ScriptNode> scriptList = null;
        for (ScriptNode scriptNode : scriptList) {
            //处理脚本
            String script = processScript(scriptNode);
            //执行脚本
            try {
                engine.eval(script);
            } catch (Exception e) {
                LOGGER.error("JS引擎出错，出错脚本:[{}]", script);
            }

        }
        initializeService(engine);
        return rootNode;
    }

    /**
     * 对脚本进行处理
     *
     * @param scriptNode
     * @return
     */
    private String processScript(ScriptNode scriptNode) {
        String script = null;
        Matcher m = proxy.matcher(scriptNode.getScript());
        if (m.find()) {
            //函数
            String funName = m.group(1);
            String body = m.group(2);
            String proxyScript = TEMPLATE_PROXY
                    .replaceAll("#funcName#", funName)
                    .replaceAll("#ruleName#", scriptNode.getName())
                    .replaceAll("#body#", body)
                    .replaceAll("#script#", scriptNode.getScript());
            script = proxyScript;
        } else {
            //表达式
            LOGGER.info("old script: " + scriptNode.getScript());
            script = scriptNode.getScript();
        }
        return script;
    }

    /**
     * 获取主函数节点
     *
     * @param modelType
     * @return
     */
    private ScriptNode getMainEntryNode(String modelType) {
        return new ScriptNode();
    }

    /**
     * 注入到ScriptEngine的上下文中，在脚本引擎中可以调用
     *
     * @param engine
     */
    private void initializeService(ScriptEngine engine) {
        for (ScriptService scriptService : scriptServiceList) {
            engine.put(scriptService.getName(), scriptService.getObject());
        }
    }


    /**
     * 根据脚本获取函数名
     *
     * @param funcScript
     * @return
     */
    private String getFunctionNameByScript(String funcScript) {
        Matcher matcher = pattern.matcher(funcScript);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("funcScript not be a standard  function. " + funcScript);
    }

}
