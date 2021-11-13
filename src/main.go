package main

import (
	"fmt"
	"github.com/golang-collections/go-datastructures/queue"
	"github.com/golang-collections/go-datastructures/set"
	zmq "github.com/pebbe/zmq4"
	"math/rand"
	"os"
	"src/publisher"
	"src/subscriber"
	"strings"
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
	var sub_map = make(map[string]*set.Set)
	var msg_queues = make(map[string]*queue.Queue)

	pubs, _ := zctx.NewSocket(zmq.ROUTER)
	defer pubs.Close()
	pubs.Bind("tcp://*:" + pub_port)

	subs, _ := zctx.NewSocket(zmq.ROUTER)
	defer subs.Close()
	subs.Bind("tcp://*:" + sub_port)

	poller := zmq.NewPoller()
	poller.Add(pubs, zmq.POLLIN)
	poller.Add(subs, zmq.POLLIN)

	for {
		sockets, _ := poller.Poll(-1)
		fmt.Print(sockets)
		for _, socket := range sockets {
			switch s := socket.Socket; s {
			case subs:
				msg, _ := s.RecvMessage(0)
				// TODO check for NULL frame
				id := msg[0]
				content := msg[2]
				fmt.Printf("Sub %s - %s\n", id, content)

				// handle message
				split_content := strings.SplitN(content, " ", 2)
				// TODO check len
				switch cmd := split_content[0]; cmd {
				case "sub":
					arg := split_content[1]
					sub_set, ok := sub_map[id]
					if !ok {
						// subscriber subs for the first time, create his dataset
						sub_set = set.New(5)
						sub_map[id] = sub_set
					}
					sub_set.Add(arg)
				case "unsub":
					arg := split_content[1]
					sub_set, ok := sub_map[id]
					if !ok {
						// subscriber subs for the first time, create his dataset
						sub_set = set.New(5)
						sub_map[id] = sub_set
					}
					sub_set.Remove(arg)
				}
			case pubs:
				// receive message
				msg, _ := s.RecvMessage(0)
				// TODO check for NULL frame
				id := msg[0]
				content := msg[2]
				fmt.Printf("Pub %s - %s\n", id, content)

				// handle message
				// TODO check len
				split_content := strings.SplitN(content, " ", 2)
				topic := split_content[0]
				update := split_content[1]
				q, ok := msg_queues[topic]
				// TODO mutex pwease
				if !ok {
					// topic does not exists, create queue for it
					q = queue.New(100)
					msg_queues[topic] = q
				}
				// push new update
				q.Put(update)

				// send OK reply
				msg[2] = "We got you fam"
				pubs.SendMessage(msg)
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
