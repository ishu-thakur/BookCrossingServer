package io.github.enkarin.bookcrossing.books.controllers;

import io.github.enkarin.bookcrossing.base.BookCrossingBaseTests;
import io.github.enkarin.bookcrossing.books.dto.BookDto;
import io.github.enkarin.bookcrossing.books.dto.BookModelDto;
import io.github.enkarin.bookcrossing.books.service.BookService;
import io.github.enkarin.bookcrossing.errors.ErrorListResponse;
import io.github.enkarin.bookcrossing.support.TestDataProvider;
import io.github.enkarin.bookcrossing.user.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MyBookControllerTest extends BookCrossingBaseTests {

    @Autowired
    private BookService bookService;

    @Test
    void saveBookTest() {
        final int user = createAndSaveUser(TestDataProvider.buildBot()).getUserId();
        enabledUser(user);
        final BookModelDto bookDto = checkPost(
                userService.findByLoginAndPassword(TestDataProvider.buildAuthBot()).getAccessToken(),
                TestDataProvider.buildDorian(), 201)
                .expectBody(BookModelDto.class)
                .returnResult().getResponseBody();
        assertThat(bookDto)
                .isNotNull()
                .isEqualTo(TestDataProvider.buildDorian(bookDto.getBookId()));
    }

    @Test
    void saveBadBookTest() {
        final int user = createAndSaveUser(TestDataProvider.buildBot()).getUserId();
        enabledUser(user);
        final var response = checkPost(
                userService.findByLoginAndPassword(TestDataProvider.buildAuthBot()).getAccessToken(),
                TestDataProvider.prepareBook().author("").build(), 400)
                .expectBody(ErrorListResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.getErrors())
                .containsExactlyInAnyOrder("title: Название должно содержать хотя бы один видимый символ",
                        "author: Поле \"автор\" должно содержать хотя бы один видимый символ");
    }

    @Test
    void bookListTest() {
        final List<UserDto> users = TestDataProvider.buildUsers().stream()
                .map(this::createAndSaveUser)
                .collect(Collectors.toList());
        enabledUser(users.get(0).getUserId());
        enabledUser(users.get(1).getUserId());

        final List<BookDto> books = TestDataProvider.buildBooks();
        bookService.saveBook(books.get(0), users.get(1).getLogin());
        final int book1 = bookService.saveBook(books.get(1), users.get(0).getLogin()).getBookId();
        final int book2 = bookService.saveBook(books.get(2), users.get(0).getLogin()).getBookId();

        final var response = checkGet(userService.findByLoginAndPassword(TestDataProvider.buildAuthBot())
                .getAccessToken())
                .expectBodyList(BookModelDto.class).returnResult().getResponseBody();
        assertThat(response)
                .hasSize(2)
                .containsExactlyInAnyOrder(TestDataProvider.buildDandelion(book1),
                        TestDataProvider.buildWolves(book2));
    }

    @Test
    void bookEmptyListTest() {
        final List<UserDto> users = TestDataProvider.buildUsers().stream()
                .map(this::createAndSaveUser)
                .collect(Collectors.toList());
        enabledUser(users.get(0).getUserId());
        enabledUser(users.get(1).getUserId());
        bookService.saveBook(TestDataProvider.buildDandelion(), users.get(1).getLogin());

        checkGet(userService.findByLoginAndPassword(TestDataProvider.buildAuthBot()).getAccessToken())
                .expectBodyList(BookModelDto.class).hasSize(0);
    }

    @Test
    void deleteBookTest() {
        final UserDto user = createAndSaveUser(TestDataProvider.buildBot());
        enabledUser(user.getUserId());
        final List<BookModelDto> book = TestDataProvider.buildBooks().stream()
                .map(b -> bookService.saveBook(b, user.getLogin()))
                .collect(Collectors.toList());
        checkDelete(userService.findByLoginAndPassword(TestDataProvider.buildAuthBot()).getAccessToken(),
                book.get(0).getBookId(), 200);
        assertThat(bookService.findBookForOwner(user.getLogin())).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("select exists(select * from t_book where book_id=?)", Boolean.class,
                book.get(0).getBookId())).isFalse();
    }

    @Test
    void deleteBookNotFoundExceptionTest() {
        final UserDto user = createAndSaveUser(TestDataProvider.buildBot());
        enabledUser(user.getUserId());
        checkDelete(userService.findByLoginAndPassword(TestDataProvider.buildAuthBot()).getAccessToken(),
                Integer.MAX_VALUE, 404)
                .expectBody()
                .jsonPath("$.book")
                .isEqualTo("Книга не найдена");
    }

    private WebTestClient.ResponseSpec checkPost(final String access, final Object body, final int status) {
        return webClient.post()
                .uri("/user/myBook")
                .headers(headers -> headers.setBearerAuth(access))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(status);
    }

    private WebTestClient.ResponseSpec checkGet(final String access) {
        return webClient.get()
                .uri("/user/myBook")
                .headers(headers -> headers.setBearerAuth(access))
                .exchange()
                .expectStatus().isEqualTo(200);
    }

    private WebTestClient.ResponseSpec checkDelete(final String access, final int bookId, final int status) {
        return webClient.delete()
                .uri("/user/myBook?bookId={bookId}", bookId)
                .headers(headers -> headers.setBearerAuth(access))
                .exchange()
                .expectStatus().isEqualTo(status);
    }

    private void enabledUser(final int userId) {
        jdbcTemplate.update("update t_user set enabled = 1 where user_id = " + userId);
    }
}
