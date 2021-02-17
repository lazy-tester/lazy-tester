package com.company.demo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BestEverService {

    private final IdGenerator idGenerator;
    private final ContactService contactService;

    public Integer getBestNumber() {
        var someId = idGenerator.generate();
        return generateHashNumber(someId);
    }

    private int generateHashNumber(String someId) {
        return (someId + "_" + contactService.getEmail()).hashCode();
    }

    public void justDoIt() {
        System.out.println("Just did it.");
    }

}
