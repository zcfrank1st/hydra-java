/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.github.jsonldjava.core.JsonLdError;
import com.jayway.jsonassert.JsonAssert;
import de.escalon.hypermedia.hydra.JsonLdTestUtils;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.mapping.Term;
import de.escalon.hypermedia.hydra.mapping.Vocab;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;


public class JacksonHydraSerializerTest {

    private ObjectMapper mapper;

    StringWriter w = new StringWriter();


    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        // see https://github.com/json-ld/json-ld.org/issues/76
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        mapper.registerModule(new SimpleModule() {

            public void setupModule(SetupContext context) {
                super.setupModule(context);

                context.addBeanSerializerModifier(new BeanSerializerModifier() {

                    public JsonSerializer<?> modifySerializer(
                            SerializationConfig config,
                            BeanDescription beanDesc,
                            JsonSerializer<?> serializer) {

                        if (serializer instanceof BeanSerializerBase) {
                            return new JacksonHydraSerializer(
                                    (BeanSerializerBase) serializer);
                        } else {
                            return serializer;
                        }
                    }
                });
            }
        });

    }

    @Test
    public void testDefaultVocabIsRendered() throws Exception {

        class Person {
            private String name = "Dietrich Schulten";

            public String getName() {
                return name;
            }
        }

        mapper.writeValue(w, new Person());
        System.out.println(w);
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"" +
                "}" +
                ",\"@type\":\"Person\"," +
                "\"name\":\"Dietrich Schulten\"}"
                , w.toString());
    }

    @Vocab("http://xmlns.com/foaf/0.1/")
    class Person {
        private String name = "Dietrich Schulten";

        public String getName() {
            return name;
        }
    }

    @Test
    public void testFoafVocabIsRendered() throws Exception {

        mapper.writeValue(w, new Person());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://xmlns.com/foaf/0.1/\"" +
                "}" +
                ",\"@type\":\"Person\"," +
                "\"name\":\"Dietrich Schulten\"}"
                , w.toString());
    }

    @Test
    public void testAppliesPackageDefinedVocab() throws IOException {
        mapper.writeValue(w, new de.escalon.hypermedia.hydra.beans.withvocab.Person("1964-08-08", "Schulten"));
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://xmlns.com/foaf/0.1/;\"," +
                "\"surname\":\"http://schema.org/familyName\"" +
                "}," +
                "\"@type\":\"Person\"," +
                "\"birthDate\":\"1964-08-08\"," +
                "\"surname\":\"Schulten\"}", w.toString());
    }

    @Test
    public void testAppliesPackageDefinedTerms() throws IOException, JsonLdError {
        mapper.writeValue(w, new de.escalon.hypermedia.hydra.beans.withterms.Offer());
        assertEquals("{\"@context\":{\"@vocab\":\"http://schema.org/\"," +
                "\"gr\":\"http://purl.org/goodrelations/v1#\"," +
                "\"dc\":\"http://purl.org/dc/elements/1.1/\"," +
                "\"children\":{\"@reverse\":\"http://example.com/vocab#parent\"}," +
                "\"businessFunction\":{" +
                "\"@type\":\"@vocab\"}," +
                "\"RENT\":\"gr:LeaseOut\"," +
                "\"price\":\"gr:hasCurrencyValue\"}," +
                "\"@type\":\"gr:Offering\"," +
                "\"businessFunction\":\"RENT\"," +
                "\"price\":1.99}", w.toString());
        final String newline = System.getProperty("line.separator");
        assertEquals("{" + newline +
                "  \"@type\" : \"http://purl.org/goodrelations/v1#Offering\"," + newline +
                "  \"http://purl.org/goodrelations/v1#hasCurrencyValue\" : 1.99," + newline +
                "  \"http://schema.org/businessFunction\" : {" + newline +
                "    \"@id\" : \"http://purl.org/goodrelations/v1#LeaseOut\"" + newline +
                "  }" + newline +
                "}", JsonLdTestUtils.applyContext(w.toString()));
    }

    @Test
    public void testNestedContextWithDifferentVocab() throws Exception {

        @Vocab("http://purl.org/dc/elements/1.1/")
        @Expose("BibliographicResource")
        class Document {
            public String title = "Moby Dick";
            public Person creator = new Person();
        }

        mapper.writeValue(w, new Document());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://purl.org/dc/elements/1.1/\"" +
                "}" +
                ",\"@type\":\"BibliographicResource\"" +
                ",\"title\":\"Moby Dick\"" +
                ",\"creator\":{" +
                "\"@context\":{" +
                "\"@vocab\":\"http://xmlns.com/foaf/0.1/\"}" +
                ",\"@type\":\"Person\"" +
                ",\"name\":\"Dietrich Schulten\"}}"
                , w.toString());
    }

    @Test
    public void testDefaultVocabWithCustomTerm() throws Exception {

        class Person {
            public String birthDate;
            public String firstName;

            // override field name by schema.org property
            @Expose("familyName")
            public String lastName;

            // override getter by schema.org property
            @Expose("givenName")
            public String getFirstName() {
                return firstName;
            }

            public Person(String birthDate, String firstName, String lastName) {
                this.birthDate = birthDate;
                this.lastName = lastName;
                this.firstName = firstName;
            }
        }


        mapper.writeValue(w, new Person("1964-08-08", "Dietrich", "Schulten"));
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"," +
                "\"lastName\":\"familyName\"," +
                "\"firstName\":\"givenName\"" +
                "}," +
                "\"@type\":\"Person\"," +
                "\"birthDate\":\"1964-08-08\"," +
                "\"firstName\":\"Dietrich\"," +
                "\"lastName\":\"Schulten\"}"
                , w.toString());

    }

    class Movie {
        public String name = "Pirates of the Caribbean";
        public String description = "Jack Sparrow and Barbossa embark on a quest.";
        public String model = "http://www.imdb.com/title/tt0325980/";
        public List<Offer> offers = Arrays.asList(new Offer());
    }

    @Term(define = "gr", as = "http://purl.org/goodrelations/v1#")
    class Offer {
        public BusinessFunction businessFunction = BusinessFunction.RENT;
        public UnitPriceSpecification priceSpecification = new UnitPriceSpecification();
        private DeliveryMethod availableDeliveryMethod = DeliveryMethod.DOWNLOAD;
        public QuantitativeValue eligibleDuration = new QuantitativeValue();

        public DeliveryMethod getAvailableDeliveryMethod() {
            return availableDeliveryMethod;
        }

        public void setAvailableDeliveryMethod(DeliveryMethod availableDeliveryMethod) {
            this.availableDeliveryMethod = availableDeliveryMethod;
        }
    }

    enum DeliveryMethod {
        @Expose("gr:DeliveryModeDirectDownload")
        DOWNLOAD
    }

    enum BusinessFunction {
        @Expose("gr:LeaseOut")
        RENT,
        @Expose("gr:Sell")
        FOR_SALE,
        @Expose("gr:Buy")
        BUY
    }

    @Term(define = "gr", as="http://purl.org/goodrelations/v1#")
    class UnitPriceSpecification {
        public BigDecimal price = BigDecimal.valueOf(3.99);
        public String priceCurrency = "USD";
        public String datetime = "2012-12-31T23:59:59Z";
    }

    class QuantitativeValue {
        public String value = "30";
        public String unitCode = "DAY";
    }

    @Test
    public void testNestedSchemaOrgClassesWithGoodrelationsExtensions() throws IOException, JsonLdError {
        final Movie movie = new Movie();
        mapper.writeValue(w, movie);
        final String json = w.toString();
        System.out.println(json);
        JsonAssert.with(json)
                .assertThat("$.name", is(movie.name));
        JsonAssert.with(json)
                .assertThat("$.offers[0].availableDeliveryMethod", is(DeliveryMethod.DOWNLOAD.name()));
        final String s = JsonLdTestUtils.applyContext(json);
        System.out.println(s);

    }




    @Test
    public void testSchemaOrgOfferWithGoodrelationsExtensions() throws IOException {

        mapper.writeValue(w, new Offer());
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"," +
                "\"gr\":\"http://purl.org/goodrelations/v1#\"," +
                "\"businessFunction\":{\"@type\":\"@vocab\"}," +
                "\"RENT\":\"gr:LeaseOut\"," +
                "\"availableDeliveryMethod\":{\"@type\":\"@vocab\"}," +
                "\"DOWNLOAD\":\"gr:DeliveryModeDirectDownload\"}," +
                "\"@type\":\"Offer\"," +
                "\"businessFunction\":\"RENT\"," +
                "\"priceSpecification\":{" +
                "\"@type\":\"UnitPriceSpecification\"," +
                "\"price\":3.99," +
                "\"priceCurrency\":\"USD\"," +
                "\"datetime\":\"2012-12-31T23:59:59Z\"}," +
                "\"availableDeliveryMethod\":\"DOWNLOAD\"," +
                "\"eligibleDuration\":{" +
                "\"@type\":\"QuantitativeValue\"," +
                "\"value\":\"30\"," +
                "\"unitCode\":\"DAY\"}}", w.toString());
    }

    @Test
    public void testSchemaOrgOfferWithNullEnum() throws IOException {

        final Offer offer = new Offer();
        offer.setAvailableDeliveryMethod(null);
        mapper.writeValue(w, offer);
        assertEquals("{\"@context\":{" +
                "\"@vocab\":\"http://schema.org/\"," +
                "\"gr\":\"http://purl.org/goodrelations/v1#\"," +
                "\"businessFunction\":{\"@type\":\"@vocab\"}," +
                "\"RENT\":\"gr:LeaseOut\"}," +
                "\"@type\":\"Offer\"," +
                "\"businessFunction\":\"RENT\"," +
                "\"priceSpecification\":{" +
                "\"@type\":\"UnitPriceSpecification\"," +
                "\"price\":3.99," +
                "\"priceCurrency\":\"USD\"," +
                "\"datetime\":\"2012-12-31T23:59:59Z\"}," +
                "\"eligibleDuration\":{" +
                "\"@type\":\"QuantitativeValue\"," +
                "\"value\":\"30\"," +
                "\"unitCode\":\"DAY\"}}", w.toString());
    }


    @Test
    public void testDoesNotRepeatContextIfUnnecessary() throws IOException {
        mapper.writeValue(w, new Offer());
        assertEquals("{\"@context\":" +
                "{\"@vocab\":\"http://schema.org/\"," +
                "\"gr\":\"http://purl.org/goodrelations/v1#\"," +
                "\"businessFunction\":{\"@type\":\"@vocab\"}," +
                "\"RENT\":\"gr:LeaseOut\"," +
                "\"availableDeliveryMethod\":{\"@type\":\"@vocab\"}," +
                "\"DOWNLOAD\":\"gr:DeliveryModeDirectDownload\"}," +
                "\"@type\":\"Offer\"," +
                "\"businessFunction\":\"RENT\"," +
                "\"priceSpecification\":{\"@type\":\"UnitPriceSpecification\"," +
                "\"price\":3.99," +
                "\"priceCurrency\":\"USD\"," +
                "\"datetime\":\"2012-12-31T23:59:59Z\"}," +
                "\"availableDeliveryMethod\":\"DOWNLOAD\"," +
                "\"eligibleDuration\":{\"@type\":\"QuantitativeValue\",\"value\":\"30\",\"unitCode\":\"DAY\"}}",
                w.toString());
    }


}