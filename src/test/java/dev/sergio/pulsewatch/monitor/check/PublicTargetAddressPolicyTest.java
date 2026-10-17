package dev.sergio.pulsewatch.monitor.check;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class PublicTargetAddressPolicyTest {

    private final PublicTargetAddressPolicy policy = new PublicTargetAddressPolicy(false);

    @Test
    void rejectsLoopbackAddressesByDefault() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(URI.create("http://127.0.0.1/health")));
    }

    @Test
    void rejectsPrivateNetworkAddresses() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(URI.create("http://192.168.1.10/health")));
    }

    @Test
    void rejectsIpv6LoopbackAddresses() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(URI.create("http://[::1]/health")));
    }

    @Test
    void rejectsIpv6SpecialPurposeAndDocumentationRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(URI.create("http://[2001:0::1]/health")));
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(URI.create("http://[3fff::1]/health")));
    }

    @Test
    void rejectsIpv4DocumentationAddresses() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(URI.create("http://192.0.2.10/health")));
    }

    @Test
    void allowsPublicIpv4Addresses() {
        assertDoesNotThrow(
                () -> policy.validate(URI.create("https://8.8.8.8/health")));
    }

    @Test
    void allowsLoopbackOnlyWhenExplicitlyConfiguredForTests() {
        PublicTargetAddressPolicy testPolicy = new PublicTargetAddressPolicy(true);

        assertDoesNotThrow(
                () -> testPolicy.validate(URI.create("http://127.0.0.1/health")));
        assertThrows(IllegalArgumentException.class,
                () -> testPolicy.validate(URI.create("http://192.168.1.10/health")));
    }
}
