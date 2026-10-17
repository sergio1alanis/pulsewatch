package dev.sergio.pulsewatch.monitor.check;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Blocks private and reserved IP destinations before an outbound probe. */
@Component
public class PublicTargetAddressPolicy {

    private final boolean allowLoopback;

    public PublicTargetAddressPolicy(
            @Value("${pulsewatch.probe.allow-loopback:false}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    public void validate(URI target) {
        resolvePublicAddresses(target);
    }

    public List<InetAddress> resolvePublicAddresses(URI target) {
        String host = target.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("El endpoint debe tener un host válido");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("El host no tiene direcciones resolubles");
            }
            for (InetAddress address : addresses) {
                if (isBlocked(address)) {
                    throw new IllegalArgumentException(
                            "El destino resuelve a una dirección no pública");
                }
            }
            return List.of(addresses);
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("No se pudo resolver el host del endpoint");
        }
    }

    private boolean isBlocked(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return !allowLoopback;
        }
        if (address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return isBlockedIpv4(bytes);
        }
        if (address instanceof Inet6Address) {
            return isBlockedIpv6(bytes);
        }
        return true;
    }

    private boolean isBlockedIpv4(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);

        return first == 0
                || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 192 && second == 0)
                || (first == 192 && second == 88 && third == 99)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113);
    }

    private boolean isBlockedIpv6(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        int fourth = Byte.toUnsignedInt(bytes[3]);

        boolean globalUnicastRange = (first & 0xE0) == 0x20;
        boolean specialPurpose2001Range = first == 0x20 && second == 0x01
                && (third & 0xFE) == 0;
        boolean documentationRange = first == 0x20 && second == 0x01
                && third == 0x0D && fourth == 0xB8;
        boolean sixToFourRange = first == 0x20 && second == 0x02;
        boolean documentation3fffRange = first == 0x3F && second == 0xFF
                && (third & 0xF0) == 0;
        return !globalUnicastRange
                || specialPurpose2001Range
                || documentationRange
                || sixToFourRange
                || documentation3fffRange;
    }
}
