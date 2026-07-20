package main

import (
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/Tomahawk-Center/SkyMouse/pc/internal/emulator"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server/tcp"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server/udp"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/session"
	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
)

func main() {
	log.SetOutput(os.Stdout)

	emuEventsCh := make(chan emulator.Event, 20)
	emu := emulator.NewEmulator(emuEventsCh)

	sessMgr := session.NewSessionManager()

	udpServer, err := udp.NewServer(":9999", sessMgr, emu)
	if err != nil {
		log.Fatal(err)
	}

	tcpServer, err := tcp.NewServer(":10000", sessMgr, emu, udpServer.Port)
	if err != nil {
		log.Fatal(err)
	}

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

	go func() {
		for ev := range emuEventsCh {
			log.Printf("Event received: %v\n", ev)
			msgToClient := &protoapi.MessageToClient{
				Event: &protoapi.MessageToClient_ServerEvent{
					ServerEvent: ev.Data,
				},
			}
			err := udpServer.SendProto(ev.SessionId, msgToClient)
			if err != nil {
				log.Printf("Error sending message to client: %v\n", err)
			}
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
