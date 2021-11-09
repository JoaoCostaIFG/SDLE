package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"time"
)

func main() {
	zctx, _ := zmq.NewContext()

	responder, _ := zctx.NewSocket(zmq.REP)
	defer responder.Close()
	err := responder.Connect("tcp://localhost:5560")
	if err != nil {
		fmt.Print("Failed connection")
		return
	}

	for {
		request, _ := responder.RecvMessage(0)
		fmt.Printf("Received request: [%s]\n", request[0])

		// do some work
		time.Sleep(1 * time.Second)

		responder.Send("World", 0)
	}
}
