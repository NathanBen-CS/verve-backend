package com.vervetutor.tutor_assistant.Mappers;

import com.vervetutor.tutor_assistant.Tutor.Tutor;
import com.vervetutor.tutor_assistant.Tutor.TutorDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TutorMapper implements Mapper<Tutor, TutorDto>
{
    private ModelMapper modelMapper;

    TutorMapper(ModelMapper modelMapper)
    {
        this.modelMapper = modelMapper;
    }

    @Override
    public TutorDto mapTo(Tutor tutor) {
        return modelMapper.map(tutor, TutorDto.class);
    }

    @Override
    public Tutor mapFrom(TutorDto tutorDto) {
        return modelMapper.map(tutorDto, Tutor.class);
    }
}
