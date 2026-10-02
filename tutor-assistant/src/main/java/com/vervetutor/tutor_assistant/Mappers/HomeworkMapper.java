package com.vervetutor.tutor_assistant.Mappers;

import com.vervetutor.tutor_assistant.Homework.Homework;
import com.vervetutor.tutor_assistant.Homework.HomeworkDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class HomeworkMapper implements Mapper<Homework, HomeworkDto> {
    private final ModelMapper modelMapper;

    public HomeworkMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public HomeworkDto mapTo(Homework homework) {
        return modelMapper.map(homework, HomeworkDto.class);
    }

    @Override
    public Homework mapFrom(HomeworkDto homeworkDto) {
        return modelMapper.map(homeworkDto, Homework.class);
    }
}