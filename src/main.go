package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"math/rand"
	"os"
	"src/publisher"
	"src/subscriber"
	"time"
)

func publish(zctx *zmq.Context, endpoint string) {
	p, err := publisher.NewPublisher(zctx)
	defer p.Destroy()
	if err != nil {
		fmt.Println("Create publisher error")
		return
	}

	err = p.Connect(endpoint)
	if err != nil {
		fmt.Println("Couldn't connect publisher")
		return
	}

	// send random publishing
	rand.Seed(time.Now().UnixNano())
	for {
		zipcode := rand.Intn(100000)
		temperature := rand.Intn(215) - 80

		topic := fmt.Sprintf("%05d", zipcode)
		msg := fmt.Sprintf("%d", temperature)

		p.Put(topic, msg)
	}
}

func subscribe(zctx *zmq.Context, endpoint string) {
	s, err := subscriber.NewSubscriber(zctx)
	defer s.Destroy()
	if err != nil {
		fmt.Println("Create subscriber error")
		return
	}

	err = s.Connect(endpoint)
	if err != nil {
		fmt.Println("Couldn't connect subscriber")
		return
	}

	// subscribe to zipcode
	filter := "10001 "
	s.Subscribe(filter)

	//poller := zmq.NewPoller()
	//poller.Add(subscriber, zmq.POLLIN)

	for {
		update := s.Get()
		fmt.Println("Got weather update:", update)
	}
}

func proxy(zctx *zmq.Context, pub_port string, sub_port string) {
	pubs, _ := zctx.NewSocket(zmq.XSUB)
	defer pubs.Close()
	pubs.Bind("tcp://*:" + pub_port)

	subs, _ := zctx.NewSocket(zmq.XPUB)
	defer subs.Close()
	subs.Bind("tcp://*:" + sub_port)

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

func main() {
	zctx, _ := zmq.NewContext()
	defer zctx.Term()

	switch os.Args[1] {
	case "pub":
		publish(zctx, "tcp://localhost:5559")
	case "sub":
		subscribe(zctx, "tcp://localhost:5560")
	case "proxy":
		proxy(zctx, "5559", "5560")
	}
}
