#!/usr/bin/env python3

import socket


#HOST = '169.234.10.40'
#PORTS = [9991]

HOST = "127.0.0.1"  # localhost
PORTS = [65400, 65410]  # middle server ports

song_name = "songname"

received_data = 0

for port in PORTS:

    if received_data > 0:
        break

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            print(f"Attempting connection to middle server {port}...")
            s.connect((HOST, port))

            print(f"Successfully connected to middle server {port}.")
            s.sendall(b'songname')
    
            test_i = 0
            while True:
                data = s.recv(1024)
                if not data:
                    print("done")
                    break
                test_i += 1
                print(f"got data piece {test_i}")
                
            received_data = 1

        except socket.error as msg:
            print(f"Connection failed: {msg}")
            continue


print("all done")

#print(f"Received {data!r}")
