/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.core.query.selector.attribute.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.test.util.SiddhiTestHelper;
import org.wso2.siddhi.core.util.EventPrinter;

import java.util.concurrent.atomic.AtomicInteger;

public class CountAttributeAggregatorTestCase {
    private static final Log log = LogFactory.getLog(CountAttributeAggregatorTestCase.class);
    private AtomicInteger atomicEventCount;

    @Before
    public void init() {
        atomicEventCount = new AtomicInteger(0);
    }

    @Test
    public void CountAggregatorTest() throws InterruptedException {
        log.info("CountAggregator Test #1");

        SiddhiManager siddhiManager = new SiddhiManager();

        String execPlan = "" +
                "@Plan:name('CountAggregatorTests') " +
                "" +
                "define stream cseEventStream (symbol string, price float);" +
                "" +
                "@info(name = 'query1') " +
                "from cseEventStream#window.timeBatch(5 sec) " +
                "select count(price) as count " +
                "group by symbol " +
                "having count > 2 " +
                "insert all events into outputStream;";

        ExecutionPlanRuntime execPlanRunTime = siddhiManager.createExecutionPlanRuntime(execPlan);

        execPlanRunTime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                atomicEventCount.addAndGet(inEvents.length);
                if (atomicEventCount.get() == 1) {
                    junit.framework.Assert.assertEquals(3L, inEvents[0].getData(0));
                }
            }
        });

        execPlanRunTime.start();
        InputHandler inputHandler = execPlanRunTime.getInputHandler("cseEventStream");
        inputHandler.send(new Object[]{"WSO2", 0F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"WSO2", 0F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"APIM", 3F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"WSO2", 3F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"APIM", 3F});
        Thread.sleep(2000);
        inputHandler.send(new Object[]{"APIM", 3F});
        SiddhiTestHelper.waitForEvents(100, 1, atomicEventCount, 2000);
        execPlanRunTime.shutdown();
        junit.framework.Assert.assertEquals(1, atomicEventCount.intValue());
    }

    @Test
    public void CountAggregatorTest2() throws InterruptedException {
        log.info("CountAggregator Test #2 : Setting Reset value to be false");

        SiddhiManager siddhiManager = new SiddhiManager();

        String execPlan = "" +
                "@Plan:name('CountAggregatorTests') " +
                "" +
                "define stream cseEventStream (symbol string, price float);" +
                "" +
                "@info(name = 'query1') " +
                "from cseEventStream#window.timeBatch(5 sec) " +
                "select count(price, false) as count " +
                "group by symbol " +
                "having count > 2 " +
                "insert all events into outputStream;";

        ExecutionPlanRuntime execPlanRunTime = siddhiManager.createExecutionPlanRuntime(execPlan);

        execPlanRunTime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                atomicEventCount.addAndGet(inEvents.length);
                if (atomicEventCount.get() == 1) {
                    junit.framework.Assert.assertEquals(3L, inEvents[0].getData(0));
                }
            }
        });

        execPlanRunTime.start();
        InputHandler inputHandler = execPlanRunTime.getInputHandler("cseEventStream");
        inputHandler.send(new Object[]{"WSO2", 0F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"WSO2", 0F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"APIM", 3F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"WSO2", 3F});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"APIM", 3F});
        Thread.sleep(2000);
        inputHandler.send(new Object[]{"APIM", 3F});
        SiddhiTestHelper.waitForEvents(100, 1, atomicEventCount, 2000);
        execPlanRunTime.shutdown();
        junit.framework.Assert.assertEquals(1, atomicEventCount.intValue());
    }

    @Test
    public void CountAggregatorTest3() throws InterruptedException {
        log.info("CountAggregator Test #3 : Setting Reset value to be true");

        SiddhiManager siddhiManager = new SiddhiManager();

        String execPlan = "" +
                "@Plan:name('CountAggregatorTests') " +
                "" +
                "define stream cseEventStream (symbol string, price float);" +
                "" +
                "@info(name = 'query1') " +
                "from cseEventStream#window.timeBatch(2 sec) " +
                "select symbol, ifThenElse(price == -1F,count(price, true),count(price, false)) as count " +
                "group by symbol " +
                "insert all events into outputStream;";


        ExecutionPlanRuntime execPlanRunTime = siddhiManager.createExecutionPlanRuntime(execPlan);

        execPlanRunTime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                atomicEventCount.set(0);
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                atomicEventCount.addAndGet(inEvents.length);
                if (atomicEventCount.get() == 1) {
                    junit.framework.Assert.assertEquals("APIM", inEvents[0].getData(0));
                    junit.framework.Assert.assertEquals(1L, inEvents[0].getData(1));

                } else if (atomicEventCount.get() == 2) {
                    junit.framework.Assert.assertEquals("WSO2", inEvents[0].getData(0));
                    junit.framework.Assert.assertEquals(3L, inEvents[0].getData(1));
                    junit.framework.Assert.assertEquals("APIM", inEvents[1].getData(0));
                    junit.framework.Assert.assertEquals(0L, inEvents[1].getData(1));

                }
            }
        });

        execPlanRunTime.start();
        InputHandler inputHandler = execPlanRunTime.getInputHandler("cseEventStream");
        inputHandler.send(new Object[]{"WSO2", 0F});
        Thread.sleep(400);
        inputHandler.send(new Object[]{"WSO2", 0F});
        Thread.sleep(400);
        inputHandler.send(new Object[]{"APIM", 2F});
        Thread.sleep(400);
        inputHandler.send(new Object[]{"WSO2", 3F});
        Thread.sleep(400);
        inputHandler.send(new Object[]{"APIM", -1F});
        Thread.sleep(800);
        junit.framework.Assert.assertEquals(2, atomicEventCount.intValue());
        inputHandler.send(new Object[]{"APIM", 3F});
        Thread.sleep(2000);
        execPlanRunTime.shutdown();
        junit.framework.Assert.assertEquals(1, atomicEventCount.intValue());
    }
}