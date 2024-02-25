package com.typicode.jsonplaceholder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.typicode.TheApp;
import com.typicode.jsonplaceholder.context.MongoStorageContext;
import com.typicode.jsonplaceholder.model.Post;
import com.typicode.jsonplaceholder.model.Comment;
import com.typicode.jsonplaceholder.storage.Storage;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Consumer;

@ContextConfiguration(classes = {TheApp.class})
@SpringBootTest(classes = {MongoStorageContext.class})
@TestPropertySource("classpath:datasource.properties")
public abstract class BaseTest {

    @Autowired
    private Storage storage;

    @Value("#{T(Boolean).parseBoolean('${datasource.reload.required}')}")
    private Boolean reloadRequired;

    @Value("${service.url}")
    private String baseUrl;

    private static boolean wasNotRun = true;

    @BeforeEach
    public void setup() {
        synchronized (BaseTest.class) {
            if (wasNotRun) {
                wasNotRun = false;
                RestAssured.requestSpecification = RestAssured.given().baseUri(baseUrl);
                if (dataReloadRequired()) {
                    storage.truncate();
                    fillStorageWithData();
                }
            }
        }
    }

    private boolean dataReloadRequired() {
        return !storage.isEmpty() || reloadRequired;
    }

    private void fillStorageWithData() {
        readAndStore("posts", Post.class, storage::addPost);
        readAndStore("comments", Comment.class, storage::addComment);
    }

    private <T> void readAndStore(String path, Class<T> dataClass, Consumer<T> action) {
        String json = RestAssured.when()
                .get(path)
                .body().asString();
        JsonElement dataArray = JsonParser.parseString(json);
        Gson gson = new Gson();
        dataArray.getAsJsonArray().forEach(e -> action.accept(gson.fromJson(e, dataClass)));
    }
}
