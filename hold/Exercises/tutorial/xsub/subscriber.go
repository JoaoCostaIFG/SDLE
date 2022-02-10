package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
)

func main() {
	zctx, _ := zmq.NewContext()

	subscriber, _ := zctx.NewSocket(zmq.SUB)
	defer subscriber.Close()
	err := subscriber.Connect("tcp://localhost:5560")
	if err != nil {
		fmt.Println("Couldn't connect")
		return
	}

	// subscribe to zipcode
	filter := "10001 "
	subscriber.SetSubscribe(filter)

	poller := zmq.NewPoller()
	poller.Add(subscriber, zmq.POLLIN)

	for {
		update, _ := subscriber.RecvMessage(0)
		fmt.Println("Got weather update:", update)
	}
}
