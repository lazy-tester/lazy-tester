# lazy-tester - Simple tool to generate unit tests skeleton

## Example of test generation:

### Source testee class
```java
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
```

### Generated test
```java
package com.github.lazy.tester.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BestEverServiceTest {

    @Mock
    IdValidator idValidator;

    @Mock
    ContactService contactService;

    @InjectMocks
    BestEverService bestEverService;

    @Test
    void shouldJustDoIt() throws Exception {
        // then
        bestEverService.justDoIt();
    }

    @Test
    void shouldGetBestNumber() throws Exception {
        // given
        String stringParameter = new String();
        int intParameter = -1188957731;
        // when
        Mockito.mock(contactService.getEmail()).thenReturn("some value to return");
        Mockito.mock(idValidator.beautify()).thenReturn("some value to return");
        // then
        Integer result = bestEverService.getBestNumber(stringParameter, intParameter);
        // assert
        Assertions.assertEquals(expectedResult, result);
        // verify
        Mockito.verify(idValidator).validate();
        Mockito.verify(idValidator).validateTwo();
    }
}
```