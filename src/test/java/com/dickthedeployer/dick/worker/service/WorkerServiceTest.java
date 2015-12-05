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
import com.dickthedeployer.dick.worker.facade.model.DeploymentForm;
import com.dickthedeployer.dick.worker.facade.model.DeploymentStatus;
import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mariusz
 */
public class WorkerServiceTest extends ContextTestBase {

    @Autowired
    WorkerService workerService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Before
    public void prepare() {
        reset(dickWebFacade);
    }

    @Test
    public void shouldDeployEvenIfDickWebCheckStatusFails() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenThrow(new RuntimeException());

        workerService.performDeployment("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(4, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldDeployEvenIfDickWebReportProgressFails() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus());
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportProgress(eq("someId"), any());

        workerService.performDeployment("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(4, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(1)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldDeployEvenIfDickWebReportSuccessFails() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus());
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportSuccess(eq("someId"), any());

        workerService.performDeployment("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(4, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldDeployEvenIfDickWebReportFailureFails() {
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportFailure(eq("someId"), any());
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus());

        workerService.performDeployment("someId",
                produceErrorCommands(),
                emptyMap());

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
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

    @Test
    public void shouldStopDeploymentOnSignalFromWeb() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus(true));
        workerService.performDeployment("someId",
                produceCommands(),
                emptyMap());

        sleep(10, TimeUnit.SECONDS);

        ArgumentCaptor<DeploymentForm> captor = ArgumentCaptor.forClass(DeploymentForm.class);
        verify(dickWebFacade, times(1)).reportProgress(any(), any());
        verify(dickWebFacade, times(0)).reportSuccess(any(), any());
        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), captor.capture());
        verify(dickWebFacade, times(1)).checkStatus(eq("someId"));
        if (isWindows()) {
            assertThat(captor.getValue().getLog()).isEqualTo(
                    "Executing command: [cmd.exe, /c, echo, %FOO%]\n"
                    + "%FOO%\n"
                    + "Executing command: [cmd.exe, /c, ping, 127.0.0.1, -n, 4, >, nul]\n"
            );
        }
    }

    @Test
    public void shouldStopDeploymentOnTimeout() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new DeploymentStatus());
        workerService.performDeployment("someId",
                produceCommandsWithTimeout(100),
                emptyMap());

        sleep(15, TimeUnit.SECONDS);

        ArgumentCaptor<DeploymentForm> captor = ArgumentCaptor.forClass(DeploymentForm.class);
        verify(dickWebFacade, times(1)).reportProgress(any(), any());
        verify(dickWebFacade, times(0)).reportSuccess(any(), any());
        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), captor.capture());
        verify(dickWebFacade, times(3)).checkStatus(eq("someId"));
    }

    private List<String> produceErrorCommands() {
        if (isWindows()) {
            return asList("cmd.exe /c return 1");
        } else {
            return asList("return 1");
        }
    }

    private List<String> produceCommands() {
        return produceCommandsWithTimeout(3);
    }

    private List<String> produceCommandsWithTimeout(int timeout) {
        if (isWindows()) {
            return asList("cmd.exe /c echo %FOO%",
                    "cmd.exe /c ping 127.0.0.1 -n " + (timeout + 1) + " > nul",
                    "cmd.exe /c echo bar");
        } else {
            return asList("echo $FOO",
                    "sleep " + timeout,
                    "echo bar");
        }
    }
}
