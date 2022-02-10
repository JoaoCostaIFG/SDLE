package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"math/rand"
	"time"
)

func main() {
	zctx, _ := zmq.NewContext()

	publisher, _ := zctx.NewSocket(zmq.PUB)
	defer publisher.Close()
	err := publisher.Connect("tcp://localhost:5559")
	if err != nil {
		fmt.Println("Connect error")
		return
	}

	rand.Seed(time.Now().UnixNano())

	for {
		zipcode := rand.Intn(100000)
		temperature := rand.Intn(215) - 80
		relhumidity := rand.Intn(50) + 10

		msg := fmt.Sprintf("%05d %d %d", zipcode, temperature, relhumidity)
		publisher.SendMessage(msg)
	}
}
