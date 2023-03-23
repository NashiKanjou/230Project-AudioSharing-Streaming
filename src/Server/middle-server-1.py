import socket
import sys
import math
import time
import wave
from threading import Lock
from threading import Thread

PUBLIC_HOST = "127.0.0.1"
LOCAL_HOST = "127.0.0.1"
DATA_PORT = 65400
CLIENT_PORT = 65500
SIZE = 2048
FORMAT = "utf-8"


class MiddleServer:
    def __init__(self):
        self.data_servers = {}
        self.clients = {}

        self.request_queue = {}

        self.playlist = {}
        self.playlist_lock = Lock()
        self.playlist_updated = False

        self.waitCache = 0
        self.cache = {}


    def update_playlist(self, tid, message, lock):
        self.playlist_updated = False
        message = message.strip();
        list = message.split('-')[1].split(';')
        for file in list:
            file = file.strip()
            if file != '':
                if file in self.playlist:
                    value = self.playlist.get(file)
                    if tid not in value:
                        value.append(tid)
                else:
                    self.playlist[file] = [tid]

        self.playlist_updated = True


    def send_data(self, conn, filename):
        data = self.cache[filename]
        for i in range(0, int(math.ceil(len(data) / SIZE))):
            chunk = data[i*SIZE : (i+1)*SIZE]
            conn.send(chunk)
        print(f"Finished sending {filename}")


    def send_data_chunk(self, conn, filename, index):
        data = self.cache[filename]
        chunk = data[index*SIZE : (index + 1)*SIZE]
        conn.send(chunk + index.to_bytes(4, 'big'))


    def handle_request(self, tid, conn, request):

        ## middle <-> data server
        if (request[0:8] == b'playlist'):
            request = request.decode(FORMAT)
            self.update_playlist(tid, request, self.playlist_lock)
        elif (request[0:8] == b'caching '):
            metadata = request.split(b'.wav')[0]
            data = request.split(b'.wav')[1]
            filename = str(metadata[8:])[2:len(str(metadata[8:]))-1] + ".wav"
            if filename not in self.cache.keys():
                self.cache[filename] = data
            else:
                self.cache[filename] += data
        elif (request[0:8] == b'cacheend'):
            self.waitCache = 0
            print("Done caching")

        ## middle <-> client
        elif (request[0:4] == b'list'):
            playlist_str = "liststart\r\n"
            for key in self.playlist.keys():
                playlist_str += key + "\r\n"
            playlist_str += "listend\r\n"
            conn.send(bytes(playlist_str, 'utf-8'))
            print("Playlist sent")
        elif (request[0:6] == b'cache:'):
            request = request.decode(FORMAT)
            filename = request.split(':')[1].strip()
            if filename not in self.cache.keys():
                try:
                    data_server_tid = self.playlist[filename][0]
                    if (data_server_tid == tid):
                        conn.send(filename.encode(FORMAT))
                    else:
                        data_command_queue = self.request_queue[data_server_tid]
                        data_command_queue.append(filename)
                except Exception:
                    print(f"File {filename} is not available")

        elif (request[0:8] == b'download'):
            request = request.decode(FORMAT)
            conn.send(request.encode(FORMAT))

            filename = request.split(' ')[1].strip()
            if filename in self.cache.keys():
                num_pieces = int(math.ceil(len(self.cache[filename]) / float(SIZE)))
                conn.send(num_pieces.to_bytes(4, 'big'))
            else:
                try:
                    data_server_tid = self.playlist[filename][0]
                    if (data_server_tid == tid):
                        conn.send(filename.encode(FORMAT))
                    else:
                        data_command_queue = self.request_queue[data_server_tid]
                        data_command_queue.append(filename)

                except Exception:
                    print(f"File {filename} is not available")
                # find the thread and port communicate with data server
                # holding this file and push a command into its queue
                # for execution
        else:
            request = str(request)

            if (request.startswith("b'")):
                request = request[2:len(str(request)) - 1]
            if (request.endswith("\\r\\n")):
                request = request[0:len(str(request)) - 4]
            if ("\\r\\n" in request):
                commands = request.split("\\r\\n")


            stamp = -1
            if request.endswith('.wav') == False:
                arr = request.split('.wav')
                request = arr[0] + '.wav'
                stamp = int(arr[1])
            if request in self.playlist.keys():
                if request in self.cache.keys():
                    if stamp == -1:
                        conn.send(bytes('stream\r\n', FORMAT))
                        self.send_data(conn, request)
                    else:
                        self.send_data_chunk(conn, request, stamp)
                else:
                    data_client_tid = self.playlist[request][0]
                    if (data_client_tid == tid):
                        conn.send(request.encode(FORMAT))
                    else:
                        data_command_queue = self.request_queue[data_client_tid]
                        data_command_queue.append(request)
                        
                        self.waitCache = tid
                        client_command_queue = self.request_queue[tid]
                        client_command_queue.append(request)

            else:
                print("Invalid request, file not available")

    
    def executer_thread(self, conn, addr):
        while True:
            if len(self.request_queue[addr[1]]) > 0:
                # if (self.waitCache == addr[1]):
                #     continue
                # else:
                request = self.request_queue[addr[1]].pop(0)
                self.handle_request(addr[1], conn, request)


    def receiver_thread(self, conn, addr, port):
        self.request_queue[addr[1]] = []

        if (port == DATA_PORT):
            conn.send(("Data server has successfully connected to middle server\r\n").encode(FORMAT))
            conn.send(("playlist\r\n").encode(FORMAT))

        Thread(target=self.executer_thread, args=(conn, addr)).start()

        while True:
            try:
                message = conn.recv(SIZE)
                if message:
                    self.request_queue[addr[1]].append(message)
            except ConnectionResetError:
                print("Data server disconnected. Terminating current thread")
                conn.close()
                break

            except:
                continue


    def thread_program(self, host, port):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((host, port))
        s.listen()
        
        while True:
            conn, addr = s.accept()
            if port == DATA_PORT:
                self.data_servers[addr[1]] = conn
            elif port == CLIENT_PORT:
                self.clients[addr[1]] = conn
            
            Thread(target=self.receiver_thread, args=(conn, addr, port)).start()

    def middle_server_program(self):
        Thread(target=self.thread_program, args=(LOCAL_HOST, DATA_PORT)).start()
        Thread(target=self.thread_program, args=(PUBLIC_HOST, CLIENT_PORT)).start()



if __name__ == "__main__":
    middleServer = MiddleServer()
    middleServer.middle_server_program()

