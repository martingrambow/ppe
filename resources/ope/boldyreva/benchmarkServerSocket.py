# Benchmarks encryption via socket.
#
# Usage:
#
# python benchmarkServerSocket.py port key 
# example: python benchmarkServerSocket.py 1234 Friday
#

import socket
import sys
from pyope.ope import OPE
from pyope.ope import ValueRange

# Create a TCP/IP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Bind the socket to the port
server_address = ('localhost', int(sys.argv[1]))
print >>sys.stderr, 'starting up on %s port %s' % server_address
sock.bind(server_address)

# store key
key = str(sys.argv[2])
inrange = ValueRange(0, 2**34-1)
outrange = ValueRange(0, 2**50-1)
cipher = OPE(key, inrange, outrange)

# Listen for incoming connections
sock.listen(1)

while True:
    # Wait for a connection
    print >>sys.stderr, 'waiting for a connection'
    connection, client_address = sock.accept()

    try:
        print 'got a connection'
        # Receive the data in small chunks and retransmit it
        while True:
            data = connection.recv(32)
            if data:
                val = int(str(data).strip());
                if  val < 0:
                  break

                enc = cipher.encrypt(val)
                dec = cipher.decrypt(enc)
                answer = '(' + str(enc) + ',' + str(dec) + ')\n'
                connection.sendall(answer)
                #print answer
            else:
                print >>sys.stderr, 'no more data from', client_address
                break
            
    finally:
        # Clean up the connection
        connection.close()
        break

sock.close()




