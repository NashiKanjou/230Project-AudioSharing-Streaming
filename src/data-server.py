#!/usr/bin/env python3

import socket
import wave
import os

HOST = "127.0.0.1"  # localhost
PORT = 65500  # data server port

songlist = []
for n in os.listdir():
    if n.endswith('.wav'):
        songlist.append(n)

songlist_str = "liststart\r\n"

for s in songlist:
    songlist_str = songlist_str + s + "\r\n"

songlist_str = songlist_str + "listend"

print(songlist)
print(songlist_str)


with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    
    while True:
        s.listen()
        print(f"data server - listening on port {PORT}")
        conn, addr = s.accept()
        with conn:
            print(f"Connected by {addr}")
            
            request = conn.recv(1024)
            req = str(request)[2:len(str(request))-1]

            if req in songlist:
                print("Looking for song: " + req)
                with open(req, 'rb') as f:
                    while True:
                        data = f.read(1024)
                        if len(data) < 1:
                            print("Done sending WAV file.")
                            break
                        conn.send(data)

            elif 'list' in req:
                print("Sending song list.")
                conn.send(bytes(songlist_str, 'utf-8'))

            else:
                print(f"Requested file {req} not found.")
                conn.send(b"Requested file not found.")
                

