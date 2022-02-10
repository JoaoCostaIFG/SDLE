package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
)

func main() {
	zctx, _ := zmq.NewContext()

	pubs, _ := zctx.NewSocket(zmq.XSUB)
	defer pubs.Close()
	pubs.Bind("tcp://*:5559")

	subs, _ := zctx.NewSocket(zmq.XPUB)
	defer subs.Close()
	subs.Bind("tcp://*:5560")

	poller := zmq.NewPoller()
	poller.Add(pubs, zmq.POLLIN)
	poller.Add(subs, zmq.POLLIN)

	for {
		sockets, _ := poller.Poll(-1)
		// fmt.Print(sockets)
		for _, socket := range sockets {
			switch s := socket.Socket; s {
			case subs:
				msg, _ := s.RecvMessage(0)
				fmt.Printf("Sub %s\n", msg)
				pubs.SendMessage(msg)
			case pubs:
				msg, _ := s.RecvMessage(0)
				fmt.Printf("Pub %s\n", msg)
				subs.SendMessage(msg)
			}
		}
	}
}
