package com.instream.chatSync.domain.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@WebFluxTest(MessageStorageServiceTest.class)
public class MessageStorageServiceTest {
    @Autowired
    private MessageStorageService messageStorageService;

    @MockBean
    private BillingService billingService;
}
