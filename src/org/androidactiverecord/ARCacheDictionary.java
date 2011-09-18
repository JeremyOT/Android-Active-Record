//   Copyright 2011 Jeremy Olmsted-Thompson
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   
package org.androidactiverecord;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A weak reference dictionary used for caching inflated entities.
 * 
 * @author Jeremy Olmsted-Thompson
 */
class ARCacheDictionary {
	private ConcurrentHashMap<String, WeakReference<AREntity>> map = new ConcurrentHashMap<String, WeakReference<AREntity>>();

	@SuppressWarnings("unchecked")
	<T extends AREntity> T get(Class<T> c, long id) {
		WeakReference<AREntity> i = map.get(String.format("%s%d", c.getName(), id));
		if (i == null)
			return null;
		return (T) i.get();
	}

	void set(AREntity e) {
		map.put(String.format("%s%d", e.getClass().getName(), e.getId()), new WeakReference<AREntity>(e));
	}
}
