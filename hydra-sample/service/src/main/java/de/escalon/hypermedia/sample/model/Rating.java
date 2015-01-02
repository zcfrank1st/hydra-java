/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.sample.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.action.Input;

/**
 * Sample rating.
 * Created by dschulten on 09.12.2014.
 */
public class Rating {
    public final static String bestRating = "5";
    public final static String worstRating = "1";
    private String ratingValue;

    @JsonCreator
    public Rating(@JsonProperty("ratingValue") String ratingValue) {
        this.ratingValue = ratingValue;
    }

    public void setRatingValue(@Input(min = 1, max = 5, step = 1) String ratingValue) {
        this.ratingValue = ratingValue;
    }

    public String getRatingValue() {
        return ratingValue;
    }
}