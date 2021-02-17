package com.company.demo;

import com.company.demo.ContactService;
import com.company.demo.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BestEverServiceTest {

    @Mock
    IdGenerator idGenerator;
    @Mock
    ContactService contactService;
    @InjectMocks
    double bestEverService;

    @Test
    void shouldGetBestNumber() {
    }

    @Test
    void shouldJustDoIt() {
    }

}