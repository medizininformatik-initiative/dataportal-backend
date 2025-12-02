package de.fdpg.dataportal_backend.query.broker.dsf;

import de.fdpg.dataportal_backend.query.broker.SiteNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class DSFBrokerClientTest {

    @InjectMocks
    private DSFBrokerClient client;

    @ParameterizedTest
    @ValueSource(strings = {"my-site", "something", "identity", "Site 1"})
    public void testGetSiteName_IsIdentity(String siteId) throws SiteNotFoundException {
        assertEquals(siteId, client.getSiteName(siteId));
    }

    @Test
    public void testGetSiteName_NullRaisesError() {
        assertThrows(SiteNotFoundException.class, () -> client.getSiteName(null));
    }
}
