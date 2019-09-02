# Property Preserving Encryption

This code was used in the paper "Three Tales of Disillusion: Benchmarking Property Preserving Encryption Schemes" 

Bibtex:
````
@incollection{paper_pallas_grambow_three_tales_disillusion,
    Title = {Three Tales of Disillusion: Benchmarking Property Preserving Encryption Schemes},
    Author = {Frank Pallas and Martin Grambow},
    Booktitle = {15th International Conference on Trust, Privacy and Security in Digital Business - TrustBus 2018},
    Year = {2018}
}
````

## Structure:
```
/craft - benchmarking Boldyreva encryption scheme
/crypto - supporting classes for craft scenario
/efhe - benchmarking fully homomorphic encryption
/keyvaluestore - benchmarking Partial Order Preserving Encoding (POPE)
/resources - used (partially modified) implementations of encryption schemes 
/util - supporting classes
```

***

## Getting Started

These instructions will help you to start/modify the experiments.

***

### Craft (Boldyreva)

#### Required software

* Java 1.7
* python
* mysql
* gcc
* libffi-devel
* python-devel
* openssl-devel

#### Installing / Running
* Setup SSL certificates and truststores (if you want to use SSL)
* Setup and start mysql server
* Start encryption and decryption service (decryptServer.py, encryptServer.py)
* Start client application (main.java.org.craft.client.Main)

***

### Keyvaluestore (POPE)

#### Required software

* Java 1.7
* python3
* mysql
* gcc
* pycrypto

#### Installing / Running
* Start client application (main.java.org.metadata.client.ClientApplication)
* Start servers (main.java.org.metadata.server.POPEServerStarter)
* The servers have to be online when the script starts

***

### EFHE

#### Required software

* Java 1.7
* gcc
* FFTW3

#### Installing / Running
* Start server application (main.java.org.efhe.server.EFHEServer)
* Start client application (main.java.org.efhe.client.EFHEClient)
* run benchmarking script (*.sh)

***

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

***

## Authors

* **Martin Grambow** - *Initial work* - [martingrambow](https://github.com/MartinGrambow)

See also the list of [contributors](https://github.com/martingrambow/ppe/contributors) who participated in this project.

***

## License

This project is licensed under the GPUv2 License - see the [LICENSE](LICENSE) file for details

***

## Used software

* [pyope](https://github.com/rev112/pyope)
* [POPE](https://github.com/dsroche/pope)
* [FHEW](https://github.com/lducas/FHEW)


