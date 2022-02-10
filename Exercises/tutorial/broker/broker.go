package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
)

func main() {
	zctx, _ := zmq.NewContext()

	frontend, _ := zctx.NewSocket(zmq.ROUTER)
	backend, _ := zctx.NewSocket(zmq.DEALER)
	defer frontend.Close()
	defer backend.Close()
	frontend.Bind("tcp://*:5559")
	backend.Bind("tcp://*:5560")

	poller := zmq.NewPoller()
	poller.Add(frontend, zmq.POLLIN)
	poller.Add(backend, zmq.POLLIN)

	for {
		sockets, _ := poller.Poll(-1)
		// fmt.Print(sockets)
		for _, socket := range sockets {
			switch s := socket.Socket; s {
			case frontend:
				msg, _ := s.RecvMessage(0)
				fmt.Printf("A frente %s\n", msg)
				backend.SendMessage(msg)
			case backend:
				msg, _ := s.RecvMessage(0)
				fmt.Printf("A tras%s\n", msg)
				// _, msg = unwrap(msg)
				frontend.SendMessage(msg)
			}
		}
	}
}