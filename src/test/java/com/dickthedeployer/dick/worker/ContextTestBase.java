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
package com.dickthedeployer.dick.worker;

import com.dickthedeployer.dick.worker.facade.DickWebFacade;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author mariusz
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@WebIntegrationTest
@SpringApplicationConfiguration(classes = {ContextTestBase.class, ApplicationConfig.class})
public class ContextTestBase {

    @Bean
    public DickWebFacade dickWebFacade() {
        return mock(DickWebFacade.class);
    }

    public static boolean isWindows() {
        log.debug("OS NAME {}", System.getProperty("os.name").toLowerCase());
        return (System.getProperty("os.name").toLowerCase().contains("win"));
    }
}
