package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
)

func main() {
	zctx, _ := zmq.NewContext()

	requester, _ := zctx.NewSocket(zmq.REQ)
	defer requester.Close()
	err := requester.Connect("tcp://localhost:5559")
	if err != nil {
		fmt.Print("very sad no connect")
		return
	}

	for i := 0; i < 10; i++ {
		requester.Send("Hello", 0)
		reply, _ := requester.RecvMessage(0)
		fmt.Printf("Received reply %d [%s]\n", i, reply)
	}
}
