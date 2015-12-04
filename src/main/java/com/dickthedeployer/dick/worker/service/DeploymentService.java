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

import com.dickthedeployer.dick.worker.facade.DickWebFacade;
import com.dickthedeployer.dick.worker.facade.model.DeploymentForm;
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
import rx.subscriptions.Subscriptions;

/**
 *
 * @author mariusz
 */
@Slf4j
@Service
public class DeploymentService {

    @Autowired
    CommandService commandService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Value("${dick.worker.report.timespan:1}")
    long timespan;

    public Subscription deploy(String deploymentId, List<String> commands, Map<String, String> environment) {
        StringBuffer buffer = new StringBuffer();
        try {
            Path temp = Files.createTempDirectory("deployment-" + deploymentId);
            return Observable.just(commands)
                    .concatMap(Observable::from)
                    .map(command -> command.split(" "))
                    .concatMap(commandArray
                            -> commandService.invokeWithEnvironment(temp, environment, commandArray)
                    ).buffer(timespan, TimeUnit.SECONDS)
                    .filter(logLines -> !logLines.isEmpty())
                    .map(logLines -> StringUtils.collectionToDelimitedString(logLines, "\n"))
                    .doOnNext(buffer::append)
                    .subscribe(logLines -> onProgress(deploymentId, logLines),
                            ex -> processError(deploymentId, ex, buffer),
                            () -> completeDeployment(deploymentId, buffer));
        } catch (IOException ex) {
            processError(deploymentId, ex, buffer);
            return Subscriptions.empty();
        }
    }

    private void onProgress(String deploymentId, String logLines) {
        log.debug("Reporting progress on {} with \n {}", deploymentId, logLines);
        dickWebFacade.reportProgress(deploymentId, new DeploymentForm(logLines));
    }

    private void processError(String deploymentId, Throwable ex, StringBuffer buffer) {
        log.info("Deployment failed", ex);
        dickWebFacade.reportFailure(deploymentId, new DeploymentForm(buffer.toString()));
    }

    private void completeDeployment(String deploymentId, StringBuffer buffer) {
        dickWebFacade.reportSuccess(deploymentId, new DeploymentForm(buffer.toString()));
    }

}
