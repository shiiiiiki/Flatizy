package org.flatizy.flatizy.entity.mapper;

import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.user.UserDto;
import org.flatizy.flatizy.entity.dto.user.UserRegistrationDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto fromEntityToUserDto(User user);

    User fromUserRegistrationDtoToEntity(UserRegistrationDto userRegistrationDto);

    UserDto fromEntityToDto(User user);

    User fromDtoToEntity(UserDto user);
}
