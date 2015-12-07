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
import com.dickthedeployer.dick.worker.facade.model.RegistrationData;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author mariusz
 */
@Service
@Slf4j
public class SchedulerService {

    @Autowired
    DickWebClient dickWebClient;

    @Autowired
    WorkerService workerService;

    @Value("${dick.worker.name:}")
    String dickWorkerName;

    @Value("${user.home}")
    String userHome;

    @PostConstruct
    public void init() {
        if (dickWorkerName == null) {
            RegistrationData data = dickWebClient.register();
            log.info("Obtained name from web {}", data.getName());
            dickWorkerName = "dick.worker.name=" + data.getName();
            try {
                Files.write(Paths.get(userHome + "/worker.properties"), dickWorkerName.getBytes("utf-8"), StandardOpenOption.CREATE);
            } catch (IOException ex) {
                log.error("Creating property file with name failed!", ex);
                throw Throwables.propagate(ex);
            }
        }
    }

    @Scheduled(fixedRateString = "${dick.worker.peek.interval:3000}")
    public void sheduleWork() {
        if (dickWorkerName != null) {
            dickWebClient.peekBuild(dickWorkerName)
                    .ifPresent(order -> workerService.performBuild(order.getBuildId(), order.getCommands(), order.getEnvironment()));
        }
    }
}
