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

import com.dickthedeployer.dick.worker.facade.DickWebClient;
import com.dickthedeployer.dick.worker.facade.model.BuildForm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

/**
 *
 * @author mariusz
 */
@Slf4j
@Service
public class BuildService {

    @Autowired
    CommandService commandService;

    @Autowired
    DickWebClient dickWebClient;

    @Value("${dick.worker.report.timespan:2}")
    long timespan;

    public Subscription build(Long buildId, List<String> commands, Map<String, String> environment) {
        StringBuffer buffer = new StringBuffer();
        try {
            Path temp = Files.createTempDirectory("build-" + buildId);
            return Observable.just(commands)
                    .concatMap(Observable::from)
                    .map(command -> command.split(" "))
                    .concatMap(commandArray
                            -> commandService.invokeWithEnvironment(temp, environment, commandArray)
                    )
                    .doOnNext(logLine -> buffer.append(logLine).append("\n"))
                    .buffer(timespan, TimeUnit.SECONDS)
                    .filter(logLines -> !logLines.isEmpty())
                    .map(logLines -> StringUtils.collectionToDelimitedString(logLines, "\n"))
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(logLines -> onProgress(buildId, logLines),
                            ex -> processError(buildId, ex, buffer),
                            () -> completeDeployment(buildId, buffer));
        } catch (IOException ex) {
            processError(buildId, ex, buffer);
            return Subscriptions.empty();
        }
    }

    private void onProgress(Long buildId, String logLines) {
        log.debug("Reporting progress on {} with \n {}", buildId, logLines);
        dickWebClient.reportProgress(buildId, new BuildForm(logLines));
    }

    private void processError(Long buildId, Throwable ex, StringBuffer buffer) {
        log.info("Build failed on:" + buildId, ex);
        dickWebClient.reportFailure(buildId, new BuildForm(buffer.toString()));
    }

    private void completeDeployment(Long buildId, StringBuffer buffer) {
        dickWebClient.reportSuccess(buildId, new BuildForm(buffer.toString()));
    }

}
