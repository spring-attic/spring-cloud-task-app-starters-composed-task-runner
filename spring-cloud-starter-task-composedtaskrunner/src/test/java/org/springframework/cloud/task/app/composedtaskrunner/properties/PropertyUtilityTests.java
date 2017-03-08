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

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Glenn Renfro
 */
public class PropertyUtilityTests {

	@Test
	public void retrieveNoSpace() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		properties.setComposedTaskProperties("app.foo.prop1=aaa,app.foo.prop2=bbb,app.bar.prop1=ccc,app.foo.prop3=ddd");
		Map<String, String> result = PropertyUtility.getPropertiesForTask("foo", properties);
		assertEquals(3, result.size());
		assertEquals("aaa", result.get("prop1"));
		assertEquals("bbb", result.get("prop2"));
		assertEquals("ddd", result.get("prop3"));

		result = PropertyUtility.getPropertiesForTask("bar", properties);
		assertEquals(1, result.size());
		assertEquals("ccc", result.get("prop1"));
	}

	@Test
	public void retrieveSingleSpace() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		properties.setComposedTaskProperties("app.foo.prop1=aaa, app.foo.prop2=bbb, app.bar.prop1=ccc, app.foo.prop3=ddd");
		Map<String, String> result = PropertyUtility.getPropertiesForTask("foo", properties);
		assertEquals(3, result.size());
		assertEquals("aaa", result.get("prop1"));
		assertEquals("bbb", result.get("prop2"));
		assertEquals("ddd", result.get("prop3"));

		result = PropertyUtility.getPropertiesForTask("bar", properties);
		assertEquals(1, result.size());
		assertEquals("ccc", result.get("prop1"));
	}

	@Test
	public void retrieveWithQuotes() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		properties.setComposedTaskProperties("app.foo.prop1=\"aaa aaa\", app.foo.prop2=bbb, app.bar.prop1='ccc ccc', app.foo.prop3=ddd");
		Map<String, String> result = PropertyUtility.getPropertiesForTask("foo", properties);
		assertEquals(3, result.size());
		assertEquals("\"aaa aaa\"", result.get("prop1"));
		assertEquals("bbb", result.get("prop2"));
		assertEquals("ddd", result.get("prop3"));

		result = PropertyUtility.getPropertiesForTask("bar", properties);
		assertEquals(1, result.size());
		assertEquals("'ccc ccc'", result.get("prop1"));
	}

}
