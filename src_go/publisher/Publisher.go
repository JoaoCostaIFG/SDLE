package publisher

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
)

type PublisherI interface {
	Connect(endpoint string) error
	Put(topic string, msg string)
	Destroy() error
}

type Publisher struct {
	zctx *zmq.Context
	s    *zmq.Socket
}

func NewPublisher(zctx *zmq.Context) (*Publisher, error) {
	p := new(Publisher)
	p.zctx = zctx
	s, e := p.zctx.NewSocket(zmq.REQ)
	p.s = s
	return p, e
}

func (p *Publisher) Destroy() error {
	return p.s.Close()
}

func (p *Publisher) Connect(endpoint string) error {
	return p.s.Connect(endpoint)
}

func (p *Publisher) Put(topic string, msg string) {
	p.s.SendMessage(topic + " " + msg)
	fmt.Printf("Put mandou: %s %s\n", topic, msg)
	reply, _ := p.s.RecvMessage(0)
	fmt.Printf("Put recebeu: %s\n", reply)
}
