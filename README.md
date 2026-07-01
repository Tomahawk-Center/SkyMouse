#### Command for generate proto for Kotlin & Go  
run in repo root:  
```sh
protoc --proto_path=proto \
--go_out=pc/pkg/protoapi \
--go_opt=paths=source_relative \
--java_out=android/app/src/main/java \
--kotlin_out=android/app/src/main/java \
proto/skymouse.proto
```
