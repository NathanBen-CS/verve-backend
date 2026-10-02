package com.vervetutor.tutor_assistant.Mappers;

import com.vervetutor.tutor_assistant.Lesson.Lesson;
import com.vervetutor.tutor_assistant.Lesson.LessonDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper implements Mapper<Lesson, LessonDto> {
    private final ModelMapper modelMapper;

    LessonMapper(ModelMapper modelMapper)
    {
        this.modelMapper = modelMapper;
    }

    @Override
    public LessonDto mapTo(Lesson lesson) {
        return modelMapper.map(lesson, LessonDto.class);
    }

    @Override
    public Lesson mapFrom(LessonDto studentDto) {
        return modelMapper.map(studentDto, Lesson.class);
    }
}