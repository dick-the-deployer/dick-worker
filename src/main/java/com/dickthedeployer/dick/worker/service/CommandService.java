/*
 * Copyright 2015 dick the deployer.
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

import com.dickthedeployer.dick.worker.exception.ProcessExitedWithNotZeroException;
import com.dickthedeployer.dick.worker.facade.model.EnvironmentVariable;
import com.google.common.base.Throwables;
import com.watchrabbit.commons.marker.Feature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.toMap;

/**
 * @author mariusz
 */
@Slf4j
@Service
public class CommandService {

    @Value("${dick.worker.job.duration:86400}")
    long maxDuration = 86400;

    public Observable<String> invokeWithEnvironment(Path workingDir, List<EnvironmentVariable> environment, boolean isSecure, String... command) throws RuntimeException {
        return Observable.create((Subscriber<? super String> observer) -> {
            try {

                emmitCommand(workingDir, observer, command, isSecure);

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(workingDir.toFile());
                builder.redirectErrorStream(true);
                emmitEnvironment(environment, observer);
                Map<String, String> collectedEnvironment = environment.stream()
                        .collect(toMap(EnvironmentVariable::getName, EnvironmentVariable::getValue));
                builder.environment().putAll(collectedEnvironment);
                Process process = builder.start();
                try {
                    Executors.newSingleThreadExecutor().submit(() -> {
                        watchForProcess(process, observer, command);
                        observer.onCompleted();
                    }).get(maxDuration, TimeUnit.SECONDS);
                } finally {
                    process.destroyForcibly();
                }
            } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
                observer.onError(e);
            }
        });
    }

    private void emmitCommand(Path workingDir, Subscriber<? super String> observer, String[] command, boolean isSecure) {
        if (isSecure) {
            log.info("Executing command in path {}", workingDir.toString());
            observer.onNext("Executing command.");
        } else {
            log.info("Executing command {} in path {}", Arrays.toString(command), workingDir.toString());
            observer.onNext(
                    new StringBuilder().append("Executing command: ").append(Arrays.toString(command)).toString()
            );
        }
    }

    private void emmitEnvironment(List<EnvironmentVariable> environment, Subscriber<? super String> observer) {
        for (EnvironmentVariable variable : environment) {
            if (variable.isSecure()) {
                observer.onNext(
                        new StringBuilder().append("Setting environment variable: ")
                                .append(variable.getName())
                                .append("=")
                                .append("***********************").toString()
                );
            } else {
                observer.onNext(
                        new StringBuilder().append("Setting environment variable: ")
                                .append(variable.getName())
                                .append("=")
                                .append(variable.getValue()).toString()
                );
            }
        }
    }

    @Feature("Here is possible thread/memory leak, when reading output from process that never ends thread working on this method will be blocked also")
    private void watchForProcess(Process process, Subscriber<? super String> observer, String[] command) throws RuntimeException {
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            readProcessOutput(scanner, observer);
            analyzeProcessCode(process, observer, command);
        } catch (InterruptedException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private void readProcessOutput(final Scanner s, Subscriber<? super String> observer) {
        while (s.hasNextLine() && !Thread.currentThread().isInterrupted()) {
            String nextLine = s.nextLine();
            log.debug("Emitting: {}", nextLine);
            observer.onNext(
                    nextLine
            );
        }
    }

    private void analyzeProcessCode(Process process, Subscriber<? super String> observer, String[] command) throws InterruptedException {
        int result = process.waitFor();
        log.info("Process exited with result {}", result);

        if (result != 0) {
            observer.onNext(
                    new StringBuilder().append("\nCommand exited with non-zero: ").append(result).append("\n").toString()
            );
            observer.onError(new ProcessExitedWithNotZeroException("Exited with not zero on " + Arrays.toString(command)));
        }
    }

}
