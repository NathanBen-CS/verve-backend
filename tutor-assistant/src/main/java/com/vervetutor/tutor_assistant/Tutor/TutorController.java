package com.vervetutor.tutor_assistant.Tutor;

import com.vervetutor.tutor_assistant.Mappers.Mapper;
import com.vervetutor.tutor_assistant.Mappers.TutorMapper;
import com.vervetutor.tutor_assistant.Student.Student;
import com.vervetutor.tutor_assistant.Student.StudentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class TutorController
{
    private TutorService tutorService;
    private Mapper<Tutor, TutorDto> tutorMapper;

    @Autowired
    public TutorController(TutorService tutorService, TutorMapper tutorMapper){
        this.tutorService = tutorService;
        this.tutorMapper = tutorMapper;
    }

    @PostMapping(path = "/tutors")
    //Response entity changes the response code
    public ResponseEntity<TutorDto> createTutor(@RequestBody TutorDto tutor)
    {
        Tutor tutorEnt = tutorMapper.mapFrom(tutor);
        Tutor savedTutor = tutorService.createTutor(tutorEnt);
        return new ResponseEntity<>(tutorMapper.mapTo(savedTutor), HttpStatus.CREATED);
    }

    @GetMapping("/tutors")
    public List<TutorDto> findAllTutors()
    {
        List<Tutor> tutors = tutorService.findAllTutors();
        return tutors.stream().map(tutorMapper::mapTo).collect(Collectors.toList());
    }

    @GetMapping("/tutors/{id}")
    public ResponseEntity<TutorDto> getTutor(@PathVariable("id") Long id)
    {
        Optional<Tutor> tutorSaved = tutorService.findTutor(id);
        return tutorSaved.map(tutor -> {
            TutorDto tutorDto = tutorMapper.mapTo(tutor);
            return new ResponseEntity<>(tutorMapper.mapTo(tutor), HttpStatus.OK);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}