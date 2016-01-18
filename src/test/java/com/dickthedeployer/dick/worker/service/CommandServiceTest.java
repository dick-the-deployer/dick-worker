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

import com.dickthedeployer.dick.worker.util.ArgumentTokenizer;
import org.junit.Test;
import rx.Observable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dickthedeployer.dick.worker.ContextTestBase.isWindows;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 *
 * @author mariusz
 */
public class CommandServiceTest {

    CommandService commandService = new CommandService();

    @Test
    public void shouldEmmitProperOutput() throws IOException {
        commandService.maxDuration = 1000;

        Path temp = Files.createTempDirectory("deployment");
        StringBuilder stringBuffer = new StringBuilder();

        if (isWindows()) {
            Observable.just("cmd.exe /c echo foo", "cmd.exe /c echo bar", "cmd.exe /c echo foo2")
                    .flatMap(command -> commandService.invokeWithEnvironment(temp, emptyMap(), command.split(" ")))
                    .subscribe(result -> stringBuffer.append(result));

            assertThat(stringBuffer.toString()).isEqualTo("Executing command: [cmd.exe, /c, echo, foo]fooExecuting command: [cmd.exe, /c, echo, bar]barExecuting command: [cmd.exe, /c, echo, foo2]foo2");
        } else {
            Observable.just("echo foo", "echo bar", "echo foo2")
                    .flatMap(command -> commandService.invokeWithEnvironment(temp, emptyMap(), command.split(" ")))
                    .subscribe(result -> stringBuffer.append(result));

            assertThat(stringBuffer.toString()).isEqualTo("Executing command: [echo, foo]fooExecuting command: [echo, bar]barExecuting command: [echo, foo2]foo2");
        }
    }

    @Test
    public void shouldMonitorErrorStream() throws IOException {
        commandService.maxDuration = 1000;

        Path temp = Files.createTempDirectory("deployment");
        StringBuilder stringBuffer = new StringBuilder();
        if (!isWindows()) {
            Observable.just("bash -c \"echo foo 1>&2\"")
                    .flatMap(command ->
                            commandService.invokeWithEnvironment(temp, emptyMap(), ArgumentTokenizer.tokenize(command).toArray(new String[0])))
                    .subscribe(result -> stringBuffer.append(result));
        }
        assertThat(stringBuffer.toString()).isEqualTo("Executing command: [bash, -c, echo foo 1>&2]foo");

    }
}
