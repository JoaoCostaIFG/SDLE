package subscriber

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
)

type SubscriberI interface {
	Destroy() error
	Connect(endpoint string) error
	Get() []string
	Subscribe(string)
	Unsubscribe(string)
}

type Subscriber struct {
	zctx *zmq.Context
	s    *zmq.Socket
}

func NewSubscriber(zctx *zmq.Context) (*Subscriber, error) {
	s := new(Subscriber)
	s.zctx = zctx
	socket, e := s.zctx.NewSocket(zmq.REQ)
	s.s = socket
	return s, e
}

func (s *Subscriber) Destroy() error {
	return s.s.Close()
}

func (s *Subscriber) Connect(endpoint string) error {
	return s.s.Connect(endpoint)
}

func (s *Subscriber) Get() []string {
	s.s.SendMessage("get")
	reply, _ := s.s.RecvMessage(0)
	return reply
}

func (s *Subscriber) Subscribe(topic string) {
	s.s.SendMessage("sub " + topic)
	reply, _ := s.s.RecvMessage(0)
	fmt.Printf("Subbed: %s: %s\n", topic, reply)
}

func (s *Subscriber) Unsubscribe(topic string) {
	s.s.SendMessage("unsub " + topic)
	reply, _ := s.s.RecvMessage(0)
	fmt.Printf("Unsubbed: %s: %s\n", topic, reply)
}
