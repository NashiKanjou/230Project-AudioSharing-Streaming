import os
import socket
import select
import sys

HOST = "127.0.0.1"
PORT = 65500
SIZE = 2048
FORMAT = "utf-8"

class DataServer:
    def __init__(self):
        self.addr = (HOST, PORT)
        self.playlist = []
        self.path = "database"

    def parse_directory(self):
        playlist_str = "playlist - "
        for file in os.listdir(os.path.join(os.getcwd(), self.path)):
            if file.endswith('.wav'):
                self.playlist.append(file)
                playlist_str = playlist_str + file + "; "
        playlist_str = playlist_str + "\r\n"
        return playlist_str      

    def send_data(self, filename):
        with open(os.path.join(os.getcwd(), self.path, filename), 'rb') as file:
            chunk_size = SIZE - len('caching ' + filename)
            data = file.read(chunk_size)
            while len(data) > 0:
                data = bytes('caching ' + filename, FORMAT) + data
                self.server_connection.send(data)
                data = file.read(chunk_size)
            print(f"Done sending {filename} to middle server")
            self.server_connection.send(bytes('cacheend ' + filename, FORMAT))

    def parse_message(self, data):
        messages = data.splitlines()
        for msg in messages:
            if (msg == "playlist"):
                playlist_str = self.parse_directory()
                self.server_connection.send(playlist_str.encode(FORMAT))
                print("playlist sent\n")
            elif (msg in self.playlist):
                self.send_data(msg)
            else:
                print(msg)


    def data_server_program(self):
        self.server_connection = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_connection.connect(self.addr)

        while True:
            sockets_list = [self.server_connection]
            read_sockets, write_socket, error_socket = select.select(sockets_list, [], [])

            for connection in read_sockets:
                if connection == self.server_connection:
                    data = connection.recv(SIZE).decode(FORMAT)
                    self.parse_message(data)
                else:
                    message = sys.stdin.readline()
                    self.server_connection.send(message.encode(FORMAT))
                        

        server_connection.close()


if __name__ == "__main__":
    dataServer = DataServer()
    dataServer.data_server_program()