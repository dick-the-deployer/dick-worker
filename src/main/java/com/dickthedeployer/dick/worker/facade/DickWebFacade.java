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
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author mariusz
 */
@FeignClient(url = "${dick.web.url}")
public interface DickWebFacade {

    @RequestMapping(value = "/api/internal/job/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    void reportProgress(@PathVariable("id") Long id, @RequestBody BuildForm form);

    @RequestMapping(value = "/api/internal/job/{id}/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    BuildStatus checkStatus(@PathVariable("id") Long id);

    @RequestMapping(value = "/api/internal/job/{id}/failure", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    void reportFailure(@PathVariable("id") Long id, @RequestBody BuildForm form);

    @RequestMapping(value = "/api/internal/job/{id}/success", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    void reportSuccess(@PathVariable("id") Long id, @RequestBody BuildForm form);

    @RequestMapping(value = "/api/internal/job/peek/{dickWorkerName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    BuildOrder peekBuild(@PathVariable("dickWorkerName") String dickWorkerName);

    @RequestMapping(value = "/api/internal/register", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    RegistrationData register();
}
