/*
 *
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.app.composedtaskrunner.properties;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Glenn Renfro
 */
public class ComposedTaskPropertiesTests {

	@Test
	public void testGettersAndSetters() throws URISyntaxException{
		ComposedTaskProperties properties = new ComposedTaskProperties();
		properties.setComposedTaskProperties("aaa");
		properties.setComposedTaskArguments("bbb");
		properties.setIntervalTimeBetweenChecks(12345);
		properties.setMaxWaitTime(6789);
		properties.setDataFlowUri(new URI("http://test"));
		properties.setGraph("ddd");
		assertEquals("aaa", properties.getComposedTaskProperties());
		assertEquals("bbb", properties.getComposedTaskArguments());
		assertEquals(12345, properties.getIntervalTimeBetweenChecks());
		assertEquals(6789, properties.getMaxWaitTime());
		assertEquals("http://test", properties.getDataFlowUri().toString());
		assertEquals("ddd", properties.getGraph());

	}

	@Test
	public void testThreadDefaults() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_CORE_POOL_SIZE_DEFAULT, properties.getSplitThreadCorePoolSize());
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_KEEP_ALIVE_SECONDS_DEFAULT, properties.getSplitThreadKeepAliveSeconds());
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_MAX_POOL_SIZE_DEFAULT, properties.getSplitThreadMaxPoolSize());
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_QUEUE_CAPACITY_DEFAULT, properties.getSplitThreadQueueCapacity());
		assertFalse(properties.isSplitThreadAllowCoreThreadTimeout());
		assertFalse(properties.isSplitThreadWaitForTasksToCompleteOnShutdown());
	}
}
