// Copyright 2014 Cognitect. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cognitect.transit;

/**
 * Provides a MapReader to Transit to use in incrementally parsing
 * a map representation of a value.
 */
public interface MapReadHandler<G,A,K,V,R> extends ReadHandler<A,R> {
    /**
     * Provides a MapReader that a parser can use to convert
     * a map representation to an instance of a type incrementally
     * @return a MapReader
     */
    MapReader<G,A,K,V> mapReader();
}
