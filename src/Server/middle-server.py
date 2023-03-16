#!/usr/bin/env python3

import socket

HOST = "127.0.0.1"  # localhost
MY_PORT = 65400
DS_PORTS = [65500]

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, MY_PORT))
    
    while True:
        s.listen()
        print(f"Middle Server - listening on port {MY_PORT}")
        try:
            conn, addr = s.accept()
            with conn:
                print(f"Connected by {addr}")

                while True:
                    client_request = conn.recv(2048)

                    received_data = 0

                    for port in DS_PORTS:

                        if received_data > 0:
                            break

                        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as t:
                            try:
                                print(f"Attempting connection to data server {port}...")
                                t.connect((HOST, port))

                                print(f"Successfully connected to data server {port}...")
                                t.sendall(client_request)

                                while True:
                                    data = t.recv(2048)
                                    if not data:
                                        break
                                    conn.sendall(data)

                                received_data = 1
                                print(f"Finished sending data to client {addr}.")

                            except socket.error as msg:
                                print(f"Connection failed: {msg}")
                                continue
        except Exception:
            print("error")

                


