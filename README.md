# SkyMouse
<p align="center">
  <img src="assets/icon.png" alt="App Icon" width="128" style="border-radius:32px;"/>
</p>

<p align="center">
  <img src="assets/s1.png" alt="Screenshot 1" width="200" style="border-radius:4px;"/>
  <img src="assets/s2.png" alt="Screenshot 2" width="200" style="border-radius:4px;"/>
  <img src="assets/s3.png" alt="Screenshot 3" width="200" style="border-radius:4px;"/>
  <img src="assets/s4.png" alt="Screenshot 4" width="200" style="border-radius:4px;"/>
  <img src="assets/s5.png" alt="Screenshot 5" width="200" style="border-radius:4px;"/>
  <img src="assets/s6.png" alt="Screenshot 6" width="200" style="border-radius:4px;"/>
  <img src="assets/s7.png" alt="Screenshot 7" width="200" style="border-radius:4px;"/>
</p>


## How to startup the server
create config.yaml file near of the server.exe  
config fields:  
``` yaml
server_ip: "" # put the mashine IP or "" to listen all interfaces (not required field)
tcp_port: 10000 # port to connect to the server (required field)
log_path: "skymouse.log" # path to the log file (not required field)
```

---

### Server startup flags
`--help` — Print auto-generated help for all flags  
`-lf` — Enable log to a file  
`--config` — Path to the YAML config file  
`--version` — Print Protobuf contract version

#### Command to generate proto for Kotlin & Go  
run in repo root:  
```sh
protoc --proto_path=proto \
--go_out=pc/pkg/protoapi \
--go_opt=paths=source_relative \
--java_out=android/app/src/main/java \
--kotlin_out=android/app/src/main/java \
proto/skymouse.proto
```
