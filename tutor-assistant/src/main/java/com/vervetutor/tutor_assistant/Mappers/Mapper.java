package com.vervetutor.tutor_assistant.Mappers;

public interface Mapper<A,B> {
    B mapTo(A a);

    A mapFrom(B b);
}
