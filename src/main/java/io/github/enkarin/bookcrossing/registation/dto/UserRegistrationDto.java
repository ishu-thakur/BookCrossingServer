package io.github.enkarin.bookcrossing.registation.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Value
@SuperBuilder
@Validated
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Сущность пользователя")
public class UserRegistrationDto {

    @Schema(description = "Имя", example = "Alex")
    @NotBlank(message = "name: Имя должно содержать хотя бы один видимый символ")
    String name;

    @Schema(description = "Логин", example = "LogAll")
    @NotBlank(message = "login: Логин должен содержать хотя бы один видимый символ")
    String login;

    @Schema(description = "Пароль", example = "123456")
    @NotBlank(message = "password: Пароль должен содержать хотя бы один видимый символ")
    @Size(min = 6, message = "Пароль должен содержать больше 6 символов")
    String password;

    @Schema(description = "Подвержение пароля", example = "123456", required = true)
    String passwordConfirm;

    @Schema(description = "Почта", example = "al@yandex.ru")
    @Email(message = "email: Некорректный почтовый адрес")
    String email;

    @Schema(description = "Город", example = "Новосибирск")
    String city;

    @JsonCreator
    public static UserRegistrationDto create(final String name, final String login, final String password,
                                             final String passwordConfirm, final String email, final String city) {
        return new UserRegistrationDto(name, login, password, passwordConfirm, email, city);
    }
}
