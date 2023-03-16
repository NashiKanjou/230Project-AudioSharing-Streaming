#!/usr/bin/env python3

import socket
import wave
import os

HOST = "127.0.0.1"  # localhost
MY_PORT = 65500 

songlist = []
for n in os.listdir("./database"):
    if n.endswith('.wav'):
        songlist.append(n)

songlist_str = "liststart\r\n"

for s in songlist:
    songlist_str = songlist_str + s + "\r\n"

songlist_str = songlist_str + "listend"+ "\r\n"

print(songlist)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, MY_PORT))
    
    while True:
        s.listen()
        print(f"Data Server - listening on port {MY_PORT}")
        while True:
            conn, addr = s.accept()
            try:
                with conn:
                    print(f"Connected by {addr}")
                    
                    request = conn.recv(2048)
                    req = str(request)[2:len(str(request))-5] # trim b' ' from string
        
                    if req in songlist:
                        print("Looking for song: " + req)
                        with open(os.path.join("./database", req), 'rb') as f:
                            #conn.sendall(b'audio\r\n')
                            
                            while True:
                                data = f.read(2048)
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
            except Exception:
                print("error")
