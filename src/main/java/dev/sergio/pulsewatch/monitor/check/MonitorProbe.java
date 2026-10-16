package dev.sergio.pulsewatch.monitor.check;

import java.net.URI;

public interface MonitorProbe {

    ProbeResult probe(URI endpoint);
}
