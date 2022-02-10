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
	publisher.Bind("tcp://*:5555")

	rand.Seed(time.Now().UnixNano())

	for true {
		zipcode := rand.Intn(100000)
		temperature := rand.Intn(215) - 80
		relhumidity := rand.Intn(50) + 10

		msg := fmt.Sprintf("%05d %d %d", zipcode, temperature, relhumidity)
		publisher.Send(msg, 0)
	}
}