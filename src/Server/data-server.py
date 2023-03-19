#!/usr/bin/env python3

import socket
import wave
import os
import math
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
                    stamp = -1
                    print(req)
                    if req.startswith("download"):
                        req = req[9:]
                        if req in songlist:
                            conn.send(bytes('download '+req+'\r\n', 'utf-8'))
                            print("Looking for song: " + req)
                            size = os.path.getsize("./database/"+ req)
                            int_piece = int(math.ceil((size/2048.0)))
                            print("piece="+str(int_piece))
                            conn.send(int_piece.to_bytes(4, 'big'))
                    else:
                        if req.endswith(".wav")==False:
                            arr = req.split(".wav")
                            req = arr[0]+".wav"
                            stamp = int(arr[1])
                        if req in songlist:
                            if stamp == -1:
                                conn.send(bytes('stream\r\n', 'utf-8'))
                            print("Looking for song: " + req+str(stamp))
                            with open(os.path.join("./database/"+ req), 'rb') as f:
                                print("file open")
                                #conn.sendall(b'audio\r\n')
                                int_count=0
                                while True:
                                    data = f.read(2048)
                                    if len(data) < 1:
                                        print("Done sending WAV file.")
                                        break
                                    #print('read'+str(len(data)))
                                    if(stamp==-1):
                                        conn.send(data)
                                    else:
                                        #data.extend(stamp.to_bytes(4, 'big'))
                                        #chunk = bytes[len(data)+4]
                                        #c=0
                                        #for b in data:
                                        #    chunk[c]=b
                                        #    c+=1
                                        #print(data+stamp.to_bytes(4, 'big'))
                                        #for b in stamp.to_bytes(4, 'big'):
                                        #    chunk[c]=b
                                        #    c+=1
                                        #conn.send(data)
                                        #conn.send(stamp.to_bytes(4, 'big'))
                                        f.seek(stamp*2048,0)
                                        data = f.read(2048)
                                        conn.send(data+stamp.to_bytes(4, 'big'))
                                        print("Done sending WAV file. tag:"+str(stamp))
                                        break;
                                    
                                    int_count+=1
                        elif 'list' in req:
                            print("Sending song list.")
                            conn.send(bytes(songlist_str, 'utf-8'))
            
                        else:
                            print(f"Requested file {req} not found.")
                            conn.send(b"Requested file not found.")
            except Exception as e:
                print(e)
