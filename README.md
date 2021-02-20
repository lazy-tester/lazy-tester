# lazy-tester - Simple tool to generate unit tests skeleton

Example of test generation:

```
package com.github.lazy.tester.demo;

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
    void shouldJustDoIt()
        throws Exception
    {
        //then
        bestEverService.justDoIt();
    }

    @Test
    void shouldGetBestNumber()
        throws Exception
    {
        //when
        Mockito.mock(contactService.getEmail()).thenReturn("some value to return");
        Mockito.mock(idValidator.beautify()).thenReturn("some value to return");
        //then
        bestEverService.getBestNumber();
    }

}
```