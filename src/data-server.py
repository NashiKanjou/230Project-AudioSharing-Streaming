#!/usr/bin/env python3

import socket
import wave

HOST = "127.0.0.1"  # localhost
PORT = 65500  # data server port


with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    
    while True:
        s.listen()
        print(f"data server - listening on port {PORT}")
        conn, addr = s.accept()
        with conn:
            print(f"Connected by {addr}")
            while True:
                data = conn.recv(1024)
                print("data DS received was: " + str(data))
                if not data:
                    break

                # load WAV file
                #song = wave.open('icecream.wav', 'rb')
                with open('icecream.wav', 'rb') as f:
                    while True:
                        data = f.read(1024)
                        if not data:
                            break
                        #print("here")
                        #conn.send(data)
                        conn.send(b'trash')
                    #print("done")

                '''
                filename = 'icecream.wav'

                with open(filename, 'rb') as f:
                    for l in f: conn.sendall(l)
                s.close()
                '''

                # conn.sendall(b"songfile.wav")
                #conn.sendall(song)
