package subscriber

import zmq "github.com/pebbe/zmq4"

type SubscriberI interface {
	Connect(endpoint string) error
	Get() (string, string)
	Destroy() error
}

type Subscriber struct {
	zctx *zmq.Context
	s    *zmq.Socket
}

func NewSubscriber(zctx *zmq.Context) (*Subscriber, error) {
	p := new(Subscriber)
	p.zctx = zctx
	s, e := p.zctx.NewSocket(zmq.SUB)
	p.s = s
	return p, e
}

func (p *Subscriber) Destroy() error {
	return p.s.Close()
}

func (p *Subscriber) Connect(endpoint string) error {
	return p.s.Connect(endpoint)
}

func (p *Subscriber) Get() (string, string) {
	msg, _ := p.s.RecvMessage(0)
	return msg[0], msg[1]
}
