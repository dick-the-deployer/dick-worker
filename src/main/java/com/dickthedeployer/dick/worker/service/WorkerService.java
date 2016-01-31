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

import com.dickthedeployer.dick.worker.command.Command;
import com.dickthedeployer.dick.worker.command.CommandChainFactory;
import com.dickthedeployer.dick.worker.facade.DickWebClient;
import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author mariusz
 */
@Slf4j
@Service
public class WorkerService {

    @Autowired
    BuildService buildService;

    @Autowired
    DickWebClient dickWebClient;

    @Autowired
    CommandChainFactory commandChainFactory;

    @Value("${dick.worker.status.interval:3}")
    long interval;

    @Value("${dick.worker.job.duration:86400}")
    long maxDuration;

    public void performBuild(BuildOrder buildOrder) {
        List<Command> commands = commandChainFactory.produceCommands(buildOrder);
        Subscription deploymentSubscription = buildService.build(buildOrder.getBuildId(), commands);
        Observable.interval(interval, TimeUnit.SECONDS)
                .take(maxDuration, TimeUnit.SECONDS)
                .map(tick -> dickWebClient.checkStatus(buildOrder.getBuildId()).isStopped())
                .subscribe(new TimeoutAndCancelGuardingSubscriber(deploymentSubscription));

    }

    private class TimeoutAndCancelGuardingSubscriber extends Subscriber<Boolean> {

        private final Subscription deploymentSubscription;

        public TimeoutAndCancelGuardingSubscriber(Subscription deploymentSubscription) {
            this.deploymentSubscription = deploymentSubscription;
        }

        @Override
        public void onCompleted() {
            log.debug("Execution timeouted, unsubscribing");
            unsubscribeFromObservables();
        }

        @Override
        public void onError(Throwable ex) {
            log.error("Something went wrong on build guarding observable", ex);
            unsubscribeFromObservables();
        }

        @Override
        public void onNext(Boolean shouldStop) {
            boolean unsubscribed = deploymentSubscription.isUnsubscribed();
            log.debug("Checking if should stop: {} or is finished: {}", shouldStop, unsubscribed);
            if (shouldStop || unsubscribed) {
                unsubscribeFromObservables();
            }
        }

        private void unsubscribeFromObservables() {
            deploymentSubscription.unsubscribe();
            unsubscribe();
        }
    }
}
