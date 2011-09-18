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

/**
 * A runtime exception thrown when an entity has properties or a constructor that can't be accessed
 * by the supporting AREntity classes.
 * 
 * @author Jeremy Olmsted-Thompson
 */
public class ARInflationException extends RuntimeException {

	private static final long serialVersionUID = 1428814365988669345L;

    public ARInflationException(Throwable throwable) {
        super(throwable);
    }
    public ARInflationException(String message) {
        super(message);
    }
}
