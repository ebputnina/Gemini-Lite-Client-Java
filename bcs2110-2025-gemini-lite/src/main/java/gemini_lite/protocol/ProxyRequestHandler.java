package gemini_lite.protocol;

import java.net.URI;

import gemini_lite.Engine.ClientEngine;

public class ProxyRequestHandler implements RequestHandler {

    private final ClientEngine engine;

    public ProxyRequestHandler(ClientEngine engine) {
        if (engine == null) throw new NullPointerException("engine");
        this.engine = engine;
    }

    @Override
    public Reply handle(Request request) throws Exception {
        final URI uri = request.getURI();
        return engine.sendRequest(uri);
    }
}
