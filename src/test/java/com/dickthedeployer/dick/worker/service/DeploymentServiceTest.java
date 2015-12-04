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
import com.dickthedeployer.dick.worker.facade.model.DeploymentForm;
import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mariusz
 */
public class DeploymentServiceTest extends ContextTestBase {

    @Autowired
    DeploymentService deploymentService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Before
    public void prepare() {
        reset(dickWebFacade);
    }

    @Test
    public void shouldStartContext() {

        deploymentService.deploy("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(6, TimeUnit.SECONDS);

        ArgumentCaptor<DeploymentForm> captor = ArgumentCaptor.forClass(DeploymentForm.class);
        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), captor.capture());

        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue().getLog()).isEqualTo(produceOutput());
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

    private String produceOutput() {
        if (isWindows()) {
            return "Executing command: [cmd.exe, /c, echo, %FOO%]\n"
                    + "Setting environment variable: FOO=foo\n"
                    + "foo\n"
                    + "Executing command: [cmd.exe, /c, ping, 127.0.0.1, -n, 4, >, nul]\n"
                    + "Setting environment variable: FOO=fooExecuting command: [cmd.exe, /c, echo, bar]\n"
                    + "Setting environment variable: FOO=foo\n"
                    + "bar";
        } else {
            return "Executing command: [echo, %FOO%]\n"
                    + "Setting environment variable: FOO=foo\n"
                    + "foo\n"
                    + "Executing command: [sleep, 4]\n"
                    + "Setting environment variable: FOO=fooExecuting command: [echo, bar]\n"
                    + "Setting environment variable: FOO=foo\n"
                    + "bar";
        }
    }
}
