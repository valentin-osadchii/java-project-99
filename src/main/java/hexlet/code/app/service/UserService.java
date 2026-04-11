package hexlet.code.app.service;

import hexlet.code.app.dto.UserCreateDTO;
import hexlet.code.app.dto.UserDTO;
import hexlet.code.app.dto.UserUpdateDTO;

import java.util.List;

public interface UserService {

    List<UserDTO> getAll();

    UserDTO get(long id);

    UserDTO create(UserCreateDTO userData);

    UserDTO update(long id, UserUpdateDTO userData);

    void delete(long id);

    boolean isOwner(Long userId);
}
