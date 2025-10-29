package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.convert.UserConvert;
import com.example.thangcachep.movie_project_be.entities.RoleEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.models.dto.UserDTO;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.components.JwtTokenUtils;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.repositories.RoleRepository;
import com.example.thangcachep.movie_project_be.models.request.StaffCreateRequest;
import com.example.thangcachep.movie_project_be.models.request.UserUpdateRequest;
import com.example.thangcachep.movie_project_be.models.responses.AllStaffResponse;
import com.example.thangcachep.movie_project_be.models.responses.AllUserResponse;
import com.example.thangcachep.movie_project_be.services.IUserService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService {
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenUtils jwtTokenUtil;

    private final AuthenticationManager authenticationManager;

    private final LocalizationUtils localizationUtils;

    private final UserConvert userConvert;


    @Override
    @Transactional
    public UserEntity createUser(UserDTO userDTO) throws Exception {
         userDTO.setRoleId( userDTO.getRoleId() == null ? 1 : userDTO.getRoleId());
        String phoneNumber = userDTO.getPhoneNumber();
        if(userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        RoleEntity role =roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataNotFoundException(
                        localizationUtils.getLocalizedMessage(MessageKeys.ROLE_DOES_NOT_EXISTS)));

        if(role.getRoleName().toUpperCase().equals(RoleEntity.ADMIN)) {
            throw new PermissionDenyException("You cannot register an admin account");
        }

        UserEntity newUser = userConvert.convertToEntity(userDTO);
        newUser.setRole(role);
        newUser.setActive(true);


        // Kiểm tra nếu có accountId, không yêu cầu password
        if (userDTO.getFacebookAccountId() == 0 && userDTO.getGoogleAccountId() == 0) {
            String password = userDTO.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            newUser.setPassword(encodedPassword);
        }
        newUser.setActive(true);
        return userRepository.save(newUser);
    }

    @Override
    public String login(
            String phoneNumber,
            String password
//            Long roleId
    ) throws Exception {

        Optional<UserEntity> optionalUser = userRepository.findByPhoneNumber(phoneNumber);

        if(optionalUser.isEmpty()) {
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.WRONG_PHONE_PASSWORD));
        }
//        return optionalUser.get();//muốn trả JWT token ?
        UserEntity existingUser = optionalUser.get();
        //check password
        if (existingUser.getFacebookAccountId() == 0
                && existingUser.getGoogleAccountId() == 0) {
            if(!passwordEncoder.matches(password, existingUser.getPassword())) {
                throw new BadCredentialsException(localizationUtils.getLocalizedMessage(MessageKeys.WRONG_PHONE_PASSWORD));
            }
        }
        Long roleId = existingUser.getRole().getRoleId();
        Optional<RoleEntity> optionalRole = roleRepository.findById(roleId);
        if(optionalRole.isEmpty() || !roleId.equals(existingUser.getRole().getRoleId())) {
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.ROLE_DOES_NOT_EXISTS));
        }
//        if(!optionalUser.get().isActive()) {
          if(!existingUser.getActive()){
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.USER_IS_LOCKED));
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                phoneNumber, password,
                existingUser.getAuthorities()
        );

        //authenticate with Java Spring security
        authenticationManager.authenticate(authenticationToken);
        return jwtTokenUtil.generateToken(existingUser);
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UserUpdateRequest userDTO) throws DataNotFoundException {
        // Find the existing user by userId
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        // Check if the phone number is being changed and if it already exists for another user
        String newPhoneNumber = userDTO.getPhoneNumber();
        if (!existingUser.getPhoneNumber().equals(newPhoneNumber) &&
                userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }

        existingUser = userConvert.updateEntityFromDto(userDTO, existingUser,userId);

        // Save the updated user
         userRepository.save(existingUser);
    }

    @Override
    public Page<AllStaffResponse> getAllStaffs(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<UserEntity> data = userRepository.findByRole_RoleId(pageable,2);
        return data.map( s -> new AllStaffResponse(
                s.getUserId(), s.getFullName(),
                s.getEmail(), s.getPhoneNumber(), s.getPassword(),
                s.getImg()
        ));
    }

    @Override
    @Transactional
    public UserEntity createStaff(StaffCreateRequest staffCreateRequest) {


        if(userRepository.existsByPhoneNumber(staffCreateRequest.getPhoneNumber().trim())) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }

        if (userRepository.existsByEmail(staffCreateRequest.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        UserEntity newStaff = userConvert.convertToEntity(staffCreateRequest);
        // Kiểm tra nếu có accountId, không yêu cầu password
        if (staffCreateRequest.getFacebookAccountId() == 0 && staffCreateRequest.getGoogleAccountId() == 0) {
            String password = staffCreateRequest.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            newStaff.setPassword(encodedPassword);
        }

        newStaff.setActive(true);
//        RoleEntity role = new RoleEntity(); khong set thi cac truong kia bi  null
        RoleEntity role = roleRepository.findByRoleId(2L);
        newStaff.setRole(role);
        return userRepository.save(newStaff);
    }



    @Override
    public UserEntity getUserDetailsFromToken(String token) throws Exception {
        if(jwtTokenUtil.isTokenExpired(token)) {
            throw new Exception("Token is expired");
        }
        String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
        Optional<UserEntity> user = userRepository.findByPhoneNumber(phoneNumber);

        if (user.isPresent()) {
            return user.get();
        } else {
            throw new Exception("User not found");
        }
    }

    @Override
    public UserDTO getUserById(Long userId) throws DataNotFoundException {
        Optional<UserEntity> optionaUser = userRepository.findByUserId(userId);
        if(optionaUser.isEmpty()){
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.FIND_USER_FAILED));
        }
        UserEntity existingUser = optionaUser.get();
        RoleEntity role = existingUser.getRole();
        UserDTO userDto = userConvert.convertToDto(existingUser);
        userDto.setRoleName(role.getRoleName().toUpperCase());
        return userDto;
    }

    @Override
    @Transactional
    public void deleteUser(long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Page<AllUserResponse> getAllUsers(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<UserEntity> data = userRepository.findAllByRole_RoleId(1L,pageable);
        return data.map(AllUserResponse::new);
    }
}








