package com.pikacho.script.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikacho.script.common.Constant;
import com.pikacho.script.entity.ScriptNode;
import com.pikacho.script.entity.TracerItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: CryRobot
 * @Date: 11/2/2019 7:57 PM
 * @Description:
 */

@Component
@Slf4j
public class PythonEngineService {
    /**
     * python函数名正则
     */
    private static Pattern pattern = Pattern.compile("\\s?def\\s+(\\w+)\\s?\\(");
    /**
     * python函数正则
     */
    private static Pattern proxy = Pattern.compile("\\s*def\\s+(\\w+)\\s*\\(\\s*\\w+\\s*\\)\\s*\\{\\s*([\\s\\S]+)}", Pattern.DOTALL);

    /**
     * 临时代理函数，用户统计函数执行时间，结果。执行的函数名等
     * 1.先交栈顶的元素取出，这个是调用当前脚本的节点，是当前节点的父节点
     * 2.将当前脚本节点对象压入栈中
     * 3.执行并记录节点执行的结果和时间
     * 4.弹出当前脚本节点对象
     */
    private static String TEMPLATE_PROXY = "def #funcName#(data) { " +
            " var parent = tracer.peek();" +
            " var span = { 'method': '#funcName#', 'parent': parent, 'name': '#ruleName#', 'script': '#script#'};" +
            " tracer.push(span); " +
            " var proxy = def () {#body#};" +
            " var start = new Date().getTime();" +
            " var result = proxy(); " +
            " var total = new Date().getTime() - start;" +
            " span.result = result;" +
            " span.total = total;" +
            " tracer.pop();" +
            "return result;" +
            "}";


    private ScriptEngineManager sem = new ScriptEngineManager();

    @Autowired
    List<ScriptService> scriptServiceList;


    private ObjectMapper objectMapper;

    @Value("${python.script.path}")
    private String pyPath;

    private String pyScript;

    @PostConstruct
    public void init() {
        log.info("init python script ...");
        pyScript = getPyScript();
        log.info("python script: {}", pyScript);
    }


    public PythonEngineService() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    private ScriptEngine getScriptEngine() {
        return sem.getEngineByName(Constant.PYTHON);
    }


    public Object runScriptEngine(String content, String modelType) {
        Object result = null;
        ScriptEngine engine = getScriptEngine();
        try {
            //获取函数入口节点
            ScriptNode rootNode = initializeEngine(engine, modelType);
            String entryMain = getFunctionNameByScript(rootNode.getScript());
            Invocable invocable = (Invocable) engine;
            //调用脚本方法，返回结果
            result = invocable.invokeFunction(entryMain, content);
            log.info("invokeFunction result : {}", result);
            //获取执行的链路
            TracerScriptService tracer = (TracerScriptService) engine.get(TracerScriptService.TRACER);
            //保存结果
            Map map = objectMapper.convertValue(result, Map.class);

        } catch (Exception e) {
            throw new RuntimeException("runScriptEngine error", e);
        }
        return result;

    }


    private List<TracerItem> processExecResult(Object result, List<Map<String, Object>> all) throws Exception {
        final String tracerJson = objectMapper.writeValueAsString(all);
        List<TracerItem> tracers = objectMapper.readValue(tracerJson, new TypeReference<List<TracerItem>>() {
        });
        return tracers;
    }


    /**
     * @param engine
     * @param modelType
     * @return
     */
    private ScriptNode initializeEngine(ScriptEngine engine, String modelType) {
        ScriptNode scriptNode = new ScriptNode();
        try {
            engine.eval(pyScript);
        } catch (Exception e) {
            throw new RuntimeException("python file is incorrect", e);
        }
        scriptNode.setName("主函数");
        scriptNode.setScript("def printme( str ):\n" +
                "   \"打印传入的字符串到标准显示设备上\"\n" +
                "   print str\n" +
                "   return");
        return scriptNode;

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
            log.info(" script: " + scriptNode.getScript());
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


    private String getPyScript() {
        String content;
        try {
            File file = new File(pyPath);
            content = FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException("cant fund python script file");
        }
        return content;
    }


}
