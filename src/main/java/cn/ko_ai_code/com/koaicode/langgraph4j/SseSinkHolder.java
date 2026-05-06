package cn.ko_ai_code.com.koaicode.langgraph4j;

import reactor.core.publisher.FluxSink;

/**
 * ThreadLocal holder for the FluxSink created by {@code Flux.create()} in
 * {@code executeWorkflowWithFlux}.  Allows workflow nodes (e.g.
 * {@code CodeGeneratorNode}) to push content chunks directly into the
 * SSE-bound Flux without needing the sink to be threaded through method
 * signatures.
 */
public final class SseSinkHolder {

    private static final ThreadLocal<FluxSink<String>> SINK_HOLDER = new ThreadLocal<>();

    private SseSinkHolder() {}

    public static void setSink(FluxSink<String> sink) {
        SINK_HOLDER.set(sink);
    }

    public static FluxSink<String> getSink() {
        return SINK_HOLDER.get();
    }

    public static void clear() {
        SINK_HOLDER.remove();
    }
}
