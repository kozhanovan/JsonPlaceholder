package com.typicode.jsonplaceholder.context;

import com.typicode.jsonplaceholder.storage.MongoStorage;
import com.typicode.jsonplaceholder.storage.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoStorageContext {

    @Autowired
    private MongoTemplate mongo;

    @Bean
    public Storage getStorage() {
        return new MongoStorage(mongo);
    }
}
