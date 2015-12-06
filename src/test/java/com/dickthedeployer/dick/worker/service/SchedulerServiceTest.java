/*
 * Copyright dick the deployer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dickthedeployer.dick.worker.service;

import com.dickthedeployer.dick.worker.ContextTestBase;
import com.dickthedeployer.dick.worker.facade.DickWebFacade;
import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import com.dickthedeployer.dick.worker.facade.model.BuildStatus;
import com.dickthedeployer.dick.worker.facade.model.RegistrationData;
import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Collections.singletonMap;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mariusz
 */
public class SchedulerServiceTest extends ContextTestBase {

    @Autowired
    DickWebFacade dickWebFacade;

    @Autowired
    SchedulerService schedulerService;

    @Before
    public void prepare() {
        reset(dickWebFacade);
    }

    @Test
    public void shouldCreatePropertiesName() {
        schedulerService.dickWorkerName = null;
        when(dickWebFacade.register()).thenReturn(new RegistrationData("test-build-dude"));
        schedulerService.init();

        System.err.println("");
    }

    @Test
    public void shouldPeekFromWebAndBuild() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());
        when(dickWebFacade.peekBuild(eq("test-build-dude")))
                .thenReturn(new BuildOrder("someId", produceCommands(), singletonMap("FOO", "foo")))
                .thenReturn(null);

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }
}
