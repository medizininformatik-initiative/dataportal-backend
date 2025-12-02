package de.fdpg.dataportal_backend.query.broker.aktin;

import org.aktin.broker.client2.BrokerAdmin2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AktinBrokerClientTest {

    @Mock
    private BrokerAdmin2 delegate;

    @InjectMocks
    private AktinBrokerClient client;

    @Test
    public void testGetResultFeasibility_ReturnsActualPeerResultIfParsable() throws IOException {
        when(delegate.getResultString(anyInt(), anyInt())).thenReturn("5");

        var result = assertDoesNotThrow(() -> client.getResultFeasibility("1", "1"));
        assertEquals(5, result);
    }

    @Test
    public void testGetResultFeasibility_ReturnsZeroIfPeerSendsNonNumberResult() throws IOException {
        when(delegate.getResultString(anyInt(), anyInt())).thenReturn("no integer");

        var result = assertDoesNotThrow(() -> client.getResultFeasibility("1", "1"));
        assertEquals(0, result);
    }
}
