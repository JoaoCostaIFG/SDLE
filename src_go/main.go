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

func proxy(zctx *zmq.Context, pubPort string, subPort string) {
	var subMap = make(map[string]*set.Set)
	var msgQueues = make(map[string]*queue.Queue)

	pubs, _ := zctx.NewSocket(zmq.ROUTER)
	defer pubs.Close()
	pubs.Bind("tcp://*:" + pubPort)

	subs, _ := zctx.NewSocket(zmq.ROUTER)
	defer subs.Close()
	subs.Bind("tcp://*:" + subPort)

	poller := zmq.NewPoller()
	poller.Add(pubs, zmq.POLLIN)
	poller.Add(subs, zmq.POLLIN)

	for {
		sockets, _ := poller.Poll(-1)
		for _, socket := range sockets {
			switch s := socket.Socket; s {
			case subs:
				msg, _ := s.RecvMessage(0)
				// TODO check for NULL frame
				id := msg[0]
				content := msg[2]

				// handle message
				splitContent := strings.SplitN(content, " ", 2)
				// TODO check len
				switch cmd := splitContent[0]; cmd {
				case "sub":
					fmt.Printf("Sub %s - %s\n", id, content)

					arg := splitContent[1]
					subsSet, ok := subMap[id]
					if !ok {
						// subscriber subs for the first time, create his dataset
						subsSet = set.New()
						subMap[id] = subsSet
					}
					subsSet.Add(arg)

					// send OK reply
					msg[2] = "OK SUB"
					subs.SendMessage(msg)
				case "unsub":
					fmt.Printf("Unsub %s - %s\n", id, content)

					arg := splitContent[1]
					subsSet, ok := subMap[id]
					if !ok {
						// subscriber subs for the first time, create his dataset
						subsSet = set.New()
						subMap[id] = subsSet
					}
					subsSet.Remove(arg)

					// send OK reply
					msg[2] = "OK UNSUB"
					subs.SendMessage(msg)
				case "get":
					fmt.Printf("Get %s\n", id)

					subscriptions, ok := subMap[id]
					if ok {
						var update string

						// TODO RIP performance
						for _, subscriptionI := range subscriptions.Flatten() {
							subscription := subscriptionI.(string)
							q, ok := msgQueues[subscription]
							if ok && !q.Empty() {
								updates, _ := q.Get(1)
								update = updates[0].(string)
								break
							}
						}

						if update == "" {
							// TODO é suposto dar block
							// send EMPTY reply
							msg[2] = ""
							subs.SendMessage(msg)
						} else {
							// send the UPDATE reply
							msg[2] = update
							subs.SendMessage(msg)
						}
					} else {
						// TODO send ERROR reply
						msg[2] = ""
						subs.SendMessage(msg)
					}
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
				splitContent := strings.SplitN(content, " ", 2)
				topic := splitContent[0]
				update := splitContent[1]
				q, ok := msgQueues[topic]
				// TODO mutex pwease
				if !ok {
					// topic does not exists, create queue for it
					q = queue.New(100)
					msgQueues[topic] = q
				}
				// push new update
				q.Put(update)

				// send OK reply
				msg[2] = "OK PUB"
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
