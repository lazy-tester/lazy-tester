package com.company.demo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BestEverService {

    private final IdGenerator idGenerator;
    private final ContactService contactService;

    public Integer getBestNumber() {
        var someId = idGenerator.generate();
        var email = contactService.getEmail();
        return generateHashNumber(someId, email);
    }

    private int generateHashNumber(String someId, String email) {
        return (someId + "_" + email).hashCode();
    }

    public void justDoIt() {
        System.out.println("Just did it.");
    }

}
