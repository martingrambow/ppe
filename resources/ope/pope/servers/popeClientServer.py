# Starts a pope client server.

import socket
import sys
import argparse
import random

from ope.nwopec import NwOpeClient
from ope.ciphers import AES

def convkey(x):
    """Turns a integer value into a string preserving comparison."""
    prefix = '0'
    if len(x) < 10:
      y = prefix + x;
      return convkey(y)
    return x

def main(passphrase, pope_hostname, pope_port, client_port):
    #create AES cipher
    crypt = AES(passphrase)
    #create client object
    with NwOpeClient(pope_hostname, pope_port, crypt) as opec:
      # Create a TCP/IP socket
      sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
      # Bind the socket to the port
      server_address = ('localhost', client_port)
      print ('starting up on %s port %s' % server_address)
      sock.bind(server_address)

      # Listen for incoming connections
      sock.listen(1)
  
      while True:
        # Wait for a connection
        print ('waiting for a connection')
        connection, client_address = sock.accept()
  
        try:
          print('got a connection')
          while True:
              data = connection.recv(1024).decode()
              if not data:
                      break
              answer = 'unprocessed'
              values = str(data).split()
              if len(values) > 0:
                if str(values[0]) == "i":
                  print ('insert received')
                  if len(values) > 2:
                    key = str(values[1])
                    value = str(values[2])
                    opec.insert(key, value)
                    answer = str(key + ' , ' + value + ' inserted.\n')
                    connection.send(answer.encode())
                if str(values[0]) == "ii":
                  print ('insert integer received')
                  if len(values) > 2:
                    key = convkey(str(values[1]))
                    value = str(values[2])
                    opec.insert(key, value)
                    answer = str(key + ' , ' + value + ' inserted.\n')
                    connection.send(answer.encode())
                if str(values[0]) == "s":
                  print ('size request received')
                  size = opec.size()
                  answer = str(str(size) + ' elements.\n')
                  connection.send(answer.encode())
                if str(values[0]) == "q":
                  print ('query received')
                  if len(values) > 2:
                    key1 = values[1]
                    key2 = values[2]
                    res = opec.range_search(key1, key2)
                    answer = str('Query result comprises ' + str(len(res)) + ' elements:\n')
                    connection.send(answer.encode())
                    for v in res:
                      answer = str(v) + '\n'
                      connection.send(answer.encode())
                if str(values[0]) == "qi":
                  print ('integer query received')
                  if len(values) > 2:
                    key1 = convkey(values[1])
                    key2 = convkey(values[2])
                    res = opec.range_search(key1, key2)
                    answer = str('Query result comprises ' + str(len(res)) + ' elements:\n')
                    connection.send(answer.encode())
                    for v in res:
                      answer = convkey(str(v)) + '\n'
                      connection.send(answer.encode())            
  
          connection.close()
          print ('Connection closed.') 
        except TypeError as e:
          print ("This is a TypeError")
          print (e)
        except NameError as e:
          print ("This is a NameError")
          print (e)
        except EOFError as e:
          print ("This is a EOFError. {}".format(e.args[-1]))
          print (e)
        except:
          print("Unexpected error:", sys.exc_info()[0])
        finally:
          # Clean up the connection
          connection.close()
          break
  
      sock.close()
      print ('Pope client server terminated.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Network client for insert requests and range queries.")
    parser.add_argument('pope_hostname', help="Host of POPE server.")
    parser.add_argument('pope_port', type=int, help="Port on which the POPE server is running.")
    parser.add_argument('passphrase', help="AES passphrase")
    parser.add_argument('client_port', type=int, help="Port on which this client should listen.")
    args = parser.parse_args()
    main(args.passphrase, args.pope_hostname, args.pope_port, args.client_port)




