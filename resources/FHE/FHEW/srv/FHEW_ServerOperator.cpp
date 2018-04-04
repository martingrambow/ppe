#include <fstream>
#include <sstream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <algorithm>
#include <unistd.h>
#include <iostream>
#include <cstdlib>
#include <cassert>
#include <sys/types.h> 
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <chrono>
#include "../FHEW.h"
#include "common.h"

using namespace std;

bool debug=false;
int port=3020;
char* evalKeyFile;
FHEW::EvalKey* EK;

// Starts server operator.
int main(int argc, char *argv[]){

  if (argc != 4) {
    std::cout << "Usage: " << argv[0] << " port evalKey debug(0/1)" << std::endl;
    exit(0);
  } 
  port = atoi(argv[1]);
  evalKeyFile = argv[2];
  debug = argv[3];

  // Read the evaluation key from disk
  if (debug) std::cout << "Start reading evaluation key ..." << std::endl;
	
  FHEW::Setup();
  EK = LoadEvalKey(evalKeyFile);

  if (debug) std::cout << "Start server operator service ..." << std::endl;
  //Socket file descriptor
  int socketFileDescriptor;
  //Domain = Internet; Type = Steam; Protcol = unspecified = 0
  socketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0);
  if (socketFileDescriptor < 0){
    std::cerr << "ERROR: Unable to open socket." << std::endl;
    exit(1);
  }
  //Create struct for socket and fill with zeros
  struct sockaddr_in serv_addr, cli_addr;
  bzero((char *) &serv_addr, sizeof(serv_addr));
  //Inititalize socket
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_addr.s_addr = INADDR_ANY;
  serv_addr.sin_port = htons(port);
  //bind socket
  if (bind(socketFileDescriptor, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
    std::cerr << "ERROR: Unable to bind on port " << port << "." << std::endl;
    exit(1);
  }
  //Socket for transmission
  int transmissionSocket;  
  //Lenght
  socklen_t addrLen;
  //used buffer
  char buffer[256];  
  int n;  
  //Listen on port; no waiting queue
  listen(socketFileDescriptor, 1);
  bool end = false;
  std::cout << "Server operator service started and listening on port " << port << "." << std::endl;

  while(!end) {
    //Accept next request
    addrLen = sizeof(cli_addr);
    transmissionSocket = accept(socketFileDescriptor, (struct sockaddr*) &cli_addr, &addrLen);
    if (transmissionSocket < 0) {
      std::cerr << "ERROR: Unable to accept." << std::endl;
      exit(1);
    }
    bzero(buffer,256);
    n = read(transmissionSocket,buffer,255);
    if (n < 0) {
      std::cerr << "ERROR: Unable to read from socket." << std::endl;
      exit(1);
    }
    //Start request time
    string timestring = "";
    auto request_start = std::chrono::high_resolution_clock::now();

    //Handle Request    
    char * answer = new char[5];
    string unhandled = "Unhandled request.\n";
    answer = new char[unhandled.length() + 1];
    strcpy(answer, unhandled.c_str());
    string requestString = buffer;
    if (debug) std::cout << "Request: " << requestString << std::endl;
    
    string cmd = "";
    if (requestString.find(" ") != string::npos) {
      int cmdEnd = requestString.find(" ");
      cmd = requestString.substr(0, cmdEnd);
    } else {
      cmd = requestString.substr(0,1);
    }
    
    //Copy ciphertext
    if (cmd.compare("CPY") == 0) {
      //cpy(copy) source newCtx (overwrite)
      //cpy A3 B45
      int contentSize = requestString.length() - 4;
      string content = requestString.substr(4, contentSize);      
      int sourceEnd = content.find(" ");
      string sourceStr = content.substr(0, sourceEnd);
      string destStr = content.substr(sourceEnd+1, content.length()-sourceEnd-2);
      
      string fileNameSource = "files_s/ctx" + sourceStr + ".bin";
      char* source = new char[fileNameSource.length() + 1];
      strcpy(source, fileNameSource.c_str());

      string fileNameDest = "files_s/ctx" + destStr + ".bin";
      char* dest = new char[fileNameDest.length() + 1];
      strcpy(dest, fileNameDest.c_str());
      
      LWE::CipherText *ct1;
      
      // Read ciphertext from file
      ct1 = LoadCipherText(source);
      //Write new file
      SaveCipherText(ct1,dest);    
  
      string tmpAnswer = "Done.";
      answer = new char[tmpAnswer.length() + 1];
      strcpy(answer, tmpAnswer.c_str());
    }
    //negate
    if (cmd.compare("NOT") == 0) {
      //NOT source dest
      //NOT C7 D7
      int contentSize = requestString.length() - 4;
      string content = requestString.substr(4, contentSize);      
      int sourceEnd = content.find(" ");
      string sourceStr = content.substr(0, sourceEnd);
      string destStr = content.substr(sourceEnd+1, content.length()-sourceEnd-2);
      
      string fileNameSource = "files_s/ctx" + sourceStr + ".bin";
      char* source = new char[fileNameSource.length() + 1];
      strcpy(source, fileNameSource.c_str());

      string fileNameDest = "files_s/ctx" + destStr + ".bin";
      char* dest = new char[fileNameDest.length() + 1];
      strcpy(dest, fileNameDest.c_str());
      
      LWE::CipherText *ct1;
      
      // Read ciphertext from file
      ct1 = LoadCipherText(source);
      //Perform NOT
      auto cmd_start = std::chrono::high_resolution_clock::now();
      FHEW::HomNOT(ct1, *ct1);
      auto cmd_end = std::chrono::high_resolution_clock::now();
      auto cmd_time = cmd_end - cmd_start;
      ostringstream timestream;
      timestream << std::chrono::duration_cast<std::chrono::milliseconds>(cmd_time).count();
      timestring = timestring + cmd + ":" + timestream.str();

      //Write new file
      SaveCipherText(ct1,dest);    
  
      string tmpAnswer = "Done.";
      answer = new char[tmpAnswer.length() + 1];
      strcpy(answer, tmpAnswer.c_str());
    }
    //all other gates
    if ((cmd.compare("OR") == 0) || (cmd.compare("AND") == 0) || (cmd.compare("NOR") == 0) || (cmd.compare("NAND") == 0)|| (cmd.compare("XOR") == 0)) {
      //gate source1 source2 dest
      //and A B C
      int pos1 = requestString.find(" ");
      int pos2 = requestString.find(" ", (pos1+1));
      int pos3 = requestString.find(" ", (pos2+1));
      
      if ((pos1 != -1) && (pos2 != -1) && (pos3 != -1)) {
        //format ok
        int src1Size = pos2 - pos1 - 1;
        string sourceStr1 = requestString.substr(pos1+1, src1Size);
        int src2Size = pos3 - pos2 - 1;
        string sourceStr2 = requestString.substr(pos2+1, src2Size);
        int destSize = requestString.length() - pos3 - 2;
        string destStr = requestString.substr(pos3+1, destSize);      
      
        string fileNameSource1 = "files_s/ctx" + sourceStr1 + ".bin";
        char* source1 = new char[fileNameSource1.length() + 1];
        strcpy(source1, fileNameSource1.c_str());
        string fileNameSource2 = "files_s/ctx" + sourceStr2 + ".bin";
        char* source2 = new char[fileNameSource2.length() + 1];
        strcpy(source2, fileNameSource2.c_str());
        string fileNameDest = "files_s/ctx" + destStr + ".bin";
        char* dest = new char[fileNameDest.length() + 1];
        strcpy(dest, fileNameDest.c_str());
        
        LWE::CipherText *ct1,*ct2,*ct3;
        ct1 = LoadCipherText(source1);
        ct2 = LoadCipherText(source2);
        ct3 = new LWE::CipherText;

        BinGate gate;
        bool flag = false;
        if (cmd.compare("OR") == 0) gate = OR; 
        else if (cmd.compare("AND") == 0) gate = AND; 
        else if (cmd.compare("NOR") == 0) gate = NOR; 
        else if (cmd.compare("NAND") == 0) gate = NAND; 
        else 
        {
          if (cmd.compare("XOR") == 0) {
              LWE::CipherText *ct4,*ct5;
              ct4 = new LWE::CipherText;
              ct5 = new LWE::CipherText;
              auto cmd_start = std::chrono::high_resolution_clock::now();
              gate = OR;
              FHEW::HomGate(ct4, gate, *EK,*ct1,*ct2);
              gate = NAND;
              FHEW::HomGate(ct5, gate, *EK,*ct1,*ct2);
              gate = AND;
              FHEW::HomGate(ct3, gate, *EK,*ct4,*ct5);
              auto cmd_end = std::chrono::high_resolution_clock::now();
              auto cmd_time = cmd_end - cmd_start;
              ostringstream timestream;
              timestream << std::chrono::duration_cast<std::chrono::milliseconds>(cmd_time).count();
              timestring = timestring + cmd + ":" + timestream.str();

              SaveCipherText(ct3,dest);

              string tmpAnswer = "Done.";
              answer = new char[tmpAnswer.length() + 1];
              strcpy(answer, tmpAnswer.c_str());
              flag = true;
          } else {
            cerr << "This gate does not exists (please choose {AND, OR, NAND, NOR, XOR})" << std::endl;
            flag = true;
            string tmpAnswer = "This gate does not exists (please choose {AND, OR, NAND, NOR}).\n";
            answer = new char[tmpAnswer.length() + 1];
            strcpy(answer, tmpAnswer.c_str());
          }
        }
        if (!flag) {
          auto cmd_start = std::chrono::high_resolution_clock::now();
          FHEW::HomGate(ct3, gate, *EK,*ct1,*ct2);
          SaveCipherText(ct3,dest);
          auto cmd_end = std::chrono::high_resolution_clock::now();
          auto cmd_time = cmd_end - cmd_start;
          ostringstream timestream;
          timestream << std::chrono::duration_cast<std::chrono::milliseconds>(cmd_time).count();
          timestring = timestring + cmd + ":" + timestream.str();

          string tmpAnswer = "Done.";
          answer = new char[tmpAnswer.length() + 1];
          strcpy(answer, tmpAnswer.c_str());
        } 
      } else {
        string tmpAnswer = "Wrong format.\n";
        answer = new char[tmpAnswer.length() + 1];
        strcpy(answer, tmpAnswer.c_str());
      }      
    }
    //quit server
    if (cmd.compare("q") == 0) {
      std::cout << "Server operation service ends." << std::endl;
      end = true;
      string endMessage = "Server operation service ends.\n";
      answer = new char[endMessage.length() + 1];
      strcpy(answer, endMessage.c_str());
    }
    auto request_end = std::chrono::high_resolution_clock::now();
    auto request_time = request_end - request_start;
    ostringstream timestream;
    timestream << std::chrono::duration_cast<std::chrono::milliseconds>(request_time).count();
    timestring = timestring + ":" + timestream.str();

    //Write answer
    string completeAnswer = string(answer) + " :" + timestring + "\n";
    answer = new char[completeAnswer.length() + 1];
    
    strcpy(answer, completeAnswer.c_str());

    n = write(transmissionSocket, answer, strlen(answer));
    if (n < 0) {
      std::cerr << "ERROR: Unable to write to socket." << std::endl;
      exit(1);
    }
    
    //Close request
    close(transmissionSocket);
    //Wait for next request or quit server
  }  
  close(socketFileDescriptor);
  std::cout << "Server operation service stopped." << std::endl;
  return 0; 
}
