package app.websocket;
import java.lang.management.*;

public class JvmMetricsCollector {
    public static String collectMetrics() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();

        return String.format("{ \"threads\": %d, \"heapUsed\": %d, \"heapMax\": %d }",
                threadCount, heapUsed, heapMax);
    }
}
