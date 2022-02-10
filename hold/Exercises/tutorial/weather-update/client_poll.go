package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"os"
)

func main() {
	zctx, _ := zmq.NewContext()

	subscriber, _ := zctx.NewSocket(zmq.SUB)
	defer subscriber.Close()
	subscriber.Connect("tcp://localhost:5555")

	// subscribe to zipcode
	filter := "10001 "
	if len(os.Args) > 1 {
		filter = os.Args[1] + " "
	}
	subscriber.SetSubscribe(filter)

	poller := zmq.NewPoller()
	poller.Add(subscriber, zmq.POLLIN)

	for true {
		sockets, _ := poller.Poll(-1)
		for _, socket := range sockets {
			switch  s := socket.Socket; s {
			case subscriber:
				update, _ := s.Recv(0)
				fmt.Println("Got weather update:", update)
			}
		}
	}
}
