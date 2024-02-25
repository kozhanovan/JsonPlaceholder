package com.typicode.jsonplaceholder.storage;

import com.typicode.jsonplaceholder.model.Comment;
import com.typicode.jsonplaceholder.model.Post;

import java.util.List;

public interface Storage {
    void addPost(Post post);

    void addComment(Comment comment);

    List<Post> getPosts();

    List<Comment> getComments();

    Post getPost(int postId);

    List<Comment> getCommentsForPost(int postId);

    default void truncate() {
        // does nothing by default
    }

    default boolean isEmpty() {
        return false;
    }
}
