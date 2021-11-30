# Rest Test


## Requirements
### Clojure
  - Version `1.10.3.814`
  #### On Mac
  `brew install clojure/tools/clojure`
  
  #### On Linux
  To install with the Linux script installer:
  Ensure that the following dependencies are installed: bash, curl, rlwrap, and Java.
  Use the linux-install script to download and run the install, which will create the executables /usr/local/bin/clj, /usr/local/bin/clojure, and the directory /usr/local/lib/clojure:

- `curl -O https://download.clojure.org/install/linux-install-1.10.3.1029.sh`
- `chmod +x linux-install-1.10.3.1029.sh`
- `sudo ./linux-install-1.10.3.1029.sh`

### Java
  - Version `11.0.0` or later.


## Running
#### From the command line:
#### Main App
 `make tally`
 
#### Test Suite
`make run-tests`
