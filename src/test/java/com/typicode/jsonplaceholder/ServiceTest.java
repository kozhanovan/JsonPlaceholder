package com.typicode.jsonplaceholder;

import com.google.gson.Gson;
import com.typicode.jsonplaceholder.model.Comment;
import com.typicode.jsonplaceholder.model.Post;
import com.typicode.jsonplaceholder.storage.Storage;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest extends BaseTest {

    @Autowired
    private Storage storage;

    @Test
    @DisplayName("Should return all posts")
    public void shouldReturnAllPosts() {
        Post[] arr = when()
                .get("posts")
                .then()
                .statusCode(200)
                .extract()
                .as(Post[].class);
        List<Post> expected = storage.getPosts();
        List<Post> actual = Arrays.stream(arr).toList();
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Check that post with id {arguments} is obtained correctly")
    @ValueSource(ints = {1, 5, 12})
    public void shouldReturnSpecificPosts(int postId) {
        Post actual = when().get("posts/" + postId)
                .then()
                .statusCode(200)
                .extract()
                .as(Post.class);
        Post expected = storage.getPost(postId);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Should return 404 when not existent postId is specified")
    public void shouldReturn404OnNonExistentPostId() {
        int notExistentId = 333;
        when().get("posts/" + notExistentId)
                .then()
                .statusCode(404);
    }

    @ParameterizedTest(name = "Should return all comments from posts for post with id {arguments}")
    @ValueSource(ints = {3, 8, 44, 333})
    public void shouldReturnCommentsFromPostsForPost(int postId) {
        Comment[] arr = when().get("posts/" + postId + "/comments")
                .then()
                .statusCode(200)
                .extract()
                .as(Comment[].class);
        List<Comment> expected = storage.getCommentsForPost(postId);
        List<Comment> actual = Arrays.stream(arr).toList();
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Should return all comments from comments for post with id {arguments}")
    @ValueSource(ints = {6, 21, 57, 333})
    public void shouldReturnCommentsFromCommentForPost(int postId) {
        Comment[] arr = when().get("comments?postId=" + postId)
                .then()
                .statusCode(200)
                .extract()
                .as(Comment[].class);
        List<Comment> expected = storage.getCommentsForPost(postId);
        List<Comment> actual = Arrays.stream(arr).toList();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("New post could be created")
    public void newPostCouldBeCreated() throws Exception {
        URI newPost = getResourceUri("NewPost.json");
        Post expected = given()
                .contentType(ContentType.JSON)
                .body(new File(newPost))
                .when()
                .post("posts")
                .then()
                .statusCode(201)
                .extract()
                .as(Post.class);
        Post actual = readJsonFromFile(newPost);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Malformed data should not be processed")
    public void malformedDataShouldNotBeProcessed() throws URISyntaxException {
        URI newPost = getResourceUri("InvalidData.json");
        String response = given()
                .contentType(ContentType.JSON)
                .body(new File(newPost))
                .when()
                .post("posts")
                .then()
                .statusCode(500)
                .extract()
                .asString();
        assertTrue(response.contains("SyntaxError: Unexpected token : in JSON at position 39"));
    }

    @Test
    @DisplayName("Post should be created from empty source")
    public void shouldCreatePostFromEmptySource() {
        Post actual = given()
                .contentType(ContentType.JSON)
                .body("")
                .when()
                .post("posts")
                .then()
                .statusCode(201)
                .extract()
                .as(Post.class);
        Post expected = new Post();
        assertAll(
                () -> assertEquals(expected, actual),
                () -> assertEquals(101, actual.getId())
        );
    }

    @Test
    @DisplayName("Should be able to edit post")
    public void shouldBeAbleToEditPost() throws URISyntaxException {
        int postId = 3;
        URI jsonUri = getResourceUri("NewPost.json");
        Post expected = readJsonFromFile(jsonUri);
        Post actual = given()
                .contentType(ContentType.JSON)
                .body(new File(jsonUri))
                .when()
                .put("posts/" + postId)
                .then()
                .statusCode(200)
                .extract()
                .as(Post.class);
        assertAll(
                () -> assertEquals(expected, actual),
                () -> assertEquals(postId, actual.getId())
        );
    }

    @Test
    @DisplayName("Should not be able to edit not existent post")
    public void shouldNotBeAbleToEditNotExistentPost() throws URISyntaxException {
        int postId = 333;
        URI jsonUri = getResourceUri("NewPost.json");
        given()
                .contentType(ContentType.JSON)
                .body(new File(jsonUri))
                .when()
                .put("posts/" + postId)
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Should be able to patch post")
    public void shouldBeAbleToPatchPost() throws URISyntaxException {
        int postId = 41;
        URI patch = getResourceUri("Patch.json");
        Post patched = readJsonFromFile(patch);
        Post expected = apply(storage.getPost(postId), p -> {
            p.setTitle(patched.getTitle());
            return p;
        });
        Post actual = given()
                .contentType(ContentType.JSON)
                .body(new File(patch))
                .when()
                .patch("posts/" + postId)
                .then()
                .statusCode(200)
                .extract()
                .as(Post.class);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Patching not existent post does not produce any error")
    public void ableToPatchNotExistentPost() throws URISyntaxException {
        int postId = 444;
        URI patch = getResourceUri("Patch.json");
        Post expected = readJsonFromFile(patch);
        Post actual = given()
                .contentType(ContentType.JSON)
                .body(new File(patch))
                .when()
                .patch("posts/" + postId)
                .then()
                .statusCode(200)
                .extract()
                .as(Post.class);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Should return 500 when patching with broken data")
    public void shouldReturnErrorWhenPatchingWithBrokenData() throws URISyntaxException {
        int postId = 19;
        URI invalid = getResourceUri("InvalidData.json");
        given()
                .contentType(ContentType.JSON)
                .body(new File(invalid))
                .when()
                .patch("posts/" + postId)
                .then()
                .statusCode(500);
    }

    @ParameterizedTest(name = "Should return 200 on post with id {arguments} deletion")
    @ValueSource(ints = {4, 777})
    public void shouldBeAbleToDeletePost(int postId) {
        String response = when()
                .delete("posts/" + postId)
                .then()
                .statusCode(200)
                .extract()
                .asString();
        assertEquals("{}", response);
    }

    private <T> T apply(T object, Function<T, T> actions) {
        return actions.apply(object);
    }

    private URI getResourceUri(String name) throws URISyntaxException {
        return Objects.requireNonNull(getClass().getClassLoader().getResource(name)).toURI();
    }

    private Post readJsonFromFile(URI newPost) {
        try (BufferedReader br = Files.newBufferedReader(Path.of(newPost))) {
            return new Gson().fromJson(br, Post.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
