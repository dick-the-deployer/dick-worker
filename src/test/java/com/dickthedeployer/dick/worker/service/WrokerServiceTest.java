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
import static com.dickthedeployer.dick.worker.ContextTestBase.isWindows;
import com.dickthedeployer.dick.worker.facade.DickWebFacade;
import com.dickthedeployer.dick.worker.facade.model.DeploymentStatus;
import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import java.util.List;
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
public class WrokerServiceTest extends ContextTestBase {

    @Autowired
    WorkerService workerService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Before
    public void prepare() {
        reset(dickWebFacade);
    }

    @Test
    public void shouldDeploySucessfullyCheckingIfShouldStop() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus());

        workerService.performDeployment("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldReportErrorCheckingIfShouldStop() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus());
        workerService.performDeployment("someId",
                produceErrorCommands(),
                emptyMap());

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), any());
        verify(dickWebFacade, times(1)).checkStatus(eq("someId"));
        
    }
    private List<String> produceErrorCommands() {
        if (isWindows()) {
            return asList("cmd.exe /c return 1");
        } else {
            return asList("return 1");
        }
    }
    
    private List<String> produceCommands() {
        if (isWindows()) {
            return asList("cmd.exe /c echo %FOO%",
                    "cmd.exe /c ping 127.0.0.1 -n 4 > nul",
                    "cmd.exe /c echo bar");
        } else {
            return asList("echo $FOO",
                    "sleep 4",
                    "echo bar");
        }
    }
}
