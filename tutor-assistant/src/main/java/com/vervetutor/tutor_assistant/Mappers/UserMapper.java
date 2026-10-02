package com.vervetutor.tutor_assistant.Mappers;

import com.vervetutor.tutor_assistant.User.User;
import com.vervetutor.tutor_assistant.User.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements Mapper<User, UserDto> {

    private final ModelMapper modelMapper;

    public UserMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDto mapTo(User user) {
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public User mapFrom(UserDto userDto) {
        return modelMapper.map(userDto, User.class);
    }
}
