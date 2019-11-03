package com.pikacho.script;

import com.pikacho.script.service.PythonEngineService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={ScriptDemoApplication.class})
public class ScriptDemoApplicationTests {

    @Autowired
    private PythonEngineService pythonEngineService;


    @Test
    public void test() throws InterruptedException {

        for (Integer i = 0; i <1000 ; i++) {

            Integer finalI = i;
            new Thread(()->{
                Object o = pythonEngineService.runScriptEngine("sss"+ finalI, finalI +"");
            }).start();

        }


        Thread.sleep(20000);

    }

}
