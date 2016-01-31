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
import com.dickthedeployer.dick.worker.facade.model.BuildForm;
import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import com.dickthedeployer.dick.worker.facade.model.BuildStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
    public void shouldBuildEvenIfDickWebCheckStatusFails() {
        when(dickWebFacade.checkStatus(eq(123L))).thenThrow(new RuntimeException());

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(singletonMap("FOO", "foo"))
                .commands(produceCommands())
                .build()
        );

        sleep(7, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq(123L), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq(123L), any());
        verify(dickWebFacade, times(2)).checkStatus(eq(123L));
    }

    @Test
    public void shouldBuildEvenIfDickWebReportProgressFails() {
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus());
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportProgress(eq(123L), any());

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(singletonMap("FOO", "foo"))
                .commands(produceCommands())
                .build()
        );

        sleep(7, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq(123L), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq(123L), any());
        verify(dickWebFacade, times(2)).checkStatus(eq(123L));
    }

    @Test
    public void shouldBuildEvenIfDickWebReportSuccessFails() {
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus());
        Mockito.doThrow(new RuntimeException()).doNothing()
                .when(dickWebFacade).reportSuccess(eq(123L), any());

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(singletonMap("FOO", "foo"))
                .commands(produceCommands())
                .build()
        );

        sleep(7, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq(123L), any());
        verify(dickWebFacade, times(2)).reportSuccess(eq(123L), any());
        verify(dickWebFacade, times(2)).checkStatus(eq(123L));
    }

    @Test
    public void shouldBuildEvenIfDickWebReportFailureFails() {
        Mockito.doThrow(new RuntimeException()).doNothing()
                .when(dickWebFacade).reportFailure(eq(123L), any());
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus());

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(emptyMap())
                .commands(produceErrorCommands())
                .build()
        );

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportFailure(eq(123L), any());
        verify(dickWebFacade, times(1)).checkStatus(eq(123L));
    }

    @Test
    public void shouldBuildSuccessfullyCheckingIfShouldStop() {
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus());

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(singletonMap("FOO", "foo"))
                .commands(produceCommands())
                .build()
        );

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq(123L), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq(123L), any());
        verify(dickWebFacade, times(2)).checkStatus(eq(123L));
    }

    @Test
    public void shouldReportErrorCheckingIfShouldStop() {
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus());

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(emptyMap())
                .commands(produceErrorCommands())
                .build()
        );

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(1)).reportFailure(eq(123L), any());
        verify(dickWebFacade, times(1)).checkStatus(eq(123L));
    }

    @Test
    public void shouldStopBuildOnSignalFromWeb() {
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus(true));

        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(emptyMap())
                .commands(produceCommands())
                .build()
        );

        sleep(10, TimeUnit.SECONDS);

        ArgumentCaptor<BuildForm> captor = ArgumentCaptor.forClass(BuildForm.class);
        verify(dickWebFacade, times(1)).reportProgress(any(), any());
        verify(dickWebFacade, times(0)).reportSuccess(any(), any());
        verify(dickWebFacade, times(1)).reportFailure(eq(123L), captor.capture());
        verify(dickWebFacade, times(1)).checkStatus(eq(123L));
        if (isWindows()) {
            assertThat(captor.getValue().getLog()).isEqualTo(
                    "Executing command: [cmd.exe, /c, echo, %FOO%]\n"
                    + "%FOO%\n"
                    + "Executing command: [cmd.exe, /c, ping, 127.0.0.1, -n, 4, >, nul]\n"
            );
        }
    }

    @Test
    public void shouldStopBuildOnTimeout() {
        when(dickWebFacade.checkStatus(eq(123L))).thenReturn(new BuildStatus());
        workerService.performBuild(BuildOrder.builder()
                .buildId(123L)
                .environment(emptyMap())
                .commands(produceCommandsWithTimeout(100))
                .build()
        );

        sleep(15, TimeUnit.SECONDS);

        ArgumentCaptor<BuildForm> captor = ArgumentCaptor.forClass(BuildForm.class);
        verify(dickWebFacade, times(1)).reportProgress(any(), any());
        verify(dickWebFacade, times(0)).reportSuccess(any(), any());
        verify(dickWebFacade, times(1)).reportFailure(eq(123L), captor.capture());
        verify(dickWebFacade, times(3)).checkStatus(eq(123L));
    }

}
