package com.typicode.jsonplaceholder.storage;

import com.google.gson.Gson;
import com.typicode.jsonplaceholder.model.Comment;
import com.typicode.jsonplaceholder.model.Post;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.conversions.Bson;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

public class MongoStorage implements Storage {

    private static final String POSTS = "posts";

    private static final String COMMENTS = "comments";

    private final Logger logger = LoggerFactory.getLogger(MongoStorage.class);

    private final MongoTemplate mongo;

    public MongoStorage(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void addPost(Post post) {
        logger.trace(() -> "Add post: " + post);
        mongo.insert(post, POSTS);
    }

    @Override
    public void addComment(Comment comment) {
        logger.trace(() -> "Add comment: " + comment);
        mongo.insert(comment, COMMENTS);
    }

    @Override
    public List<Post> getPosts() {
        return collectionAsList(POSTS, Post.class, new BsonDocument());
    }

    @Override
    public List<Comment> getComments() {
        return collectionAsList(COMMENTS, Comment.class, new BsonDocument());
    }

    @Override
    public Post getPost(int postId) {
        return collectionAsList(POSTS , Post.class, new BsonDocument("_id", new BsonInt32(postId)))
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Comment> getCommentsForPost(int postId) {
        return collectionAsList(COMMENTS, Comment.class, new BsonDocument("postId", new BsonInt32(postId)));
    }

    private <T> List<T> collectionAsList(String name, Class<T> type, Bson filter) {
        logger.trace(() -> "Query full content of " + name + " collection");
        List<T> result = new ArrayList<>();
        Gson gson = new Gson();
        mongo.getCollection(name).find(filter).forEach(d -> {
            T item = gson.fromJson(d.toJson().replace("_id", "id"), type);
            result.add(item);
        });
        return result;
    }

    @Override
    public void truncate() {
        logger.info(() -> "Remove all existing collections from MongoDB");
        mongo.getCollectionNames().forEach(mongo::dropCollection);
    }

    @Override
    public boolean isEmpty() {
        return mongo.collectionExists(POSTS) && mongo.collectionExists(COMMENTS);
    }
}
