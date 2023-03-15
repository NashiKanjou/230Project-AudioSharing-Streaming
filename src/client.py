#!/usr/bin/env python3

import socket

HOST = "127.0.0.1"  # localhost
PORT = 65400  # middle server port

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    s.sendall(b"Songname")
    
    while True:
        data = s.recv(1024)
        if not data:
            break
        print("got data piece")

#print(f"Received {data!r}")
