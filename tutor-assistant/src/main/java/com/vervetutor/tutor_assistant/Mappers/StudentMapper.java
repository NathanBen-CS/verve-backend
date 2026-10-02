package com.vervetutor.tutor_assistant.Mappers;
import com.vervetutor.tutor_assistant.Student.Student;
import com.vervetutor.tutor_assistant.Student.StudentDto;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper implements Mapper<Student, StudentDto>
{
    private ModelMapper modelMapper;

    StudentMapper(ModelMapper modelMapper)
    {
        this.modelMapper = modelMapper;
    }

    @Override
    public StudentDto mapTo(Student student) {
        return modelMapper.map(student, StudentDto.class);
    }

    @Override
    public Student mapFrom(StudentDto studentDto) {
        return modelMapper.map(studentDto, Student.class);
    }
}
