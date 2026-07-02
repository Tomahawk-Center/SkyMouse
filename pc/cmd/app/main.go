package main

import (
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/Tomahawk-Center/SkyMouse/pc/internal/emulator"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server/tcp"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server/udp"
)

func main() {
	udpServer, err := udp.NewServer(":9999")
	if err != nil {
		log.Fatal(err)
	}

	emu := emulator.NewEmulator(5)

	tcpServer := tcp.NewServer(":10000", emu)

	go func() {
		if err := tcpServer.Start(); err != nil {
			log.Fatalf("TCP server start failed: %v\n", err)
		}
	}()

	go func() {
		if err := udpServer.Start(); err != nil {
			log.Fatalf("UDP server start failed: %v\n", err)
		}
	}()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	<-sigCh

	log.Println("Shutting down UDP")
	udpServer.Stop()

	log.Println("Shutting down TCP")
	tcpServer.Stop()
}
