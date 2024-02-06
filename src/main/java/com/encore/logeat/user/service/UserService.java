package com.encore.logeat.user.service;

import com.encore.logeat.common.dto.ResponseDto;
import com.encore.logeat.common.jwt.JwtTokenProvider;
import com.encore.logeat.common.jwt.refresh.UserRefreshToken;
import com.encore.logeat.common.jwt.refresh.UserRefreshTokenRepository;
import com.encore.logeat.user.domain.User;
import com.encore.logeat.user.dto.request.UserCreateRequestDto;
import com.encore.logeat.user.dto.request.UserLoginRequestDto;
import com.encore.logeat.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRefreshTokenRepository userRefreshTokenRepository;

	@Autowired
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
		JwtTokenProvider jwtTokenProvider, UserRefreshTokenRepository userRefreshTokenRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.userRefreshTokenRepository = userRefreshTokenRepository;
	}

	@Transactional
	public User createUser(UserCreateRequestDto userCreateRequestDto) {
		userCreateRequestDto.setPassword(
			passwordEncoder.encode(userCreateRequestDto.getPassword()));
		User user = userCreateRequestDto.toEntity();
		// 유저 프로필 이미지 추가해주는 로직 작성 필요
		return userRepository.save(user);
	}

	@Transactional
	public ResponseDto userLogin(UserLoginRequestDto userLoginRequestDto) {
		User user = userRepository.findByEmail(userLoginRequestDto.getEmail())
			.filter(
				it -> passwordEncoder.matches(userLoginRequestDto.getPassword(), it.getPassword()))
			.orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));
		String accessToken = jwtTokenProvider.createAccessToken(
			String.format("%s:%s", user.getId(), user.getRole()));
		String refreshToken = jwtTokenProvider.createRefreshToken();
		userRefreshTokenRepository.findById(user.getId())
			.ifPresentOrElse(
				it -> it.updateUserRefreshToken(refreshToken),
				() -> userRefreshTokenRepository.save(new UserRefreshToken(user, refreshToken))
			);
		Map<String, String> result = new HashMap<>();
		result.put("access_token", accessToken);
		result.put("refresh_token", refreshToken);
		return new ResponseDto(HttpStatus.OK, "JWT token is created!", result);
	}
}