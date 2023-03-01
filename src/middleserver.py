from http.server import BaseHTTPRequestHandler, HTTPServer
import urllib.parse as urlparse
import logging
import time
import requests

hostName = "localhost"
serverPort = 8080

class MiddleServer(BaseHTTPRequestHandler):
    def _set_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_GET(self):
        print(self.path)

        req = str(self.path)[1:]

        self.send_response(200)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(bytes(str("requested file was: " + str(req)), "utf-8"))
   
        # request song from data servers
        dataPorts = [8100]

        for p in dataPorts:
            url = "http://localhost:" + str(p) + "/" + str(req)
            r = requests.get(url)
            print(str(r))



    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        logging.info("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n",
                     str(self.path), str(self.headers), post_data.decode('utf-8'))

        self._set_response()
        self.wfile.write("POST request for {}".format(self.path).encode('utf-8'))

if __name__ == "__main__":        
    webServer = HTTPServer((hostName, serverPort), MiddleServer)
    print("Server started http://%s:%s" % (hostName, serverPort))

    try:
        webServer.serve_forever()
    except KeyboardInterrupt:
        pass

    webServer.server_close()
    print("Server stopped.")
