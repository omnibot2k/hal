package com.example.hal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

//more info on pojos
//https://stackoverflow.com/questions/25858698/spring-hateoas-embedded-resource-support

//doesn't work
/*
@RunWith(SpringRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@Slf4j
*/
//full app works required spring hateoas support
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class HalTest {

    /*@Configuration
    @SpringBootApplication
    @EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
    static class ContextConfiguration {
    }*/

    // new version https://github.com/spring-projects/spring-hateoas/blob/master/src/test/java/org/springframework/hateoas/mediatype/hal/Jackson2HalIntegrationTest.java
    static final String LIST_EMBEDDED_RESOURCE_REFERENCE = "{\"_embedded\":{\"content\":[{\"text\":\"test1\",\"number\":1,\"_links\":{\"self\":{\"href\":\"localhost\"}}},{\"text\":\"test2\",\"number\":2,\"_links\":{\"self\":{\"href\":\"localhost\"}}}]},\"_links\":{\"self\":{\"href\":\"localhost\"}}}";

    @Autowired
    RestTemplateBuilder restTemplateBuilder;

    //JSON mapper wont do us any good!
    //@Autowired
    //ObjectMapper mapper;

    @Test
    public void test() throws Exception {

        /* DOES NOT WORK!!!*/
        //https://github.com/spring-projects/spring-hateoas/blob/0.25.0.RELEASE/src/main/java/org/springframework/hateoas/config/ConverterRegisteringWebMvcConfigurer.java#L99
        //configuring own mapper and serializing/deserializing is difficult, client must enable hypermediasupport and pass in that resttemplatebuilder
        // add runtime checking
        /*ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        mapper.registerModule(new Jackson2HalModule());

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
        converter.setObjectMapper(mapper);
        RestTemplate template = new RestTemplateBuilder().build();*/

        RestTemplate template = restTemplateBuilder.build();

        getHalConverter(template);

        // Create a MockWebServer. These are lean enough that you can create a new
        // instance for every unit test.
        MockWebServer server = new MockWebServer();
        // Start the server.
        server.start(8888);

        //verify that can deserialize
        deserializesMultipleResourceResourcesAsEmbedded(template);

        String json = new String(Files.readAllBytes(Paths.get("./src/test/resources/hal.json")), "UTF-8");

        // Schedule some responses.
        MockResponse response = new MockResponse()
                .addHeader("Content-Type", "application/hal+json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(json);
        server.enqueue(response);

        // Ask the server for its URL. You'll need this to make HTTP requests.
        String rootUrl = server.url("/v1/chat/").toString();

        //set accept to hal+json
        /*final ResponseEntity<SearchResults> studentResponse = template
                .exchange(rootUrl, HttpMethod.GET, null,
                        new ParameterizedTypeReference<SearchResults>() {
                        });

        String out = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(studentResponse.getBody());*/

        //test traverson
        /*MockResponse response2 = new MockResponse()
                .addHeader("Content-Type", "application/hal+json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(json);
        server.enqueue(response2);

        Traverson traverson = new Traverson(new URI(server.url("").toString()), MediaTypes.HAL_JSON_UTF8);
        traverson.setRestOperations(template);
        ResponseEntity<SearchResults> searchResults = traverson.follow("self").toEntity(SearchResults.class);
        String out = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(searchResults.getBody());
        log.info("out={}", out);*/

        server.shutdown();
    }

    private void verifyHalConverterAvailable(RestTemplate restTemplate) {
        if (getHalConverter(restTemplate) == null) {
            String error = String.format("Required media type [%s] is NOT supported with supplied RestTemplate converters.", MediaTypes.HAL_JSON);
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("Required media type [%s] is supported with supplied RestTemplate converters.", MediaTypes.HAL_JSON);
    }

    private MappingJackson2HttpMessageConverter getHalConverter(RestTemplate template) {
        return (MappingJackson2HttpMessageConverter) template.getMessageConverters()
                .stream()
                .filter(httpMessageConverter ->
                        httpMessageConverter.getSupportedMediaTypes()
                                .stream()
                                .anyMatch(mediaType -> mediaType == MediaTypes.HAL_JSON))
                .findFirst().get();
    }

    void deserializesMultipleResourceResourcesAsEmbedded(RestTemplate template) throws IOException {

        SearchResults expected = setupResources();
        expected.add(new Link("localhost"));

        String json = new String(Files.readAllBytes(Paths.get("./src/test/resources/hal.json")), "UTF-8");

        //WILL NOT WORK WITH DEFAULT JACKSON MAPPER
        ObjectMapper objectMapper = getHalConverter(template).getObjectMapper();

        SearchResults result = objectMapper.readValue(json,
                SearchResults.class);

        log.info("parsed response({})", objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(result));
        //assertThat(result).isEqualTo(expected);
    }

    private Dispatcher stubDispatcher() {
        return new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                switch (request.getPath()) {
                    case "/v1/login/auth/":
                        return new MockResponse().setResponseCode(200);
                    case "v1/check/version/":
                        return new MockResponse().setResponseCode(200).setBody("version=9");
                    case "/v1/profile/info":
                        return new MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private static SearchResults setupResources() {

        Collection<Author> content = Arrays.asList(new Author("first", "last"));
        PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(10, 100, 1000, 10000);
        Iterable<Link> links = new ArrayList<>();

        SearchResults searchResults = new SearchResults(content, metadata, links);
        return searchResults;
    }


}
