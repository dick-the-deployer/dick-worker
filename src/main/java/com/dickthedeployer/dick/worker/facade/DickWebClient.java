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
package com.dickthedeployer.dick.worker.facade;

import com.dickthedeployer.dick.worker.facade.model.BuildForm;
import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import com.dickthedeployer.dick.worker.facade.model.BuildStatus;
import com.dickthedeployer.dick.worker.facade.model.RegistrationData;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author mariusz
 */
@Slf4j
@Service
public class DickWebClient {

    @Autowired
    DickWebFacade dickWebFacade;

    @HystrixCommand(fallbackMethod = "logProgress")
    public void reportProgress(String id, BuildForm form) {
        dickWebFacade.reportProgress(id, form);
    }

    @HystrixCommand(fallbackMethod = "statusFallback")
    public BuildStatus checkStatus(String id) {
        return dickWebFacade.checkStatus(id);
    }

    @HystrixCommand(fallbackMethod = "logFailure")
    public void reportFailure(String id, BuildForm form) {
        dickWebFacade.reportFailure(id, form);
    }

    @HystrixCommand(fallbackMethod = "logSuccess")
    public void reportSuccess(String id, BuildForm form) {
        dickWebFacade.reportSuccess(id, form);
    }

    @HystrixCommand(fallbackMethod = "logBuildPeek")
    public Optional<BuildOrder> peekBuild(String dickWorkerName) {
        return Optional.ofNullable(dickWebFacade.peekBuild(dickWorkerName));
    }

    public RegistrationData register() {
        return dickWebFacade.register();
    }

    public void logProgress(String id, BuildForm form) {
        log.error("Cannot send progress to dick web, logging to console {}", form.getLog());
    }

    public void logSuccess(String id, BuildForm form) {
        log.error("Cannot send success report to dick web, logging to console {}", form.getLog());
    }

    public void logFailure(String id, BuildForm form) {
        log.error("Cannot send failure report to dick web, logging to console {}", form.getLog());
    }

    public BuildStatus statusFallback(String id) {
        log.info("Cannot check build status using hermes web, assuming ok");
        return new BuildStatus(false);
    }

    public Optional<BuildOrder> logBuildPeek(String dickWorkerName) {
        log.info("Cannot peek build order, assuming no work");
        return Optional.empty();
    }

}
