package back.service.user;

import back.domain.Users;
import back.dto.user.UserResponse;
import back.dto.user.UserUpdateRequest;
import back.exception.AuthException;
import back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 정보 조회
     */
    public UserResponse getUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(AuthException.UserNotFound::new);
        return UserResponse.from(user);
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(AuthException.UserNotFound::new);
        
        user.updateProfile(request.getRealName());
        
        return UserResponse.from(user);
    }
}
