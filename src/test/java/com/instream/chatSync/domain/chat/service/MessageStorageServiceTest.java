package com.instream.chatSync.domain.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

@WebFluxTest(MessageStorageService.class)
public class MessageStorageServiceTest {
    @Autowired
    private MessageStorageService messageStorageService;

    @MockBean
    private BillingService billingService;

    private UUID sessionId = UUID.randomUUID();

    @Test
    @DisplayName("addMessage 메서드는 각 세션 별로 최초 1회 호출 때만 ConcurrentLinkedQueue를 생성한다.")
    public void createMessageQueueOnlyOnceWhenCallAddMessage() {}

    @Timeout(10)
    @RepeatedTest(value = 500, name = "Thread {currentRepetition}/{totalRepetitions}")
    @Execution(ExecutionMode.CONCURRENT)
    @DisplayName("addMessage 메서드는 Multi-Threading 상황에서도 메세지를 안정적으로 저장한다.")
    public void saveMessageWhenMultiThreading() {}

    @Test
    @DisplayName("addPublishFlux 메서드는 n ms마다 메세지를 전파하는 로직을 각 세션마다 1회 구독한다.")
    public void subscribeOnlyOnceWhenCallAddPublishFlux() {}

    @Test
    @DisplayName("addPublishFlux 메서드는 n ms마다 메세지를 전파하는 로직을 실행할 때 Billing API를 호출한다.")
    public void callBillingAPIWhenPublishMessage() {}

    @Test
    @DisplayName("addPublishFlux 메서드는 n ms마다 메세지를 전파하는 로직을 실행할 때, 2개의 SSE 소켓에 똑같은 메세지를 전파한다.")
    public void publishSameMessageToSocketsWhenPublishMessage() {}

    @Test
    @DisplayName("addPublishFlux 메서드는 n ms마다 메세지를 전파하는 로직을 실행하고 나서 다음 전파 시간 전까지, 메세지 큐에는 이전 전파 메세지는 없고 다음 전파 메세지만이 남아있어야 한다.")
    public void existsOnlyNextPublishMessageInMessageQueue() {}

    @Timeout(10)
    @RepeatedTest(value = 500, name = "Thread {currentRepetition}/{totalRepetitions}")
    @Execution(ExecutionMode.CONCURRENT)
    @DisplayName("closePublishFlux 메서드는 Multi-Threading 상황에서도 구독 해제가 정상적으로 된다.")
    public void disposeFluxWhenCallClosePublishFlux() {}

    @Test
    @DisplayName("streamMessages 메서드는 각 세션 별로 최초 1회 호출때만 SSE 소켓 저장 리스트를 생성한다.")
    public void createSSEStoreListOnlyOnceWhenCallStreamMessages() {}
}
