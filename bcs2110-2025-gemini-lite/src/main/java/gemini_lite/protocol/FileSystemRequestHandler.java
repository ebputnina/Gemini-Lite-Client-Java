package gemini_lite.protocol;


public class FileSystemRequestHandler implements RequestHandler {

    @Override
    public Reply handle(Request request) throws Exception {
        Reply ok = new Reply(20, "text/plain");
        // not sure yet what this is supposed to do
        return ok;
    }

}
