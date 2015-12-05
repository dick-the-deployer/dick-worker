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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 *
 * @author mariusz
 */
@Slf4j
@Service
public class WorkerService {

    @Autowired
    DeploymentService deploymentService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Value("${dick.worker.status.interval:3}")
    long interval;

    @Value("${dick.worker.job.duration:60}")
    long maxDuration;

    public void performDeployment(String deploymentId, List<String> commands, Map<String, String> environment) {
        Subscription deploymentSubscribtion = deploymentService.deploy(deploymentId, commands, environment);
        Observable.interval(interval, TimeUnit.SECONDS)
                .take(maxDuration, TimeUnit.MINUTES)
                .map(tick -> dickWebFacade.checkStatus(deploymentId).isStopped())
                .subscribe(new TimeoutAndCancellGuardingSubscriber(deploymentSubscribtion));

    }

    private class TimeoutAndCancellGuardingSubscriber extends Subscriber<Boolean> {

        private final Subscription deploymentSubscribtion;

        public TimeoutAndCancellGuardingSubscriber(Subscription deploymentSubscribtion) {
            this.deploymentSubscribtion = deploymentSubscribtion;
        }

        @Override
        public void onCompleted() {
            unsubscribeFromObservables();
        }

        @Override
        public void onError(Throwable ex) {
            log.error("Something went wrong on deployment guarding observable", ex);
            unsubscribeFromObservables();
        }

        @Override
        public void onNext(Boolean shouldStop) {
            boolean unsubscribed = deploymentSubscribtion.isUnsubscribed();
            log.debug("Checking if should stop: {} or is finished: {}", shouldStop, unsubscribed);
            if (shouldStop || unsubscribed) {
                unsubscribeFromObservables();
            }
        }

        private void unsubscribeFromObservables() {
            deploymentSubscribtion.unsubscribe();
            unsubscribe();
        }
    }
}
