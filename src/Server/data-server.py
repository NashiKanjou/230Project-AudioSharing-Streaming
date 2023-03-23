import os
import socket
import select
import sys
import time
from threading import Lock
from threading import Thread

HOST = "127.0.0.1"
PORT = [65400, 65410]
SIZE = 2048
FORMAT = "utf-8"

class DataServer:
    def __init__(self):
        # self.server_connection = {}
        # self.server_connection_lock = Lock()

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

    def send_data(self, filename, server_connection):
        with open(os.path.join(os.getcwd(), self.path, filename), 'rb') as file:
            chunk_size = SIZE - len('caching ' + filename)
            data = file.read(chunk_size)
            while len(data) > 0:
                while (len(data) < chunk_size):
                    data += b'\x00'

                data = bytes('caching ' + filename, FORMAT) + data
                server_connection.send(data)
                data = file.read(chunk_size)

            time.sleep(1.0)
            data = bytes('cacheend ' + filename, FORMAT)
            server_connection.send(data)
            print(f"Done sending {filename} to middle server")


    def parse_message(self, data, server_connection):
        messages = data.splitlines()
        for msg in messages:
            print(msg)
            if (msg == "playlist"):
                playlist_str = self.parse_directory()
                server_connection.send(playlist_str.encode(FORMAT))
                print("playlist sent\n")
            elif (msg in self.playlist):
                self.send_data(msg, server_connection)
            else:
                print(msg)

    def data_server_thread(self, host, port):
        middle_server_connection = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        connected = False
        while True:
            if not connected:
                try:
                    middle_server_connection.connect((host, port))
                    connected = True
                except socket.timeout:
                    print("Connection timeout. Retry in 1.0 second\n")
                    connected = False
                    time.sleep(1.0)
                except Exception as e:
                    print(e)
                    print("Middle server not available. Terminating current thread")
                    connected = False
                    break
            else:
                try:
                    data = middle_server_connection.recv(SIZE).decode(FORMAT)
                    self.parse_message(data, middle_server_connection)
                except Exception as e:
                    print(e)
                    print("Middle server disconnected. Terminating current thread")
                    connected = False
                    middle_server_connection.close()
                    break


    def data_server_program(self):
        for port in PORT:
            Thread(target=self.data_server_thread, args=(HOST, port)).start()



if __name__ == "__main__":
    dataServer = DataServer()
    dataServer.data_server_program()