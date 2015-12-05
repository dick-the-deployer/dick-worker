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

import com.dickthedeployer.dick.worker.facade.model.DeploymentForm;
import com.dickthedeployer.dick.worker.facade.model.DeploymentStatus;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
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
    public void reportProgress(String id, DeploymentForm form) {
        dickWebFacade.reportProgress(id, form);
    }

    @HystrixCommand(fallbackMethod = "statusFallback")
    public DeploymentStatus checkStatus(String id) {
        return dickWebFacade.checkStatus(id);
    }

    @HystrixCommand(fallbackMethod = "logFailure")
    public void reportFailure(String id, DeploymentForm form) {
        dickWebFacade.reportFailure(id, form);
    }

    @HystrixCommand(fallbackMethod = "logSuccess")
    public void reportSuccess(String id, DeploymentForm form) {
        dickWebFacade.reportSuccess(id, form);
    }

    public void logProgress(String id, DeploymentForm form) {
        log.error("Cannot send progress to dick web, logging to console {}", form.getLog());
    }

    public void logSuccess(String id, DeploymentForm form) {
        log.error("Cannot send success report to dick web, logging to console {}", form.getLog());
    }

    public void logFailure(String id, DeploymentForm form) {
        log.error("Cannot send failure report to dick web, logging to console {}", form.getLog());
    }

    public DeploymentStatus statusFallback(String id) {
        log.info("Cannot check deployment status using hermes web, assuming ok");
        return new DeploymentStatus(false);
    }

}
