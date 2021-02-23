package com.github.lazy.tester.demo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BestEverService {

    private final IdValidator idValidator;
    private final ContactService contactService;

    public Integer getBestNumber(String stringParameter, int intParameter) {
        var email = contactService.getEmail();
        var number = generateHashNumber(email);
        validate(number + (stringParameter + intParameter).hashCode());
        return idValidator.beautify(number);
    }

    private void validate(int number) {
        idValidator.validate(number);
        validateOtherPrivate(number);
    }

    private void validateOtherPrivate(int number) {
        idValidator.validateTwo(number);
    }

    private int generateHashNumber(String email) {
        return ("someToken_" + email).hashCode();
    }

    public void justDoIt() {
        System.out.println("Just did it.");
    }

}
