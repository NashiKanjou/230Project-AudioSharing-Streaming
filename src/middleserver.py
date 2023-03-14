#!/usr/bin/env python3

import sys
import socket
import selectors
import types

sel = selectors.DefaultSelector()


def accept_wrapper(sock):
    conn, addr = sock.accept()  # Should be ready to read
    print(f"Accepted connection from {addr}")
    conn.setblocking(False)
    data = types.SimpleNamespace(addr=addr, inb=b"", outb=b"")
    events = selectors.EVENT_READ | selectors.EVENT_WRITE
    sel.register(conn, events, data=data)


def service_connection(key, mask):
    sock = key.fileobj
    data = key.data
    if mask & selectors.EVENT_READ:
        recv_data = sock.recv(1024)  # Should be ready to read
        if recv_data:
            data.outb += recv_data
        else:
            print(f"Closing connection to {data.addr}")
            sel.unregister(sock)
            sock.close()
    if mask & selectors.EVENT_WRITE:
        if data.outb:
            print(f"Echoing {data.outb!r} to {data.addr}")
            sent = sock.send(data.outb)  # Should be ready to write
            data.outb = data.outb[sent:]

            data_server_request("placeholder")


# ----------- DATA SERVER INTERACTION -----------

def data_server_request(song_name):
    messages = [b"Message 1 from ms.", b"Message 2 from ms."]

    host = '127.0.0.1'
    port = 65500
    
    # start_connections (start)
    server_addr = (host, port)
    print(f"Starting connection to {server_addr}")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setblocking(False)
    sock.connect_ex(server_addr)
    events = selectors.EVENT_READ | selectors.EVENT_WRITE
    data = types.SimpleNamespace(
        msg_total=sum(len(m) for m in messages),
        recv_total=0,
        messages=messages.copy(),
        outb=b"",
    )
    sel.register(sock, events, data=data)
    # start_connections (end)


    try:
        while True:
            events = sel.select(timeout=1)
            if events:
                for key, mask in events:
                    # service_connection (start)
                    sock2 = key.fileobj
                    data = key.data
                    if mask & selectors.EVENT_READ:
                        recv_data = sock2.recv(1024)  # Should be ready to read
                        if recv_data:
                            data.recv_total += len(recv_data)
                        if not recv_data or data.recv_total == data.msg_total:
                            sel.unregister(sock2)
                            sock2.close()
                    if mask & selectors.EVENT_WRITE:
                        if not data.outb and data.messages:
                            data.outb = data.messages.pop(0)
                        if data.outb:
                            sent = sock2.send(data.outb)  # Should be ready to write
                            data.outb = data.outb[sent:]



                    # service_connection (end)
            # Check for a socket being monitored to continue.
            if not sel.get_map():
                break
    except KeyboardInterrupt:
        print("Caught keyboard interrupt, exiting")
    finally:
        sel.close()


'''
host = '127.0.0.1'
port = 65400
lsock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
lsock.bind((host, port))
lsock.listen()
print(f"Listening on {(host, port)}")
lsock.setblocking(False)
sel.register(lsock, selectors.EVENT_READ, data=None)

try:
    while True:
        events = sel.select(timeout=None)
        for key, mask in events:
            if key.data is None:
                accept_wrapper(key.fileobj)
            else:
                service_connection(key, mask)
except KeyboardInterrupt:
    print("Caught keyboard interrupt, exiting")
finally:
    sel.close()
'''


data_server_request("placeholder")

