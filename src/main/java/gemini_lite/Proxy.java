package gemini_lite;

import gemini_lite.engine.ClientEngine;
import gemini_lite.protocol.ProxyRequestHandler;
/**
 * Proxy server entry point for the Gemini Lite: : handles incoming requests
 * through a ProxyRequestHandler, manages client communication to a ClientEngine.
 */
 public class Proxy {
	/**
	 * Main method to start the Gemini Lite proxy server.
	 * @param args Command-line arguments
	 * @throws Exception If server initialization or execution fails
	 */
	public static void main(String[] args) throws Exception {
		final int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1959; // determine the port: use provided argument or default to 1959
		final ClientEngine engine = new ClientEngine();
		new Server(port, new ProxyRequestHandler(engine)).run(); // create and start the server
	}
}
