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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;

/**
 *
 * @author mariusz
 */
@Slf4j
@Service
public class CommandService {

    public Observable<String> invokeWithEnvironment(Path workingDir, Map<String, String> environment, String... command) throws RuntimeException {
        return Observable.create((Subscriber<? super String> observer) -> {
            try {
                log.info("Executing command {} in path {}", Arrays.toString(command), workingDir.toString());
                observer.onNext(
                        new StringBuilder().append("Executing command: ").append(Arrays.toString(command)).toString()
                );

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(workingDir.toFile());
                builder.redirectErrorStream(true);
                environment.forEach((key, value)
                        -> observer.onNext(
                                new StringBuilder().append("Setting environment variable: ").append(key).append("=").append(value).toString()
                        )
                );
                builder.environment().putAll(environment);
                Process process = builder.start();

                try (Scanner s = new Scanner(process.getInputStream())) {
                    while (s.hasNextLine()) {
                        String nextLine = s.nextLine();
                        log.debug("Emitting: {}", nextLine);
                        observer.onNext(
                                nextLine
                        );
                    }
                    int result = process.waitFor();
                    log.info("Process exited with result {}", result);

                    if (result != 0) {
                        observer.onError(new ProcessExitedWithNotZeroException("Exited with not zero on " + Arrays.toString(command)));
                    }
                }

                observer.onCompleted();
            } catch (IOException | InterruptedException e) {
                observer.onError(e);
            }
        });
    }
}
