package hexlet.code.app.service;

import hexlet.code.app.dto.UserDTO;
import hexlet.code.app.mapper.UserMapper;
import hexlet.code.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDTO> getAll() {
        var users = userRepository.findAll();
        var result = users.stream().map(u -> userMapper.map(u)).toList();
        return result;
    }

}
