/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.siddhi.core.query.eventwindow;

import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.test.util.SiddhiTestHelper;
import org.wso2.siddhi.core.util.EventPrinter;

import java.util.concurrent.atomic.AtomicInteger;

public class CronEventWindowTestCase {
    private static final Log log = LogFactory.getLog(CronEventWindowTestCase.class);
    private AtomicInteger inEventCount;
    private AtomicInteger removeEventCount;
    private boolean eventArrived;

    @Before
    public void init() {
        inEventCount = new AtomicInteger(0);
        removeEventCount = new AtomicInteger(0);
        eventArrived = false;
    }

    @Test
    public void testCronWindow1() throws InterruptedException {
        log.info("Testing cron window for current events");

        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "define stream cseEventStream (symbol string, price float, volume int); " +
                                "define window cseEventWindow (symbol string, price float, volume int) cron('*/5 * * * * ?'); ";

        String query = "@info(name = 'query0') " +
                "from cseEventStream " +
                "insert into cseEventWindow; " +
                "" +
                "@info(name = 'query1') from cseEventWindow " +
                "select symbol,price,volume " +
                "insert into outputStream ;";

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(cseEventStream + query);

        executionPlanRuntime.addCallback("outputStream", new StreamCallback() {

            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    if (event.isExpired()) {
                        removeEventCount.incrementAndGet();
                    } else {
                        inEventCount.incrementAndGet();
                    }
                }
                eventArrived = true;
            }
        });


        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"IBM", 700f, 0});
        inputHandler.send(new Object[]{"WSO2", 60.5f, 1});
        Thread.sleep(6000);
        inputHandler.send(new Object[]{"IBM1", 700f, 0});
        inputHandler.send(new Object[]{"WSO22", 60.5f, 1});
        Thread.sleep(6000);
        inputHandler.send(new Object[]{"IBM43", 700f, 0});
        inputHandler.send(new Object[]{"WSO4343", 60.5f, 1});
        SiddhiTestHelper.waitForEvents(1000, 6, inEventCount, 10000);
        Assert.assertEquals(6, inEventCount.intValue());
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();

    }


    @Test
    public void testCronWindow2() throws InterruptedException {
        log.info("Testing cron window for expired events");

        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "define stream cseEventStream (symbol string, price float, volume int); " +
                "define window cseEventWindow (symbol string, price float, volume int) cron('*/5 * * * * ?') output expired events; ";

        String query = "@info(name = 'query0') " +
                "from cseEventStream " +
                "insert into cseEventWindow; " +
                "" +
                "@info(name = 'query1') from cseEventWindow " +
                "select symbol,price,volume " +
                "insert expired events into outputStream ;";

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(cseEventStream + query);

        executionPlanRuntime.addCallback("outputStream", new StreamCallback() {

            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                        removeEventCount.incrementAndGet();
                }
                eventArrived = true;
            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"IBM", 700f, 0});
        inputHandler.send(new Object[]{"WSO2", 60.5f, 1});
        Thread.sleep(7000);
        inputHandler.send(new Object[]{"IBM1", 700f, 0});
        inputHandler.send(new Object[]{"WSO22", 60.5f, 1});
        Thread.sleep(7000);
        inputHandler.send(new Object[]{"IBM43", 700f, 0});
        inputHandler.send(new Object[]{"WSO4343", 60.5f, 1});
        SiddhiTestHelper.waitForEvents(1000, 4, removeEventCount, 10000);
        Assert.assertEquals(4, removeEventCount.intValue());
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();

    }


}