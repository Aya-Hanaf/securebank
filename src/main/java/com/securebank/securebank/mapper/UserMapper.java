package com.securebank.securebank.mapper;

import com.securebank.securebank.dto.response.UserResponse;
import com.securebank.securebank.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}
