from http.server import BaseHTTPRequestHandler, HTTPServer
import logging
import time
import requests

hostName = "localhost"
serverPort = 8100

class MiddleServer(BaseHTTPRequestHandler):


    def _set_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_GET(self):
        print(self.path)

        req = str(self.path)[1:]

        print("requested song is: " + req)
       
        #if req == 'icecream.wav':
        #   f = open(filepath, 'r')
        
        filepath = "icecream.wav"
        f = open(filepath, 'r')
      

        self.send_response(200)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(bytes(str(f), "utf-8"))




if __name__ == "__main__":        
    webServer = HTTPServer((hostName, serverPort), MiddleServer)
    print("Server started http://%s:%s" % (hostName, serverPort))

    try:
        webServer.serve_forever()
    except KeyboardInterrupt:
        pass

    webServer.server_close()
    print("Server stopped.")
