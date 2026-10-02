package com.vervetutor.tutor_assistant.Registration;

import org.springframework.stereotype.Service;

import java.util.function.Predicate;

@Service
public class EmailValidator implements Predicate<String> {
    @Override
    public boolean test(String s)
    {
        //Regex to valudate email
        return true;
    }
}
