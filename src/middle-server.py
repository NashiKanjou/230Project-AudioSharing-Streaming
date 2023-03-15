#!/usr/bin/env python3

import socket

HOST = "127.0.0.1"  # localhost
CLIENT_PORT = 65400
DS_PORT = 65500



def ds_connect(songname):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((HOST, DS_PORT))
        #s.sendall(b"message here")
        s.sendall(songname)
        while True:
            data = s.recv(1024)
            if not data:
                break


    return songfile


with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, CLIENT_PORT))
    
    while True:
        s.listen()
        print(f"middle server - listening on port {CLIENT_PORT}")
        conn, addr = s.accept()
        with conn:
            print(f"Connected by {addr}")
            songname = conn.recv(1024)

            '''
            songfile = ds_connect(data)
            conn.sendall(songfile)
            '''

            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as t:
                t.connect((HOST, DS_PORT))
                #s.sendall(b"message here")
                t.sendall(songname)
                while True:
                    data = t.recv(1024)
                    if not data:
                        break
                    conn.sendall(data)

                


