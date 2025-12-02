package gemini_lite.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves files from a given documentRoot in response to Gemini requests.
 */
public class FileSystemRequestHandler implements RequestHandler {
    private final String documentRoot;
    public FileSystemRequestHandler(String documentRoot) { // make a handler that serves files under the given document root
        this.documentRoot = documentRoot;
    }

    /**
     * Handle a request by:
     * - turning the URI path into a file under documentRoot
     * - checking validity and existence
     * - returning either the file or an error reply
     * @param request the incoming request to handle
     * @return the result containing the reply and optional response body
     */
    @Override
    public HandlerResult handle(Request request) throws Exception {
        URI uri = request.getURI();
        String path = uri.getPath();
        
        if (path == null || path.isEmpty()) { // if no path is given, treat it as the root
            path = "/";
        }

        if (path == null || path.isEmpty() || path.equals("/")) { // normalize root and strip leading slash for resolving under documentRoot
            path = "";
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // build a normalized path under documentRoot and collapse ".." etc.
        Path fullPath = Paths.get(documentRoot).resolve(path).normalize();
        Path docRootPath = Paths.get(documentRoot).normalize();

        if (!fullPath.startsWith(docRootPath)) { // prevent access outside documentRoot -> directory traversal
            return new HandlerResult(new Reply(51, "Not found"));
        }

        File file = fullPath.toFile();

        if (!file.exists()) { // file doesn't exist
            return new HandlerResult(new Reply(59, "Not found"));
        }

        if (file.isDirectory()) { // if it's a directory, try to serve index.gmi from inside it (otheriwse a mess, sorry!)
            File index = new File(file, "index.gmi");
            if (!index.exists() || !index.isFile()) {
                return new HandlerResult(new Reply(51, "Not found"));
            }
            file = index;
        }

        if (!file.isFile() || !file.canRead()) { // must be a readable file
            return new HandlerResult(new Reply(50, "Server error"));
        }

        String mimeType = getMimeType(file.getName()); // get MIME type for the repsone

        // open file stream and return both reply and body
        FileInputStream fileBody = new FileInputStream(file);
        Reply reply = new Reply(20, mimeType);
        return new HandlerResult(reply, fileBody);
    }

    /**
     * MIME type detection based on file extension
     */
    private String getMimeType(String filename) {
        if (filename.endsWith(".gmi")) {
            return "text/gemini";
        } else if (filename.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }

}
