package gemini_lite;

import gemini_lite.Engine.ClientEngine;
import gemini_lite.protocol.ProxyRequestHandler;

public class Proxy {
	public static void main(String[] args) throws Exception {
		final int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1959; // default proxy port
		final ClientEngine engine = new ClientEngine();
		
		new Server(port, new ProxyRequestHandler(engine)).run();
	}
}
